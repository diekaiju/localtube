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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocalHttpServer.LogListener {

    private TextView textStatus;
    private TextView textIpAddress;
    private TextView textUrls;
    private TextView textLogs;
    private android.widget.ScrollView scrollLogs;
    private Button btnToggle;
    private Button btnOpenBrowser;
    private Button btnSettings;
    private com.google.android.material.card.MaterialCardView cardStatus;
    private TextView statusIndicator;
    private TextView textLogsTitle;
    private View cardLogs;
    private TextView textLockStatus;
    private Button btnForceRelease;
    private com.google.android.material.card.MaterialCardView cardPlayLock;

    private View cardServerRemote;
    private Button remoteLeft, remoteRight, remoteEnter, remoteBack;
    private Button remoteRewind, remotePlayPause, remoteForward;

    private ServerService serverService;
    private boolean isBound = false;
    private final java.util.ArrayList<String> htmlLogLines = new java.util.ArrayList<>();
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
        // Copy crashes on any thread to system clipboard
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    String stackTrace = sw.toString();

                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        android.content.ClipData clip = android.content.ClipData.newPlainText("App Crash Log", stackTrace);
                        clipboard.setPrimaryClip(clip);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            }
        });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textStatus = findViewById(R.id.text_status);
        textIpAddress = findViewById(R.id.text_ip_address);
        textUrls = findViewById(R.id.text_urls);
        textLogs = findViewById(R.id.text_logs);
        scrollLogs = findViewById(R.id.scroll_logs);
        btnToggle = findViewById(R.id.btn_toggle);
        btnOpenBrowser = findViewById(R.id.btn_open_browser);
        btnSettings = findViewById(R.id.btn_settings);
        cardStatus = findViewById(R.id.card_status);
        statusIndicator = findViewById(R.id.status_indicator);
        textLogsTitle = findViewById(R.id.text_logs_title);
        cardLogs = findViewById(R.id.card_logs);
        textLockStatus = findViewById(R.id.text_lock_status);
        btnForceRelease = findViewById(R.id.btn_force_release);
        cardPlayLock = findViewById(R.id.card_play_lock);

        cardServerRemote = findViewById(R.id.card_server_remote);
        remoteLeft = findViewById(R.id.remote_left);
        remoteRight = findViewById(R.id.remote_right);
        remoteEnter = findViewById(R.id.remote_enter);
        remoteBack = findViewById(R.id.remote_back);
        remoteRewind = findViewById(R.id.remote_rewind);
        remotePlayPause = findViewById(R.id.remote_play_pause);
        remoteForward = findViewById(R.id.remote_forward);

        setupRemoteButton(remoteLeft, "left");
        setupRemoteButton(remoteRight, "right");
        setupRemoteButton(remoteEnter, "enter");
        setupRemoteButton(remoteBack, "back");
        setupRemoteButton(remoteRewind, "rewind");
        setupRemoteButton(remotePlayPause, "play_pause");
        setupRemoteButton(remoteForward, "forward");

        if (btnForceRelease != null) {
            btnForceRelease.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LocalHttpServer.releaseLock();
                    Toast.makeText(MainActivity.this, "Play lock forcefully released", Toast.LENGTH_SHORT).show();
                    updateLockUi();
                }
            });
        }

        LocalHttpServer.setLockStatusListener(new LocalHttpServer.LockStatusListener() {
            @Override
            public void onLockStatusChanged() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateLockUi();
                    }
                });
            }
        });

        if (textLogsTitle != null && cardLogs != null) {
            textLogsTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (cardLogs.getVisibility() == View.VISIBLE) {
                        cardLogs.setVisibility(View.GONE);
                        textLogsTitle.setText("Console logs (tap to expand)");
                    } else {
                        cardLogs.setVisibility(View.VISIBLE);
                        textLogsTitle.setText("Console logs (tap to collapse)");
                    }
                }
            });
        }

        // Fix scrolling inside nested ScrollView
        if (scrollLogs != null) {
            scrollLogs.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return false;
                }
            });
        }

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
                    try {
                        startActivity(browserIntent);
                    } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(MainActivity.this, "No browser found to open link", Toast.LENGTH_LONG).show();
                    }
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
        updateLockUi();
    }

    private void updateLockUi() {
        if (textLockStatus == null || btnForceRelease == null || cardPlayLock == null) return;
        
        boolean locked = LocalHttpServer.isLocked();
        if (locked) {
            cardPlayLock.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2D1E13"))); // Dark orange/amber
            cardPlayLock.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#E67E22")));
            
            String status = "Locked by: " + LocalHttpServer.getActiveClientIp();
            if (LocalHttpServer.getActiveVideoTitle() != null) {
                status += "\nPlaying: " + LocalHttpServer.getActiveVideoTitle();
            }
            textLockStatus.setText(status);
            textLockStatus.setTextColor(Color.parseColor("#E67E22"));
            btnForceRelease.setEnabled(true);
        } else {
            cardPlayLock.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1E1E1E")));
            cardPlayLock.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2C2C2C")));
            textLockStatus.setText("Status: Unlocked / Idle");
            textLockStatus.setTextColor(Color.parseColor("#888888"));
            btnForceRelease.setEnabled(false);
        }

        if (cardServerRemote != null) {
            cardServerRemote.setAlpha(locked ? 1.0f : 0.5f);
        }
        setButtonEnabled(remoteLeft, locked);
        setButtonEnabled(remoteRight, locked);
        setButtonEnabled(remoteEnter, locked);
        setButtonEnabled(remoteBack, locked);
        setButtonEnabled(remoteRewind, locked);
        setButtonEnabled(remotePlayPause, locked);
        setButtonEnabled(remoteForward, locked);
    }

    private void setButtonEnabled(Button btn, boolean enabled) {
        if (btn != null) {
            btn.setEnabled(enabled);
            btn.setClickable(enabled);
        }
    }

    private void setupRemoteButton(Button btn, final String command) {
        if (btn != null) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (LocalHttpServer.isLocked()) {
                        LocalHttpServer.addPendingCommand(command);
                        Toast.makeText(MainActivity.this, "Sent: " + command, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (LocalHttpServer.isLocked()) {
            String command = null;
            switch (keyCode) {
                case android.view.KeyEvent.KEYCODE_DPAD_UP:
                    command = "up";
                    break;
                case android.view.KeyEvent.KEYCODE_DPAD_DOWN:
                    command = "down";
                    break;
                case android.view.KeyEvent.KEYCODE_DPAD_LEFT:
                    command = "left";
                    break;
                case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
                    command = "right";
                    break;
                case android.view.KeyEvent.KEYCODE_DPAD_CENTER:
                case android.view.KeyEvent.KEYCODE_ENTER:
                    command = "enter";
                    break;
                case android.view.KeyEvent.KEYCODE_BACK:
                    command = "back";
                    break;
                case android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case android.view.KeyEvent.KEYCODE_HEADSETHOOK:
                    command = "play_pause";
                    break;
                case android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    command = "forward";
                    break;
                case android.view.KeyEvent.KEYCODE_MEDIA_REWIND:
                    command = "rewind";
                    break;
            }
            if (command != null) {
                LocalHttpServer.addPendingCommand(command);
                Toast.makeText(this, "Remote Key: " + command, Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    private String formatLogToHtml(String time, String message) {
        String displayMessage = message;
        boolean isTruncated = false;
        if (message.length() > 200 || message.contains("\n")) {
            int newlineIdx = message.indexOf("\n");
            if (newlineIdx > 0 && newlineIdx < 120) {
                displayMessage = message.substring(0, newlineIdx);
            } else {
                displayMessage = message.substring(0, Math.min(message.length(), 120));
            }
            isTruncated = true;
        }

        String escapedMessage = escapeHtml(displayMessage);
        if (isTruncated) {
            escapedMessage += " <font color='#64748B'><b>[Truncated: " + message.length() + " chars]</b></font>";
        }

        String colorTime = "#64748B"; // Slate-400
        String colorMessage = "#E2E8F0"; // Slate-200 (default)

        String lowerMsg = displayMessage.toLowerCase(Locale.US);
        if (lowerMsg.contains("error") || lowerMsg.contains("exception") || lowerMsg.contains("failed")) {
            colorMessage = "#F87171"; // Red-400
        } else if (lowerMsg.contains("started") || lowerMsg.contains("completed")) {
            colorMessage = "#4ADE80"; // Green-400
        } else if (lowerMsg.contains("stopped")) {
            colorMessage = "#FB923C"; // Orange-400
        } else if (lowerMsg.startsWith("request:")) {
            colorMessage = "#E2E8F0";
            if (escapedMessage.contains(" GET ")) {
                escapedMessage = escapedMessage.replace("Request:", "<font color='#F472B6'><b>REQ</b></font>") // Pink-400
                                               .replace(" GET ", " <font color='#4ADE80'><b>GET</b></font> <font color='#38BDF8'>"); // LightBlue-400
                escapedMessage += "</font>";
            } else if (escapedMessage.contains(" POST ")) {
                escapedMessage = escapedMessage.replace("Request:", "<font color='#F472B6'><b>REQ</b></font>")
                                               .replace(" POST ", " <font color='#FB923C'><b>POST</b></font> <font color='#38BDF8'>");
                escapedMessage += "</font>";
            }
        } else if (lowerMsg.contains("proxying stream") || lowerMsg.contains("serving local")) {
            colorMessage = "#C084FC"; // Purple-400
        }

        if (lowerMsg.startsWith("request:")) {
            return "<font color='" + colorTime + "'>[" + time + "]</font> " + escapedMessage + "<br/>";
        } else {
            return "<font color='" + colorTime + "'>[" + time + "]</font> <font color='" + colorMessage + "'>" + escapedMessage + "</font><br/>";
        }
    }

    @Override
    public void onLog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String time = dateFormat.format(new Date());
                String formattedLine = formatLogToHtml(time, message);
                htmlLogLines.add(formattedLine);
                if (htmlLogLines.size() > 200) { // Limit buffer to 200 lines
                    htmlLogLines.remove(0);
                }

                StringBuilder sb = new StringBuilder();
                for (String line : htmlLogLines) {
                    sb.append(line);
                }

                if (textLogs != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        textLogs.setText(android.text.Html.fromHtml(sb.toString(), android.text.Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        textLogs.setText(android.text.Html.fromHtml(sb.toString()));
                    }
                }

                if (scrollLogs != null) {
                    scrollLogs.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollLogs.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
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
        LocalHttpServer.setLockStatusListener(null);
        super.onDestroy();
    }

}
