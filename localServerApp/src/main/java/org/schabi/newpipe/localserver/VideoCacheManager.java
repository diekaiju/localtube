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
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectionPool(new okhttp3.ConnectionPool(10, 5, java.util.concurrent.TimeUnit.MINUTES))
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

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
        startCaching(url, serviceId, null, null);
    }

    public void startCaching(final String url, final int serviceId, final String preferredQuality, final String preferredAudioTrack) {
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

                    VideoStream selectedVideo = null;
                    org.schabi.newpipe.extractor.stream.AudioStream selectedAudio = null;

                    String targetQuality = (preferredQuality != null && !preferredQuality.isEmpty()) ? preferredQuality : dbHelper.getVideoQuality();
                    int targetHeight = getResolutionHeight(targetQuality);

                    // 1. Look for video-only streams (high quality)
                    List<VideoStream> videoOnlyStreams = extractor.getVideoOnlyStreams();
                    if (videoOnlyStreams != null && !videoOnlyStreams.isEmpty()) {
                        int bestHeight = -1;
                        for (VideoStream stream : videoOnlyStreams) {
                            // Prefer MP4 format for native MediaMuxer compatibility
                            if (stream.getFormat() != org.schabi.newpipe.extractor.MediaFormat.MPEG_4) {
                                continue;
                            }
                            int height = getResolutionHeight(stream.getResolution());
                            if (height <= targetHeight) {
                                if (height > bestHeight) {
                                    bestHeight = height;
                                    selectedVideo = stream;
                                }
                            }
                        }
                        if (selectedVideo == null) {
                            for (VideoStream stream : videoOnlyStreams) {
                                if (stream.getFormat() == org.schabi.newpipe.extractor.MediaFormat.MPEG_4) {
                                    int height = getResolutionHeight(stream.getResolution());
                                    if (height > bestHeight) {
                                        bestHeight = height;
                                        selectedVideo = stream;
                                    }
                                }
                            }
                        }
                    }

                    // 2. Look for audio stream (prefer M4A for mp4 muxing, and pick original/preferred audio)
                    List<org.schabi.newpipe.extractor.stream.AudioStream> audioStreams = extractor.getAudioStreams();
                    if (audioStreams != null && !audioStreams.isEmpty()) {
                        List<org.schabi.newpipe.extractor.stream.AudioStream> m4aStreams = audioStreams.stream()
                                .filter(as -> as.getFormat() == org.schabi.newpipe.extractor.MediaFormat.M4A)
                                .collect(java.util.stream.Collectors.toList());
                        if (m4aStreams.isEmpty()) {
                            m4aStreams = new java.util.ArrayList<>(audioStreams);
                        }

                        Log.d(TAG, "startCaching - preferredQuality: " + preferredQuality + ", preferredAudioTrack: " + preferredAudioTrack);

                        // Determine preferred track ID
                        final String finalPreferredAudioTrack = (preferredAudioTrack != null) ? preferredAudioTrack : "";

                        // Check if we have an exact match for the preferred track ID across all formats, preferring M4A
                        for (org.schabi.newpipe.extractor.stream.AudioStream stream : audioStreams) {
                            String trackId = stream.getAudioTrackId();
                            if (trackId == null) {
                                trackId = "";
                            }
                            if (java.util.Objects.equals(trackId, finalPreferredAudioTrack)) {
                                if (selectedAudio == null || stream.getFormat() == org.schabi.newpipe.extractor.MediaFormat.M4A) {
                                    selectedAudio = stream;
                                }
                            }
                        }

                        // If no exact match (or no preferred track passed), use our priority sorting logic on M4A streams
                        if (selectedAudio == null) {
                            java.util.Locale preferredLanguage = java.util.Locale.getDefault();
                            String langCode = preferredLanguage.getISO3Language();
                            java.util.Collections.sort(m4aStreams, (a, b) -> {
                                org.schabi.newpipe.extractor.stream.AudioTrackType typeA = a.getAudioTrackType();
                                org.schabi.newpipe.extractor.stream.AudioTrackType typeB = b.getAudioTrackType();
                                boolean isOrigA = (typeA == org.schabi.newpipe.extractor.stream.AudioTrackType.ORIGINAL);
                                boolean isOrigB = (typeB == org.schabi.newpipe.extractor.stream.AudioTrackType.ORIGINAL);
                                if (isOrigA != isOrigB) {
                                    return isOrigA ? -1 : 1;
                                }
                                java.util.Locale localeA = a.getAudioLocale();
                                java.util.Locale localeB = b.getAudioLocale();
                                boolean langMatchA = (localeA != null && localeA.getISO3Language().equals(langCode));
                                boolean langMatchB = (localeB != null && localeB.getISO3Language().equals(langCode));
                                if (langMatchA != langMatchB) {
                                    return langMatchA ? -1 : 1;
                                }
                                int scoreA = (typeA == org.schabi.newpipe.extractor.stream.AudioTrackType.ORIGINAL) ? 4 :
                                             (typeA == null ? 3 :
                                             (typeA == org.schabi.newpipe.extractor.stream.AudioTrackType.DUBBED ? 2 :
                                             (typeA == org.schabi.newpipe.extractor.stream.AudioTrackType.SECONDARY ? 1 : 0)));
                                int scoreB = (typeB == org.schabi.newpipe.extractor.stream.AudioTrackType.ORIGINAL) ? 4 :
                                             (typeB == null ? 3 :
                                             (typeB == org.schabi.newpipe.extractor.stream.AudioTrackType.DUBBED ? 2 :
                                             (typeB == org.schabi.newpipe.extractor.stream.AudioTrackType.SECONDARY ? 1 : 0)));
                                if (scoreA != scoreB) {
                                    return Integer.compare(scoreB, scoreA);
                                }
                                boolean engMatchA = (localeA != null && localeA.getISO3Language().equals("eng"));
                                boolean engMatchB = (localeB != null && localeB.getISO3Language().equals("eng"));
                                if (engMatchA != engMatchB) {
                                    return engMatchA ? -1 : 1;
                                }
                                long brA = a.getAverageBitrate() > 0 ? a.getAverageBitrate() : a.getBitrate();
                                long brB = b.getAverageBitrate() > 0 ? b.getAverageBitrate() : b.getBitrate();
                                return Long.compare(brB, brA);
                            });

                            selectedAudio = m4aStreams.get(0);
                        }
                        Log.d(TAG, "startCaching - selectedVideo: " + (selectedVideo != null ? selectedVideo.getResolution() : "null") + ", selectedAudio: " + (selectedAudio != null ? selectedAudio.getAudioTrackId() + " (" + selectedAudio.getFormat() + ")" : "null"));
                    }

                    File videoFile = new File(cacheDir, sanitizedName + ".mp4");

                    dbHelper.updateCachedVideoMetadata(url, title, uploader, description, thumbFile.getAbsolutePath());

                    final String targetUrl = url;

                    if (selectedVideo != null && selectedAudio != null) {
                        File tempVideoFile = new File(cacheDir, sanitizedName + "_temp_video.mp4");
                        File tempAudioFile = new File(cacheDir, sanitizedName + "_temp_audio.m4a");

                        final VideoStream finalVideo = selectedVideo;
                        final org.schabi.newpipe.extractor.stream.AudioStream finalAudio = selectedAudio;

                        try {
                            final int[] videoProgress = {0};
                            final int[] audioProgress = {0};

                            java.util.concurrent.Future<?> videoFuture = executor.submit(() -> {
                                try {
                                    downloadFile(finalVideo.getContent(), tempVideoFile, progress -> {
                                        videoProgress[0] = progress;
                                        int totalProgress = (int) (videoProgress[0] * 0.70 + audioProgress[0] * 0.25);
                                        dbHelper.updateCachedVideoProgress(targetUrl, "DOWNLOADING", totalProgress, null, null);
                                    });
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                            java.util.concurrent.Future<?> audioFuture = executor.submit(() -> {
                                try {
                                    downloadFile(finalAudio.getContent(), tempAudioFile, progress -> {
                                        audioProgress[0] = progress;
                                        int totalProgress = (int) (videoProgress[0] * 0.70 + audioProgress[0] * 0.25);
                                        dbHelper.updateCachedVideoProgress(targetUrl, "DOWNLOADING", totalProgress, null, null);
                                    });
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                            videoFuture.get();
                            audioFuture.get();

                            // Mux them
                            dbHelper.updateCachedVideoProgress(targetUrl, "DOWNLOADING", 96, null, null);
                            mux(tempVideoFile.getAbsolutePath(), tempAudioFile.getAbsolutePath(), videoFile.getAbsolutePath());
                            dbHelper.updateCachedVideoProgress(targetUrl, "DOWNLOADING", 99, null, null);
                        } finally {
                            if (tempVideoFile.exists()) tempVideoFile.delete();
                            if (tempAudioFile.exists()) tempAudioFile.delete();
                        }
                    } else {
                        // Fallback to progressive stream
                        String directUrl = null;
                        List<VideoStream> progressiveStreams = extractor.getVideoStreams();
                        if (progressiveStreams != null && !progressiveStreams.isEmpty()) {
                            VideoStream selectedProg = null;
                            int bestHeight = -1;
                            for (VideoStream stream : progressiveStreams) {
                                int height = getResolutionHeight(stream.getResolution());
                                if (height <= targetHeight) {
                                    if (height > bestHeight) {
                                        bestHeight = height;
                                        selectedProg = stream;
                                    }
                                }
                            }
                            if (selectedProg == null) {
                                for (VideoStream stream : progressiveStreams) {
                                    int height = getResolutionHeight(stream.getResolution());
                                    if (height > bestHeight) {
                                        bestHeight = height;
                                        selectedProg = stream;
                                    }
                                }
                            }
                            if (selectedProg == null) {
                                selectedProg = progressiveStreams.get(0);
                            }
                            directUrl = selectedProg.getContent();
                        } else {
                            String hlsUrl = extractor.getHlsUrl();
                            if (hlsUrl != null && !hlsUrl.isEmpty()) {
                                directUrl = hlsUrl;
                            }
                        }

                        if (directUrl == null) {
                            throw new IOException("No suitable video stream found.");
                        }

                        downloadFile(directUrl, videoFile, new ProgressListener() {
                            @Override
                            public void onProgress(int progress) {
                                dbHelper.updateCachedVideoProgress(targetUrl, "DOWNLOADING", progress, null, null);
                            }
                        });
                    }

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
        long downloadedBytes = 0;
        if (destination.exists()) {
            destination.delete();
        }

        long totalBytes = -1;
        int maxRetries = 15;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(fileUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Safari/537.36");

            if (downloadedBytes > 0) {
                requestBuilder.header("Range", "bytes=" + downloadedBytes + "-");
            }

            Request request = requestBuilder.build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() && response.code() != 206) {
                    throw new IOException("Unexpected response code " + response.code());
                }

                if (totalBytes == -1) {
                    if (response.code() == 206) {
                        String contentRange = response.header("Content-Range");
                        if (contentRange != null) {
                            int slash = contentRange.lastIndexOf("/");
                            if (slash != -1) {
                                try {
                                    totalBytes = Long.parseLong(contentRange.substring(slash + 1));
                                } catch (Exception e) {}
                            }
                        }
                    }
                    if (totalBytes == -1) {
                        totalBytes = response.body().contentLength();
                    }
                }

                try (InputStream is = response.body().byteStream();
                     java.io.RandomAccessFile raf = new java.io.RandomAccessFile(destination, "rw")) {

                    raf.seek(downloadedBytes);
                    byte[] buffer = new byte[65536];
                    int read;
                    long lastProgressTime = 0;

                    while ((read = is.read(buffer)) != -1) {
                        raf.write(buffer, 0, read);
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
                    break; // download finished successfully
                }
            } catch (IOException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw e;
                }
                Log.w(TAG, "Connection reset or interrupted. Retrying download from byte " + downloadedBytes + " (attempt " + retryCount + "/" + maxRetries + ")", e);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {}
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

    private void mux(String videoPath, String audioPath, String outputPath) throws IOException {
        android.media.MediaExtractor videoExtractor = new android.media.MediaExtractor();
        videoExtractor.setDataSource(videoPath);

        android.media.MediaExtractor audioExtractor = new android.media.MediaExtractor();
        audioExtractor.setDataSource(audioPath);

        android.media.MediaMuxer muxer = new android.media.MediaMuxer(outputPath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        // Setup video track
        videoExtractor.selectTrack(0);
        android.media.MediaFormat videoFormat = videoExtractor.getTrackFormat(0);
        int videoTrackIndex = muxer.addTrack(videoFormat);

        // Setup audio track
        audioExtractor.selectTrack(0);
        android.media.MediaFormat audioFormat = audioExtractor.getTrackFormat(0);
        int audioTrackIndex = muxer.addTrack(audioFormat);

        muxer.start();

        // Write video data
        java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocate(1024 * 1024);
        android.media.MediaCodec.BufferInfo bufferInfo = new android.media.MediaCodec.BufferInfo();

        videoExtractor.selectTrack(0);
        while (true) {
            bufferInfo.offset = 0;
            bufferInfo.size = videoExtractor.readSampleData(byteBuffer, 0);
            if (bufferInfo.size < 0) {
                break;
            }
            bufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
            bufferInfo.flags = videoExtractor.getSampleFlags();
            muxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo);
            videoExtractor.advance();
        }

        // Write audio data
        audioExtractor.selectTrack(0);
        while (true) {
            bufferInfo.offset = 0;
            bufferInfo.size = audioExtractor.readSampleData(byteBuffer, 0);
            if (bufferInfo.size < 0) {
                break;
            }
            bufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
            bufferInfo.flags = audioExtractor.getSampleFlags();
            muxer.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo);
            audioExtractor.advance();
        }

        muxer.stop();
        muxer.release();
        videoExtractor.release();
        audioExtractor.release();
    }
}
