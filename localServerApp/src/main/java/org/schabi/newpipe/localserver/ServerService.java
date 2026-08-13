package org.schabi.newpipe.localserver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import org.schabi.newpipe.extractor.NewPipe;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class ServerService extends Service {

    private static final String CHANNEL_ID = "LocalServerChannel";
    private static final int NOTIFICATION_ID = 1204;
    private static final int PORT = 8080;

    private LocalHttpServer server;
    private boolean isRunning = false;
    private final IBinder binder = new LocalBinder();
    private ServerStatusListener statusListener;
    private android.os.PowerManager.WakeLock wakeLock;
    private android.net.wifi.WifiManager.WifiLock wifiLock;

    public interface ServerStatusListener {
        void onStatusChanged(boolean isRunning);
    }

    public void setStatusListener(ServerStatusListener listener) {
        this.statusListener = listener;
    }

    public class LocalBinder extends Binder {
        ServerService getService() {
            return ServerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    public static final String ACTION_PLAY = "org.schabi.newpipe.localserver.ACTION_PLAY";
    public static final String ACTION_PAUSE = "org.schabi.newpipe.localserver.ACTION_PAUSE";
    public static final String ACTION_STOP = "org.schabi.newpipe.localserver.ACTION_STOP";

    private android.media.MediaPlayer mediaPlayer;
    private String currentAudioTitle = "";
    private String currentAudioArtist = "";
    private String currentAudioUrl = "";
    private boolean isAudioPlaying = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_PAUSE.equals(action)) {
                pauseNativeAudio();
            } else if (ACTION_PLAY.equals(action)) {
                resumeNativeAudio();
            } else if (ACTION_STOP.equals(action)) {
                stopNativeAudio();
            }
        }
        if (!isRunning) {
            startServer();
        }
        return START_NOT_STICKY;
    }

    private void startServer() {
        // Query wallpaper dynamic colors and supply to HtmlRenderer for web client theme mapping
        HtmlRenderer.lightColors = DynamicColorHelper.getThemeColors(this, false);
        HtmlRenderer.darkColors = DynamicColorHelper.getThemeColors(this, true);

        // Setup foreground notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String localIp = getLocalIpAddress();
        String addressText = localIp != null ? "http://" + localIp + ":" + PORT : "http://localhost:" + PORT;

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LocalTube Running")
                .setContentText("Listening on: " + addressText)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentIntent(pendingIntent)
                .build();

        // Support Android 14 API 34+ foreground service types
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK | ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        try {
            // 1. Initialize NewPipe Extractor
            NewPipe.init(new ServerDownloader());
            org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor.setFetchIosClient(true);

            // 2. Start HTTP Server
            server = new LocalHttpServer(this, PORT);
            server.startServer();
            isRunning = true;

            try {
                android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
                if (pm != null && (wakeLock == null || !wakeLock.isHeld())) {
                    wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "LocalTube::BackgroundAudioWakeLock");
                    wakeLock.acquire();
                }
                android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                if (wm != null && (wifiLock == null || !wifiLock.isHeld())) {
                    wifiLock = wm.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LocalTube::WifiLock");
                    wifiLock.acquire();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            LocalHttpServer.log("Local server running at: " + getLocalAddress());
            if (statusListener != null) {
                statusListener.onStatusChanged(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LocalHttpServer.setLogListener(null);
            stopSelf();
            if (statusListener != null) {
                statusListener.onStatusChanged(false);
            }
        }
    }

    public void stopServer() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception ignored) {}
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            try {
                wifiLock.release();
            } catch (Exception ignored) {}
        }
        if (server != null) {
            server.stopServer();
        }
        isRunning = false;
        stopForeground(true);
        stopSelf();
        if (statusListener != null) {
            statusListener.onStatusChanged(false);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public String getLocalAddress() {
        String localIp = getLocalIpAddress();
        return localIp != null ? "http://" + localIp + ":" + PORT : "http://127.0.0.1:" + PORT;
    }

    public void playNativeAudio(String url, String title, String artist) {
        stopNativeAudio();
        this.currentAudioUrl = url;
        this.currentAudioTitle = title != null ? title : "Audio Stream";
        this.currentAudioArtist = artist != null ? artist : "LocalTube";

        try {
            mediaPlayer = new android.media.MediaPlayer();
            mediaPlayer.setAudioAttributes(
                new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .build()
            );
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isAudioPlaying = true;
                updateNotification(currentAudioTitle, "Playing: " + currentAudioArtist);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                isAudioPlaying = false;
                updateNotification("LocalTube Running", "Listening on: " + getLocalAddress());
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isAudioPlaying = false;
                return false;
            });
            mediaPlayer.prepareAsync();
            updateNotification(currentAudioTitle, "Loading audio...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pauseNativeAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isAudioPlaying = false;
            updateNotification(currentAudioTitle, "Paused: " + currentAudioArtist);
        }
    }

    public void resumeNativeAudio() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isAudioPlaying = true;
            updateNotification(currentAudioTitle, "Playing: " + currentAudioArtist);
        }
    }

    public void stopNativeAudio() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        isAudioPlaying = false;
    }

    public boolean isAudioPlaying() {
        return mediaPlayer != null && isAudioPlaying;
    }

    public String getCurrentAudioTitle() {
        return currentAudioTitle;
    }

    public String getCurrentAudioArtist() {
        return currentAudioArtist;
    }

    public int getAudioPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public void seekNativeAudio(int positionMs) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(positionMs);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int getAudioDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    private void updateNotification(String title, String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentIntent(pendingIntent)
                .setOngoing(isAudioPlaying);



        if (isAudioPlaying) {
            Intent pauseIntent = new Intent(this, ServerService.class).setAction(ACTION_PAUSE);
            PendingIntent pPause = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pPause);
        } else if (mediaPlayer != null) {
            Intent playIntent = new Intent(this, ServerService.class).setAction(ACTION_PLAY);
            PendingIntent pPlay = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(android.R.drawable.ic_media_play, "Play", pPlay);
        }

        Notification notification = builder.build();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        stopNativeAudio();
        stopServer();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "LocalTube Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
