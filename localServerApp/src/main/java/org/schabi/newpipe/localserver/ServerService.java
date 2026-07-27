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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        try {
            // 1. Initialize NewPipe Extractor
            NewPipe.init(new ServerDownloader());

            // 2. Start HTTP Server
            server = new LocalHttpServer(this, PORT);
            server.startServer();
            isRunning = true;
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

    @Override
    public void onDestroy() {
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
