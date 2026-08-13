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
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.stream.StreamExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class YoutubeInterestsImportFragment extends DialogFragment {

    private TextView statusText;
    private ProgressBar progressBar;
    private WebView webView;
    private android.view.View cancelButton;
    private OnImportCompleteListener listener;

    private boolean isLoginVerified = false;

    public interface OnImportCompleteListener {
        void onImportComplete();
    }

    public void setOnImportCompleteListener(OnImportCompleteListener listener) {
        this.listener = listener;
    }

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

        statusText.setText("Loading YouTube...");
        cancelButton.setOnClickListener(v -> dismiss());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new ScraperInterface(), "NewPipeApp");
        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("InterestsImport", "Finished loading: " + url);

                if (url.contains("accounts.google.com") || url.contains("signin")) {
                    statusText.setText("Please sign in to your YouTube/Google account.");
                    progressBar.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                } else if (url.contains("youtube.com/feed/channels")) {
                    isLoginVerified = true;
                    statusText.setText("Logged in. Loading personalized home feed...");
                    progressBar.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                    webView.loadUrl("https://m.youtube.com/");
                } else if (isLoginVerified && (url.equals("https://m.youtube.com/") || url.equals("https://m.youtube.com") || url.contains("m.youtube.com/?") || url.contains("m.youtube.com/index"))) {
                    statusText.setText("Scraping recommended videos...");
                    progressBar.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                    injectScraperScript();
                } else if (url.contains("youtube.com")) {
                    checkLoginStatus();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        // Start by loading protected channels page to trigger sign-in redirect if needed
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

    private void checkLoginStatus() {
        String js = "javascript:(function() {\n" +
                "    var signInButton = document.querySelector('a[href*=\"/signin\"]') \n" +
                "                    || document.querySelector('button[aria-label*=\"Sign in\"]')\n" +
                "                    || document.querySelector('.signin-button');\n" +
                "    window.NewPipeApp.onLoginStatusChecked(signInButton == null);\n" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    private void injectScraperScript() {
        String js = "javascript:(function() {\n" +
                "    function scrapeWithRetry(attempts) {\n" +
                "        var urls = [];\n" +
                "        var anchors = document.querySelectorAll('a');\n" +
                "        anchors.forEach(function(a) {\n" +
                "            var href = a.getAttribute('href');\n" +
                "            if (!href) return;\n" +
                "            if (href.indexOf('/watch?v=') !== -1 || href.indexOf('/shorts/') !== -1) {\n" +
                "                var absoluteUrl = a.href;\n" +
                "                if (absoluteUrl && !urls.some(function(u) { return u === absoluteUrl; })) {\n" +
                "                    urls.push(absoluteUrl);\n" +
                "                }\n" +
                "            }\n" +
                "        });\n" +
                "        if (urls.length < 5 && attempts < 10) {\n" +
                "            setTimeout(function() { scrapeWithRetry(attempts + 1); }, 1000);\n" +
                "        } else {\n" +
                "            window.NewPipeApp.onVideosScraped(JSON.stringify(urls));\n" +
                "        }\n" +
                "    }\n" +
                "    scrapeWithRetry(0);\n" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    private class ScraperInterface {
        @JavascriptInterface
        public void onLoginStatusChecked(boolean isLoggedIn) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (isLoggedIn) {
                    webView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    statusText.setText("Logged in. Scraping recommended videos...");
                    injectScraperScript();
                } else {
                    statusText.setText("Please sign in to continue.");
                    progressBar.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                }
            });
        }

        @JavascriptInterface
        public void onVideosScraped(String jsonUrls) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                try {
                    JSONArray arr = new JSONArray(jsonUrls);
                    List<String> urls = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        urls.add(arr.getString(i));
                    }
                    
                    if (urls.isEmpty()) {
                        statusText.setText("No recommended videos found on homepage. Try scrolling or refreshing.");
                        progressBar.setVisibility(View.GONE);
                        webView.setVisibility(View.VISIBLE);
                        return;
                    }

                    statusText.setText("Found " + urls.size() + " videos. Analyzing recommendation tags...");
                    progressBar.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);

                    // Start background thread to extract tags
                    new Thread(() -> processVideoTags(urls)).start();

                } catch (Exception e) {
                    Log.e("InterestsImport", "Scraping failed", e);
                    Toast.makeText(getContext(), "Scraping failed.", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        }
    }

    private void processVideoTags(List<String> urls) {
        List<String> allTags = new ArrayList<>();
        int count = 0;
        int maxVideosToProcess = Math.min(urls.size(), 24); // Process top 24 videos for broader interest coverage

        for (int i = 0; i < maxVideosToProcess; i++) {
            String url = urls.get(i);
            final int currentProgress = i + 1;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    statusText.setText("Analyzing video " + currentProgress + "/" + maxVideosToProcess + "...");
                });
            }

            try {
                StreamingService service = NewPipe.getService(0);
                StreamExtractor extractor = service.getStreamExtractor(url);
                extractor.fetchPage();
                List<String> tags = extractor.getTags();
                if (tags != null) {
                    for (String tag : tags) {
                        String clean = tag.trim();
                        if (!clean.isEmpty()) {
                            allTags.add(clean);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("InterestsImport", "Error extracting tags for: " + url, e);
            }
        }

        // Count tag frequencies
        Map<String, Integer> freqMap = new HashMap<>();
        for (String tag : allTags) {
            String key = tag.toLowerCase();
            freqMap.put(key, freqMap.getOrDefault(key, 0) + 1);
        }

        // We want to add preferred keywords. We'll map the lowercase keys back to the original casing
        Map<String, String> originalCasing = new HashMap<>();
        for (String tag : allTags) {
            originalCasing.put(tag.toLowerCase(), tag);
        }

        List<String> importedInterests = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {
            if (entry.getValue() == 1) { // Unique keywords that appear only one time
                String originalTag = originalCasing.get(entry.getKey());
                if (originalTag != null) {
                    importedInterests.add(originalTag);
                }
            }
        }

        final List<String> finalImported = importedInterests;
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (finalImported.isEmpty()) {
                    Toast.makeText(getContext(), "No tags found to import.", Toast.LENGTH_LONG).show();
                } else {
                    // Save to DB
                    HistoryDbHelper db = HistoryDbHelper.getInstance(getContext());
                    Set<String> existing = new HashSet<>(db.getPreferredKeywords());
                    int addedCount = 0;
                    for (String interest : finalImported) {
                        if (!existing.contains(interest)) {
                            existing.add(interest);
                            addedCount++;
                        }
                    }
                    if (addedCount > 0) {
                        StringBuilder sb = new StringBuilder();
                        for (String topic : existing) {
                            sb.append(topic).append("\n");
                        }
                        db.setSetting("preferred_keywords", sb.toString());
                        Toast.makeText(getContext(), "Successfully imported " + addedCount + " new interests!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "All imported interests are already followed.", Toast.LENGTH_SHORT).show();
                    }
                }
                if (listener != null) {
                    listener.onImportComplete();
                }
                dismiss();
            });
        }
    }
}
