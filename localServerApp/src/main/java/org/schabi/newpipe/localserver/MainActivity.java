package org.schabi.newpipe.localserver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocalHttpServer.LogListener {

    private TextView textStatus;
    private TextView textIpAddress;
    private TextView textUrls;
    private TextView textLogs;
    private Button btnToggle;
    private Button btnOpenBrowser;
    private Button btnSettings;
    private com.google.android.material.card.MaterialCardView cardStatus;
    private TextView statusIndicator;

    private ServerService serverService;
    private boolean isBound = false;
    private final StringBuilder logBuffer = new StringBuilder();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServerService.LocalBinder binder = (ServerService.LocalBinder) service;
            serverService = binder.getService();
            isBound = true;
            serverService.setStatusListener(new ServerService.ServerStatusListener() {
                @Override
                public void onStatusChanged(final boolean isRunning) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateUi();
                        }
                    });
                }
            });
            updateUi();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (serverService != null) {
                serverService.setStatusListener(null);
            }
            isBound = false;
            updateUi();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textStatus = findViewById(R.id.text_status);
        textIpAddress = findViewById(R.id.text_ip_address);
        textUrls = findViewById(R.id.text_urls);
        textLogs = findViewById(R.id.text_logs);
        btnToggle = findViewById(R.id.btn_toggle);
        btnOpenBrowser = findViewById(R.id.btn_open_browser);
        btnSettings = findViewById(R.id.btn_settings);
        cardStatus = findViewById(R.id.card_status);
        statusIndicator = findViewById(R.id.status_indicator);

        // Bind log callback
        LocalHttpServer.setLogListener(this);

        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound && serverService != null) {
                    if (serverService.isRunning()) {
                        serverService.stopServer();
                    } else {
                        startServerService();
                    }
                    updateUi();
                }
            }
        });

        btnOpenBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound && serverService != null && serverService.isRunning()) {
                    String url = serverService.getLocalAddress();
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        // Bind to Service
        Intent intent = new Intent(this, ServerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void startServerService() {
        Intent intent = new Intent(this, ServerService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void updateUi() {
        if (cardStatus == null || statusIndicator == null) return;
        if (isBound && serverService != null && serverService.isRunning()) {
            cardStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#132D1B")));
            cardStatus.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
            textStatus.setText("Status: Running");
            textStatus.setTextColor(Color.parseColor("#4CAF50"));
            statusIndicator.setText("🟢");

            btnToggle.setText("Stop Server");
            btnToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E53935"))); // Red
            btnOpenBrowser.setEnabled(true);

            String localIp = ServerService.getLocalIpAddress();
            textIpAddress.setText("IP Address: " + (localIp != null ? localIp : "Not Available"));
            String addressText = "Local Device: http://localhost:8080\n" +
                    (localIp != null ? "Network Link: http://" + localIp + ":8080" : "");
            textUrls.setText(addressText);
        } else {
            cardStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2D1313")));
            cardStatus.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#C62828")));
            textStatus.setText("Status: Stopped");
            textStatus.setTextColor(Color.parseColor("#E53935"));
            statusIndicator.setText("🔴");

            btnToggle.setText("Start Server");
            btnToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green
            btnOpenBrowser.setEnabled(false);
            textIpAddress.setText("IP Address: Not Available");
            textUrls.setText("Server is not running.");
        }
    }

    @Override
    public void onLog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String time = dateFormat.format(new Date());
                logBuffer.append("[").append(time).append("] ").append(message).append("\n");
                if (logBuffer.length() > 50000) { // Limit console buffer size
                    logBuffer.delete(0, 10000);
                }
                textLogs.setText(logBuffer.toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    @Override
    protected void onDestroy() {
        if (isBound) {
            if (serverService != null) {
                serverService.setStatusListener(null);
            }
            unbindService(serviceConnection);
            isBound = false;
        }
        LocalHttpServer.setLogListener(null);
        super.onDestroy();
    }

}
