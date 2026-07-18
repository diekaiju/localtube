package org.schabi.newpipe.localserver;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure NewPipe is initialized
        try {
            NewPipe.init(new ServerDownloader());
        } catch (Exception e) {
            // Already initialized or failed
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    final String url = extractUrl(sharedText);
                    if (url != null) {
                        handleSharedUrl(url);
                        return;
                    }
                }
            }
        }

        Toast.makeText(this, "No valid link found to share", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String extractUrl(String text) {
        Pattern pattern = Pattern.compile("https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private void handleSharedUrl(final String url) {
        final List<LocalHttpServer.ClientInfo> clients = LocalHttpServer.getConnectedClients();

        if (clients.isEmpty()) {
            // No clients connected, automatically save to Watch Later
            Toast.makeText(this, "No clients connected. Saving to Watch Later...", Toast.LENGTH_SHORT).show();
            saveToWatchLater(url);
            return;
        }

        // Find the active client connected with the remote (matching active IP)
        LocalHttpServer.ClientInfo targetClient = null;
        String activeIp = LocalHttpServer.getActiveClientIp();
        if (activeIp != null) {
            for (LocalHttpServer.ClientInfo client : clients) {
                try {
                    String clientIp = client.connection.getRemoteSocketAddress().getAddress().getHostAddress();
                    if (activeIp.equals(clientIp)) {
                        targetClient = client;
                        break;
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        // Fallback to the first connected client if none matches the active remote
        if (targetClient == null) {
            targetClient = clients.get(0);
        }

        LocalHttpServer.castToClient(targetClient.connection, url);
        Toast.makeText(this, "Playing on " + targetClient.name, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveToWatchLater(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StreamingService service = NewPipe.getService(0); // YouTube service
                    HistoryDbHelper dbHelper = HistoryDbHelper.getInstance(ShareActivity.this);

                    String title = "Shared Item";
                    String uploader = "";
                    String thumbnailUrl = "";
                    String type = "video";

                    if (url.contains("list=") && !url.contains("watch?v=")) {
                        // Playlist
                        type = "playlist";
                        PlaylistExtractor extractor = service.getPlaylistExtractor(url);
                        extractor.fetchPage();
                        title = extractor.getName();
                        uploader = extractor.getUploaderName();
                        if (extractor.getInitialPage() != null && extractor.getInitialPage().getItems() != null && !extractor.getInitialPage().getItems().isEmpty()) {
                            org.schabi.newpipe.extractor.InfoItem firstItem = (org.schabi.newpipe.extractor.InfoItem) extractor.getInitialPage().getItems().get(0);
                            if (firstItem.getThumbnails() != null && !firstItem.getThumbnails().isEmpty()) {
                                thumbnailUrl = firstItem.getThumbnails().get(firstItem.getThumbnails().size() - 1).getUrl();
                            }
                        }
                    } else {
                        // Video
                        type = "video";
                        StreamInfo info = StreamInfo.getInfo(service, url);
                        title = info.getName();
                        uploader = info.getUploaderName();
                        if (info.getThumbnails() != null && !info.getThumbnails().isEmpty()) {
                            thumbnailUrl = info.getThumbnails().get(info.getThumbnails().size() - 1).getUrl();
                        }
                    }

                    dbHelper.addWatchLater(url, title, uploader, thumbnailUrl, type);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ShareActivity.this, "Saved successfully to Watch Later!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Fallback save in case network fails or link is custom
                            HistoryDbHelper dbHelper = HistoryDbHelper.getInstance(ShareActivity.this);
                            String title = "Shared Video Link";
                            String type = url.contains("list=") ? "playlist" : "video";
                            dbHelper.addWatchLater(url, title, "", "", type);
                            Toast.makeText(ShareActivity.this, "Saved link to Watch Later (metadata fetch failed)", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            }
        }).start();
    }
}
