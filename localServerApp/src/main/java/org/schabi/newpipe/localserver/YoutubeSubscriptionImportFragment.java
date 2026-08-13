package org.schabi.newpipe.localserver;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.json.JSONArray;
import org.json.JSONObject;

public class YoutubeSubscriptionImportFragment extends DialogFragment {

    private TextView statusText;
    private ProgressBar progressBar;
    private WebView webView;
    private android.view.View cancelButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_youtube_import, container, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusText = view.findViewById(R.id.import_status_text);
        progressBar = view.findViewById(R.id.import_progress_bar);
        webView = view.findViewById(R.id.import_webview);
        cancelButton = view.findViewById(R.id.import_cancel_button);

        cancelButton.setOnClickListener(v -> dismiss());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new ScraperInterface(), "NewPipeApp");
        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("SubscriptionImport", "Finished loading: " + url);

                if (url.contains("accounts.google.com") || url.contains("signin")) {
                    statusText.setText("Please sign in to your YouTube/Google account.");
                    progressBar.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                } else if (url.contains("youtube.com/feed/channels")) {
                    webView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    statusText.setText("Logged in. Auto-scrolling and scraping subscriptions...");
                    injectScraperScript();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d("SubscriptionImport", "Loading URL: " + request.getUrl().toString());
                return false;
            }
        });

        // Start by loading mobile YouTube subscriptions list page
        webView.loadUrl("https://m.youtube.com/feed/channels?ra=m");
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    private void injectScraperScript() {
        String js = "javascript:(function() {\n" +
                "    var lastHeight = document.body.scrollHeight;\n" +
                "    var scrollAttempts = 0;\n" +
                "    var maxAttempts = 30;\n" +
                "    \n" +
                "    function scrollAndScrape() {\n" +
                "        window.scrollTo(0, document.body.scrollHeight);\n" +
                "        \n" +
                "        setTimeout(function() {\n" +
                "            var newHeight = document.body.scrollHeight;\n" +
                "            if (newHeight > lastHeight && scrollAttempts < maxAttempts) {\n" +
                "                lastHeight = newHeight;\n" +
                "                scrollAttempts++;\n" +
                "                scrollAndScrape();\n" +
                "            } else {\n" +
                "                var channels = [];\n" +
                "                var anchors = document.querySelectorAll('a');\n" +
                "                anchors.forEach(function(a) {\n" +
                "                    var href = a.getAttribute('href');\n" +
                "                    if (!href) return;\n" +
                "                    var isChannel = href.indexOf('/channel/') !== -1 \n" +
                "                                 || href.indexOf('/c/') !== -1 \n" +
                "                                 || href.indexOf('/user/') !== -1\n" +
                "                                 || href.startsWith('/@');\n" +
                "                    if (isChannel) {\n" +
                "                        var name = a.innerText ? a.innerText.trim() : '';\n" +
                "                        if (!name) {\n" +
                "                            var img = a.querySelector('img');\n" +
                "                            if (img && img.alt) {\n" +
                "                                name = img.alt.trim();\n" +
                "                            }\n" +
                "                        }\n" +
                "                        if (name) {\n" +
                "                            name = name.split('\\n')[0].trim();\n" +
                "                        }\n" +
                "                        var avatar = '';\n" +
                "                        var img = a.querySelector('img');\n" +
                "                        if (img) {\n" +
                "                            avatar = img.src || '';\n" +
                "                            if (avatar.startsWith('data:image') || avatar.indexOf('clear.png') !== -1) {\n" +
                "                                avatar = img.getAttribute('data-thumb') \n" +
                "                                      || img.getAttribute('data-src') \n" +
                "                                      || img.getAttribute('thumb') \n" +
                "                                      || '';\n" +
                "                            }\n" +
                "                        }\n" +
                "                        var absoluteUrl = a.href || '';\n" +
                "                        if (absoluteUrl && absoluteUrl.indexOf('m.youtube.com') !== -1) {\n" +
                "                            absoluteUrl = absoluteUrl.replace('m.youtube.com', 'www.youtube.com');\n" +
                "                        }\n" +
                "                        if (absoluteUrl.endsWith('/')) {\n" +
                "                            absoluteUrl = absoluteUrl.substring(0, absoluteUrl.length - 1);\n" +
                "                        }\n" +
                "                        if (absoluteUrl && name && !channels.some(function(c) { return c.url === absoluteUrl; })) {\n" +
                "                            if (name !== 'Home' && name !== 'Subscriptions' && name !== 'Library' && name !== 'Trending') {\n" +
                "                                channels.push({\n" +
                "                                    url: absoluteUrl,\n" +
                "                                    name: name,\n" +
                "                                    avatar: avatar\n" +
                "                                });\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                });\n" +
                "                window.NewPipeApp.onSubscriptionsScraped(JSON.stringify(channels));\n" +
                "            }\n" +
                "        }, 1000);\n" +
                "    }\n" +
                "    \n" +
                "    scrollAndScrape();\n" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    private class ScraperInterface {
        @JavascriptInterface
        public void onSubscriptionsScraped(String json) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                try {
                    JSONArray channelsArray = new JSONArray(json);
                    HistoryDbHelper db = HistoryDbHelper.getInstance(getContext());
                    for (int i = 0; i < channelsArray.length(); i++) {
                        JSONObject obj = channelsArray.getJSONObject(i);
                        String url = obj.getString("url");
                        String name = obj.getString("name");
                        String avatar = obj.optString("avatar", "");
                        db.addSubscription(url, name, avatar);
                    }
                    Log.d("SubscriptionImport", "Scraped and saved " + channelsArray.length() + " subscriptions.");
                    if (channelsArray.length() > 0) {
                        Toast.makeText(getContext(), "Imported " + channelsArray.length() + " subscriptions successfully!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "No subscriptions found.", Toast.LENGTH_SHORT).show();
                    }
                    dismiss();
                } catch (Exception e) {
                    Log.e("SubscriptionImport", "Failed to parse scraped channels", e);
                    Toast.makeText(getContext(), "Failed to parse subscriptions.", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        }
    }
}
