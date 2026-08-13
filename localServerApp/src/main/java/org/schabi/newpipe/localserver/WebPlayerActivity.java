package org.schabi.newpipe.localserver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class WebPlayerActivity extends AppCompatActivity {

    private WebView webView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private final WebChromeClient webChromeClient = new WebChromeClient() {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }
            customView = view;
            customViewCallback = callback;
            
            webView.setVisibility(View.GONE);
            
            ViewGroup decor = (ViewGroup) getWindow().getDecorView();
            decor.addView(customView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        @Override
        public void onHideCustomView() {
            if (customView == null) {
                return;
            }
            
            ViewGroup decor = (ViewGroup) getWindow().getDecorView();
            decor.removeView(customView);
            customView = null;
            
            webView.setVisibility(View.VISIBLE);
            
            if (customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
            }
            
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (serverService != null && !url.contains("/audio")) {
                    try {
                        serverService.stopNativeAudio();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(
                    "Object.defineProperty(document, 'visibilityState', {get: () => 'visible', configurable: true});\n" +
                    "Object.defineProperty(document, 'hidden', {get: () => false, configurable: true});\n" +
                    "window.addEventListener('visibilitychange', (e) => e.stopImmediatePropagation(), true);",
                    null
                );
            }
        });

        webView.addJavascriptInterface(new AppInterface(), "NewPipeApp");
        webView.setWebChromeClient(webChromeClient);

        try {
            Intent serviceIntent = new Intent(this, ServerService.class);
            bindService(serviceIntent, serviceConnection, android.content.Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        handleIntent(getIntent());
    }

    private ServerService serverService;
    private boolean isBound = false;

    private final android.content.ServiceConnection serviceConnection = new android.content.ServiceConnection() {
        @Override
        public void onServiceConnected(android.content.ComponentName name, android.os.IBinder service) {
            ServerService.LocalBinder binder = (ServerService.LocalBinder) service;
            serverService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(android.content.ComponentName name) {
            serverService = null;
            isBound = false;
        }
    };

    public class AppInterface {
        @android.webkit.JavascriptInterface
        public void enterPip() {
            runOnUiThread(() -> enterPipMode());
        }

        @android.webkit.JavascriptInterface
        public void playNativeAudio(String url, String title, String artist) {
            if (serverService != null) {
                serverService.playNativeAudio(url, title, artist);
            }
        }

        @android.webkit.JavascriptInterface
        public void pauseNativeAudio() {
            if (serverService != null) {
                serverService.pauseNativeAudio();
            }
        }

        @android.webkit.JavascriptInterface
        public void resumeNativeAudio() {
            if (serverService != null) {
                serverService.resumeNativeAudio();
            }
        }

        @android.webkit.JavascriptInterface
        public void stopNativeAudio() {
            if (serverService != null) {
                serverService.stopNativeAudio();
            }
        }
    }

    public void enterPipMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                android.app.PictureInPictureParams.Builder builder = new android.app.PictureInPictureParams.Builder();
                android.util.Rational aspectRatio = new android.util.Rational(16, 9);
                builder.setAspectRatio(aspectRatio);
                enterPictureInPictureMode(builder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (webView != null && webView.getUrl() != null && webView.getUrl().contains("/watch")) {
                enterPipMode();
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            if (webView != null) {
                webView.evaluateJavascript("document.body.classList.add('pip-mode');", null);
            }
        } else {
            if (webView != null) {
                webView.evaluateJavascript("document.body.classList.remove('pip-mode');", null);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.resumeTimers();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (webView != null) {
            webView.resumeTimers();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra("url")) {
                webView.loadUrl(intent.getStringExtra("url"));
            } else if (intent.hasExtra("video_url")) {
                String videoUrl = intent.getStringExtra("video_url");
                String watchUrl = "http://localhost:8080/watch?serviceId=0&id=" + android.net.Uri.encode(videoUrl);
                webView.loadUrl(watchUrl);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            webChromeClient.onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            try {
                webView.stopLoading();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (serverService != null) {
                try {
                    serverService.stopNativeAudio();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            webView.destroy();
            webView = null;
        }
        try {
            android.webkit.CookieManager.getInstance().removeAllCookies(null);
            android.webkit.CookieManager.getInstance().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (isBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isBound = false;
        }
        super.onDestroy();
    }
}
