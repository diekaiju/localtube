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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends AppCompatActivity {

    private TextView textStatus;
    private TextView textIpAddress;
    private TextView textUrls;
    private Button btnToggle;
    private Button btnOpenBrowser;
    private Button btnSettings;
    private com.google.android.material.card.MaterialCardView cardStatus;
    private TextView statusIndicator;
    private TextView textLockStatus;
    private Button btnForceRelease;
    private com.google.android.material.card.MaterialCardView cardPlayLock;
    private Button btnLaunchRemote;

    private View cardGettingStarted;
    private View btnToggleGuide;
    private View layoutGuideContent;
    private TextView textGuideSummary;
    private TextView textGuideArrow;

    private ServerService serverService;
    private boolean isBound = false;

    private final LocalHttpServer.LockStatusListener lockStatusListener = new LocalHttpServer.LockStatusListener() {
        @Override
        public void onLockStatusChanged() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateLockUi();
                }
            });
        }
    };

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

        // Initialize LogRepository (begins listening/buffering logs in background)
        LogRepository.getInstance();

        // Bind toolbar elements
        View btnOpenLogs = findViewById(R.id.btn_open_logs);
        if (btnOpenLogs != null) {
            btnOpenLogs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, LogActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Setup ViewPager2 and TabLayout
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        if (viewPager != null && tabLayout != null) {
            viewPager.setAdapter(new ViewPagerAdapter());
            viewPager.setOffscreenPageLimit(1); // Keep pages active in memory
            new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
                @Override
                public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                    if (position == 0) {
                        tab.setText("Control");
                    } else {
                        tab.setText("Casting");
                    }
                }
            }).attach();
        }

        // Bind to Service
        Intent intent = new Intent(this, ServerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void bindControlPage(View view) {
        textStatus = view.findViewById(R.id.text_status);
        textIpAddress = view.findViewById(R.id.text_ip_address);
        textUrls = view.findViewById(R.id.text_urls);
        btnToggle = view.findViewById(R.id.btn_toggle);
        btnOpenBrowser = view.findViewById(R.id.btn_open_browser);
        cardStatus = view.findViewById(R.id.card_status);
        statusIndicator = view.findViewById(R.id.status_indicator);

        cardGettingStarted = view.findViewById(R.id.card_getting_started);
        btnToggleGuide = view.findViewById(R.id.btn_toggle_guide);
        layoutGuideContent = view.findViewById(R.id.layout_guide_content);
        textGuideSummary = view.findViewById(R.id.text_guide_summary);
        textGuideArrow = view.findViewById(R.id.text_guide_arrow);

        if (btnToggleGuide != null && layoutGuideContent != null) {
            btnToggleGuide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (layoutGuideContent.getVisibility() == View.VISIBLE) {
                        layoutGuideContent.setVisibility(View.GONE);
                        if (textGuideSummary != null) textGuideSummary.setVisibility(View.VISIBLE);
                        if (textGuideArrow != null) textGuideArrow.setText("▶");
                    } else {
                        layoutGuideContent.setVisibility(View.VISIBLE);
                        if (textGuideSummary != null) textGuideSummary.setVisibility(View.GONE);
                        if (textGuideArrow != null) textGuideArrow.setText("▼");
                    }
                }
            });
        }

        if (btnToggle != null) {
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
        }

        if (btnOpenBrowser != null) {
            btnOpenBrowser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isBound && serverService != null && serverService.isRunning()) {
                        String url = serverService.getLocalAddress();
                        Intent intent = new Intent(MainActivity.this, WebPlayerActivity.class);
                        intent.putExtra("url", url);
                        startActivity(intent);
                    }
                }
            });
        }

        updateUi();

        // Ask to import YouTube subscriptions once
        final HistoryDbHelper db = HistoryDbHelper.getInstance(this);
        if ("false".equals(db.getSetting("asked_subscription_import", "false"))) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Import YouTube Subscriptions")
                .setMessage("Would you like to sign in and import your YouTube subscriptions? This will customize your personalized feed recommendations.")
                .setPositiveButton("Import Now", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        db.setSetting("asked_subscription_import", "true");
                        YoutubeSubscriptionImportFragment fragment = new YoutubeSubscriptionImportFragment();
                        fragment.show(getSupportFragmentManager(), "subscription_import");
                    }
                })
                .setNegativeButton("Later", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        db.setSetting("asked_subscription_import", "true");
                    }
                })
                .show();
        }
    }

    private void bindIntegrationPage(View view) {
        textLockStatus = view.findViewById(R.id.text_lock_status);
        btnForceRelease = view.findViewById(R.id.btn_force_release);
        cardPlayLock = view.findViewById(R.id.card_play_lock);
        btnLaunchRemote = view.findViewById(R.id.btn_launch_remote);
        btnSettings = view.findViewById(R.id.btn_settings);

        if (btnLaunchRemote != null) {
            btnLaunchRemote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, RemoteActivity.class);
                    startActivity(intent);
                }
            });
        }

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

        if (btnSettings != null) {
            btnSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                }
            });
        }

        updateLockUi();
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
            int colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.parseColor("#4CAF50"));
            int colorPrimaryContainer = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer, Color.parseColor("#132D1B"));
            
            cardStatus.setCardBackgroundColor(ColorStateList.valueOf(colorPrimaryContainer));
            cardStatus.setStrokeColor(ColorStateList.valueOf(colorPrimary));
            textStatus.setText("Status: Running");
            textStatus.setTextColor(colorPrimary);
            statusIndicator.setText("🟢");

            btnToggle.setText("stop");
            int colorError = MaterialColors.getColor(this, com.google.android.material.R.attr.colorError, Color.parseColor("#E53935"));
            btnToggle.setBackgroundTintList(ColorStateList.valueOf(colorError));
            btnOpenBrowser.setEnabled(true);

            String localIp = ServerService.getLocalIpAddress();
            textIpAddress.setText("IP Address: " + (localIp != null ? localIp : "Not Available"));
            String addressText = "Local Device: http://localhost:8080\n" +
                    (localIp != null ? "Network Link: http://" + localIp + ":8080" : "");
            textUrls.setText(addressText);
        } else {
            int colorError = MaterialColors.getColor(this, com.google.android.material.R.attr.colorError, Color.parseColor("#E53935"));
            int colorErrorContainer = MaterialColors.getColor(this, com.google.android.material.R.attr.colorErrorContainer, Color.parseColor("#2D1313"));
            
            cardStatus.setCardBackgroundColor(ColorStateList.valueOf(colorErrorContainer));
            cardStatus.setStrokeColor(ColorStateList.valueOf(colorError));
            textStatus.setText("Status: Stopped");
            textStatus.setTextColor(colorError);
            statusIndicator.setText("🔴");

            btnToggle.setText("start");
            int colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.parseColor("#4CAF50"));
            btnToggle.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
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
            int colorTertiary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiary, Color.parseColor("#E67E22"));
            int colorTertiaryContainer = MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiaryContainer, Color.parseColor("#2D1E13"));
            
            cardPlayLock.setCardBackgroundColor(ColorStateList.valueOf(colorTertiaryContainer));
            cardPlayLock.setStrokeColor(ColorStateList.valueOf(colorTertiary));
            
            String status = "Locked by: " + LocalHttpServer.getActiveClientIp();
            if (LocalHttpServer.getActiveVideoTitle() != null) {
                status += "\nPlaying: " + LocalHttpServer.getActiveVideoTitle();
            }
            textLockStatus.setText(status);
            textLockStatus.setTextColor(colorTertiary);
            btnForceRelease.setEnabled(true);
        } else {
            int colorSurfaceContainerLow = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerLow, Color.parseColor("#1E1E1E"));
            int colorOutlineVariant = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant, Color.parseColor("#2C2C2C"));
            int colorOnSurfaceVariant = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.parseColor("#888888"));
            
            cardPlayLock.setCardBackgroundColor(ColorStateList.valueOf(colorSurfaceContainerLow));
            cardPlayLock.setStrokeColor(ColorStateList.valueOf(colorOutlineVariant));
            textLockStatus.setText("Status: Unlocked / Idle");
            textLockStatus.setTextColor(colorOnSurfaceVariant);
            btnForceRelease.setEnabled(false);
        }

        if (btnLaunchRemote != null) {
            btnLaunchRemote.setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalHttpServer.setLockStatusListener(lockStatusListener);
        updateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalHttpServer.setLockStatusListener(null);
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
        super.onDestroy();
    }

    private class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == 0) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_control, parent, false);
                bindControlPage(view);
            } else {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_integration, parent, false);
                bindIntegrationPage(view);
            }
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // Static views are bound in onCreateViewHolder
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
