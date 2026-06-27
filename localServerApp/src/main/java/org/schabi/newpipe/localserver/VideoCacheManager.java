package org.schabi.newpipe.localserver;

import android.content.Context;
import android.util.Log;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VideoCacheManager {
    private static final String TAG = "VideoCacheManager";
    private static VideoCacheManager instance;
    private final Context context;
    private final HistoryDbHelper dbHelper;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final OkHttpClient client = new OkHttpClient();

    private VideoCacheManager(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = HistoryDbHelper.getInstance(this.context);
    }

    public static synchronized VideoCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new VideoCacheManager(context);
        }
        return instance;
    }

    public void startCaching(final String url, final int serviceId) {
        CachedVideo existing = dbHelper.getCachedVideo(url);
        if (existing != null) {
            if ("COMPLETED".equals(existing.getStatus()) || "DOWNLOADING".equals(existing.getStatus()) || "PENDING".equals(existing.getStatus())) {
                return;
            }
            dbHelper.updateCachedVideoProgress(url, "PENDING", 0, "", "");
        } else {
            dbHelper.addCachedVideo(url, "Fetching metadata...", "Unknown", "");
        }

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    dbHelper.updateCachedVideoProgress(url, "DOWNLOADING", 0, "", "");

                    StreamingService service = NewPipe.getService(serviceId);
                    StreamExtractor extractor = service.getStreamExtractor(url);
                    extractor.fetchPage();

                    String title = extractor.getName();
                    String uploader = extractor.getUploaderName();
                    String description = extractor.getDescription() != null ? extractor.getDescription().getContent() : "";

                    String thumbUrl = "";
                    if (extractor.getThumbnails() != null && !extractor.getThumbnails().isEmpty()) {
                        thumbUrl = extractor.getThumbnails().get(extractor.getThumbnails().size() - 1).getUrl();
                    }

                    File cacheDir = new File(context.getExternalFilesDir(null), "videocache");
                    if (!cacheDir.exists()) {
                        cacheDir.mkdirs();
                    }

                    String sanitizedName = url.replaceAll("[^a-zA-Z0-9]", "_");
                    File thumbFile = new File(cacheDir, sanitizedName + ".jpg");
                    if (!thumbUrl.isEmpty()) {
                        try {
                            downloadFile(thumbUrl, thumbFile, null);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to download thumbnail", e);
                        }
                    }

                    String directUrl = null;
                    String targetQuality = dbHelper.getVideoQuality();
                    int targetHeight = getResolutionHeight(targetQuality);
                    List<VideoStream> progressiveStreams = extractor.getVideoStreams();
                    if (progressiveStreams != null && !progressiveStreams.isEmpty()) {
                        VideoStream selectedStream = null;
                        int bestHeight = -1;
                        for (VideoStream stream : progressiveStreams) {
                            int height = getResolutionHeight(stream.getResolution());
                            if (height <= targetHeight) {
                                if (height > bestHeight) {
                                    bestHeight = height;
                                    selectedStream = stream;
                                }
                            }
                        }
                        if (selectedStream == null) {
                            // If no stream is <= targetHeight, pick the highest quality one available
                            for (VideoStream stream : progressiveStreams) {
                                int height = getResolutionHeight(stream.getResolution());
                                if (height > bestHeight) {
                                    bestHeight = height;
                                    selectedStream = stream;
                                }
                            }
                        }
                        if (selectedStream == null) {
                            selectedStream = progressiveStreams.get(0);
                        }
                        directUrl = selectedStream.getContent();
                    } else {
                        String hlsUrl = extractor.getHlsUrl();
                        if (hlsUrl != null && !hlsUrl.isEmpty()) {
                            directUrl = hlsUrl;
                        }
                    }

                    if (directUrl == null) {
                        throw new IOException("No suitable video stream found.");
                    }

                    File videoFile = new File(cacheDir, sanitizedName + ".mp4");

                    dbHelper.updateCachedVideoMetadata(url, title, uploader, description, thumbFile.getAbsolutePath());

                    final String targetUrl = url;
                    downloadFile(directUrl, videoFile, new ProgressListener() {
                        @Override
                        public void onProgress(int progress) {
                            dbHelper.updateCachedVideoProgress(targetUrl, "DOWNLOADING", progress, null, null);
                        }
                    });

                    dbHelper.updateCachedVideoProgress(url, "COMPLETED", 100, videoFile.getAbsolutePath(), thumbFile.getAbsolutePath());
                    LocalHttpServer.log("Cached video successfully: " + title);

                } catch (Exception e) {
                    Log.e(TAG, "Failed to cache video: " + url, e);
                    dbHelper.updateCachedVideoProgress(url, "FAILED", 0, "", "");
                    LocalHttpServer.log("Failed to cache video: " + url + " - " + e.getMessage());
                }
            }
        });
    }

    public void deleteCache(String url) {
        CachedVideo cached = dbHelper.getCachedVideo(url);
        if (cached != null) {
            if (cached.getVideoLocalPath() != null && !cached.getVideoLocalPath().isEmpty()) {
                File vFile = new File(cached.getVideoLocalPath());
                if (vFile.exists()) {
                    vFile.delete();
                }
            }
            if (cached.getThumbnailLocalPath() != null && !cached.getThumbnailLocalPath().isEmpty()) {
                File tFile = new File(cached.getThumbnailLocalPath());
                if (tFile.exists()) {
                    tFile.delete();
                }
            }
            dbHelper.deleteCachedVideo(url);
            LocalHttpServer.log("Deleted cached video: " + cached.getTitle());
        }
    }

    interface ProgressListener {
        void onProgress(int progress);
    }

    private void downloadFile(String fileUrl, File destination, ProgressListener progressListener) throws IOException {
        Request request = new Request.Builder().url(fileUrl).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected response code " + response.code());

            try (InputStream is = response.body().byteStream();
                 FileOutputStream fos = new FileOutputStream(destination)) {

                long totalBytes = response.body().contentLength();
                byte[] buffer = new byte[8192];
                int read;
                long downloadedBytes = 0;
                long lastProgressTime = 0;

                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    downloadedBytes += read;

                    if (progressListener != null && totalBytes > 0) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastProgressTime > 500) {
                            int progress = (int) ((downloadedBytes * 100) / totalBytes);
                            progressListener.onProgress(progress);
                            lastProgressTime = currentTime;
                        }
                    }
                }
                fos.flush();
            }
        }
    }

    private int getResolutionHeight(String resolution) {
        if (resolution == null || resolution.isEmpty()) return 0;
        try {
            // Split by 'p' (e.g. "720p60" -> "720") to ignore frame rate
            String[] parts = resolution.split("(?i)p");
            if (parts.length > 0) {
                String numeric = parts[0].replaceAll("[^0-9]", "");
                return numeric.isEmpty() ? 0 : Integer.parseInt(numeric);
            }
        } catch (Exception e) {
            // fallback
        }
        return 0;
    }
}
