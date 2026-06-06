package org.schabi.newpipe.localserver;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalHttpServer {

    public interface LogListener {
        void onLog(String message);
    }

    private static LogListener logListener;
    private final int port;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static final okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private final android.content.Context context;
    private final HistoryDbHelper dbHelper;

    public LocalHttpServer(android.content.Context context, int port) {
        this.context = context;
        this.port = port;
        this.dbHelper = HistoryDbHelper.getInstance(context);
    }

    public static void setLogListener(LogListener listener) {
        logListener = listener;
    }

    public static void log(String message) {
        if (logListener != null) {
            logListener.onLog(message);
        }
    }

    public void startServer() throws IOException {
        serverSocket = new ServerSocket(port);
        isRunning = true;
        log("Local server started on port " + port);

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        Socket socket = serverSocket.accept();
                        threadPool.execute(new ClientHandler(socket, dbHelper, context, threadPool));
                    } catch (IOException e) {
                        if (!isRunning) break;
                        log("Socket accept error: " + e.getMessage());
                    }
                }
            }
        });
    }

    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // ignore
        }
        threadPool.shutdownNow();
        log("Local server stopped.");
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final HistoryDbHelper dbHelper;
        private final android.content.Context context;
        private final ExecutorService executorService;

        public ClientHandler(Socket socket, HistoryDbHelper dbHelper, android.content.Context context, ExecutorService executorService) {
            this.socket = socket;
            this.dbHelper = dbHelper;
            this.context = context;
            this.executorService = executorService;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                 OutputStream os = socket.getOutputStream()) {

                String requestLine = reader.readLine();
                if (requestLine == null) return;

                String[] parts = requestLine.split(" ");
                if (parts.length < 2) return;

                String method = parts[0];
                String rawUri = parts[1];

                // Parse path and query parameters
                String path = rawUri;
                String query = null;
                int qIdx = rawUri.indexOf("?");
                if (qIdx >= 0) {
                    path = rawUri.substring(0, qIdx);
                    query = rawUri.substring(qIdx + 1);
                }

                Map<String, String> params = parseQueryParams(query);
                log("Request: " + method + " " + path + (query != null ? "?" + query : ""));

                // Parse headers
                Map<String, String> requestHeaders = new HashMap<>();
                String headerLine;
                while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                    int colonIdx = headerLine.indexOf(":");
                    if (colonIdx > 0) {
                        String name = headerLine.substring(0, colonIdx).trim().toLowerCase(java.util.Locale.US);
                        String value = headerLine.substring(colonIdx + 1).trim();
                        requestHeaders.put(name, value);
                    }
                }

                // Detect device class (TV vs Phone)
                String ua = requestHeaders.get("user-agent");
                boolean isTv = false;
                if (ua != null) {
                    String uaLower = ua.toLowerCase(java.util.Locale.US);
                    isTv = uaLower.contains("tv") || uaLower.contains("googletv") || uaLower.contains("androidtv") || uaLower.contains("smarttv") || uaLower.contains("appletv") || uaLower.contains("roku") || uaLower.contains("aftb") || uaLower.contains("aftt") || uaLower.contains("firetv");
                }

                try {
                    if (path.equals("/")) {
                        handleHome(os, params, isTv);
                    } else if (path.equals("/search")) {
                        handleSearch(os, params, isTv);
                    } else if (path.equals("/watch")) {
                        handleWatch(os, params, isTv);
                    } else if (path.equals("/history")) {
                        handleHistory(os, params, isTv);
                    } else if (path.equals("/channel")) {
                        handleChannel(os, params, isTv);
                    } else if (path.equals("/playlist")) {
                        handlePlaylist(os, params, isTv);
                    } else if (path.equals("/stream")) {
                        handleStreamProxy(os, params, requestHeaders);
                    } else if (path.equals("/cache")) {
                        handleCache(os, params, isTv);
                    } else if (path.equals("/subscriptions")) {
                        handleSubscriptions(os, params, isTv);
                    } else if (path.equals("/subscribe")) {
                        handleSubscribeAction(os, params);
                    } else if (path.equals("/bookmark_playlist")) {
                        handlePlaylistBookmarkAction(os, params);
                    } else if (path.equals("/thumbnail")) {
                        handleThumbnail(os, params);
                    } else {
                        sendResponse(os, 404, "Page Not Found", "text/plain; charset=UTF-8");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log("Error during route handling: " + e.getMessage());
                    sendResponse(os, 500, "Internal Server Error:\n" + e.toString(), "text/plain; charset=UTF-8");
                }

            } catch (Exception e) {
                // Connection error
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        private void handleHome(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            int serviceId = getServiceId(params);
            String nextPageStr = params.get("nextPage");
            Page nextPage = HtmlRenderer.deserializePage(nextPageStr);

            try {
                StreamingService service = NewPipe.getService(serviceId);
                List<InfoItem> items;
                Page next;

                if (nextPage != null) {
                    SearchExtractor extractor = service.getSearchExtractor("trending");
                    InfoItemsPage<InfoItem> page = extractor.getPage(nextPage);
                    items = page.getItems();
                    next = page.getNextPage();
                } else {
                    List<String> preferred = dbHelper.getPreferredKeywords();
                    SearchExtractor extractor;
                    if (!preferred.isEmpty()) {
                        String query = generateHomeQuery(preferred);
                        log("Generating personalized home feed for query: " + query);
                        extractor = service.getSearchExtractor(query);
                    } else {
                        extractor = service.getSearchExtractor("trending");
                    }
                    extractor.fetchPage();
                    items = extractor.getInitialPage().getItems();
                    next = extractor.getInitialPage().getNextPage();

                    List<InfoItem> subscriptions = dbHelper.getSubscriptions();
                    if (subscriptions != null && !subscriptions.isEmpty()) {
                        List<InfoItem> selectedChannels = new ArrayList<>(subscriptions);
                        java.util.Collections.shuffle(selectedChannels);
                        int limit = Math.min(3, selectedChannels.size());
                        List<java.util.concurrent.Future<List<InfoItem>>> futures = new ArrayList<>();
                        for (int i = 0; i < limit; i++) {
                            final String url = selectedChannels.get(i).getUrl();
                            futures.add(executorService.submit(new java.util.concurrent.Callable<List<InfoItem>>() {
                                @Override
                                public List<InfoItem> call() throws Exception {
                                    return fetchChannelUploads(service, url);
                                }
                            }));
                        }
                        List<InfoItem> channelItems = new ArrayList<>();
                        for (java.util.concurrent.Future<List<InfoItem>> future : futures) {
                            try {
                                List<InfoItem> res = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
                                if (res != null) {
                                    channelItems.addAll(res);
                                }
                            } catch (Exception e) {
                                log("Future timeout/error fetching channel uploads: " + e.getMessage());
                            }
                        }
                        if (!channelItems.isEmpty()) {
                            java.util.Collections.shuffle(channelItems);
                            List<InfoItem> mergedItems = new ArrayList<>();
                            int channelIdx = 0;
                            int feedIdx = 0;
                            while (channelIdx < channelItems.size() || feedIdx < items.size()) {
                                for (int k = 0; k < 2 && channelIdx < channelItems.size(); k++) {
                                    mergedItems.add(channelItems.get(channelIdx++));
                                }
                                if (feedIdx < items.size()) {
                                    mergedItems.add(items.get(feedIdx++));
                                }
                            }
                            items = mergedItems;
                        }
                    }
                }

                List<InfoItem> filtered = filterItems(items);
                String html = HtmlRenderer.renderHome(serviceId, filtered, next, isTv);
                sendResponse(os, 200, html, "text/html; charset=UTF-8");
            } catch (Exception e) {
                List<CachedVideo> cachedVideos = dbHelper.getCachedVideos();
                String html = HtmlRenderer.renderOfflineHome(serviceId, "Offline - Showing cached content (" + e.getMessage() + ")", cachedVideos, isTv);
                sendResponse(os, 200, html, "text/html; charset=UTF-8");
            }
        }

        private List<InfoItem> fetchChannelUploads(StreamingService service, String channelUrl) {
            try {
                ChannelExtractor channelExtractor = service.getChannelExtractor(channelUrl);
                channelExtractor.fetchPage();
                ChannelTabExtractor tabExtractor = service.getChannelTabExtractorFromIdAndBaseUrl(channelExtractor.getId(), "videos", channelExtractor.getBaseUrl());
                tabExtractor.fetchPage();
                List<InfoItem> list = new ArrayList<>();
                if (tabExtractor.getInitialPage() != null && tabExtractor.getInitialPage().getItems() != null) {
                    for (Object item : tabExtractor.getInitialPage().getItems()) {
                        if (item instanceof InfoItem) {
                            list.add((InfoItem) item);
                        }
                    }
                }
                return list;
            } catch (Exception e) {
                log("Failed to fetch uploads for channel " + channelUrl + ": " + e.getMessage());
                return new ArrayList<>();
            }
        }

        private String generateHomeQuery(List<String> preferred) {
            if (preferred == null || preferred.isEmpty()) return "trending";
            java.util.Random rand = new java.util.Random();
            if (preferred.size() >= 2 && rand.nextDouble() < 0.3) {
                int idx1 = rand.nextInt(preferred.size());
                int idx2 = rand.nextInt(preferred.size());
                while (idx1 == idx2) {
                    idx2 = rand.nextInt(preferred.size());
                }
                return preferred.get(idx1) + " " + preferred.get(idx2);
            } else {
                return preferred.get(rand.nextInt(preferred.size()));
            }
        }

        private void handleSearch(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            int serviceId = getServiceId(params);
            String query = params.get("q");
            if (query == null || query.isEmpty()) {
                sendRedirect(os, "/?serviceId=" + serviceId);
                return;
            }

            String nextPageStr = params.get("nextPage");
            Page nextPage = HtmlRenderer.deserializePage(nextPageStr);

            try {
                StreamingService service = NewPipe.getService(serviceId);
                SearchExtractor extractor = service.getSearchExtractor(query);
                
                List<InfoItem> items;
                Page next;

                if (nextPage != null) {
                    InfoItemsPage<InfoItem> page = extractor.getPage(nextPage);
                    items = page.getItems();
                    next = page.getNextPage();
                } else {
                    extractor.fetchPage();
                    items = extractor.getInitialPage().getItems();
                    next = extractor.getInitialPage().getNextPage();
                }

                List<InfoItem> filtered = filterItems(items);
                String html = HtmlRenderer.renderSearch(serviceId, query, filtered, next, isTv);
                sendResponse(os, 200, html, "text/html; charset=UTF-8");
            } catch (Exception e) {
                List<CachedVideo> cachedVideos = dbHelper.getCachedVideos();
                String html = HtmlRenderer.renderOfflineHome(serviceId, "Offline - Showing cached content", cachedVideos, isTv);
                sendResponse(os, 200, html, "text/html; charset=UTF-8");
            }
        }

        private void handleWatch(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            int serviceId = getServiceId(params);
            String mediaUrl = params.get("id");

            CachedVideo cachedVideo = dbHelper.getCachedVideo(mediaUrl);
            if (cachedVideo != null && "COMPLETED".equals(cachedVideo.getStatus())) {
                List<CachedVideo> otherCached = dbHelper.getCachedVideos();
                String html = HtmlRenderer.renderCachedWatch(serviceId, cachedVideo, otherCached, isTv);
                sendResponse(os, 200, html, "text/html; charset=UTF-8");
                return;
            }

            try {
                StreamingService service = NewPipe.getService(serviceId);
                StreamInfo info = StreamInfo.getInfo(service, mediaUrl);

                String thumbUrl = "";
                if (info.getThumbnails() != null && !info.getThumbnails().isEmpty()) {
                    thumbUrl = info.getThumbnails().get(info.getThumbnails().size() - 1).getUrl();
                }
                dbHelper.saveToHistory(info.getName(), info.getUrl(), info.getUploaderName(), thumbUrl);

                boolean isSubscribed = dbHelper.isSubscribed(info.getUploaderUrl());
                String html = HtmlRenderer.renderWatch(serviceId, info, cachedVideo, isSubscribed, isTv);
                sendResponse(os, 200, html, "text/html; charset=UTF-8");
            } catch (Exception e) {
                if (cachedVideo != null) {
                    List<CachedVideo> otherCached = dbHelper.getCachedVideos();
                    String html = HtmlRenderer.renderCachedWatch(serviceId, cachedVideo, otherCached, isTv);
                    sendResponse(os, 200, html, "text/html; charset=UTF-8");
                } else {
                    List<CachedVideo> cachedVideos = dbHelper.getCachedVideos();
                    String html = HtmlRenderer.renderOfflineHome(serviceId, "Offline - " + e.getMessage(), cachedVideos, isTv);
                    sendResponse(os, 200, html, "text/html; charset=UTF-8");
                }
            }
        }

        private void handleHistory(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            int serviceId = getServiceId(params);
            List<InfoItem> items = dbHelper.getHistory();
            String html = HtmlRenderer.renderHistory(serviceId, items, isTv);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handleStreamProxy(OutputStream os, Map<String, String> params, Map<String, String> requestHeaders) throws Exception {
            int serviceId = getServiceId(params);
            String mediaUrl = params.get("id");

            CachedVideo cachedVideo = dbHelper.getCachedVideo(mediaUrl);
            if (cachedVideo != null && "COMPLETED".equals(cachedVideo.getStatus())) {
                java.io.File file = new java.io.File(cachedVideo.getVideoLocalPath());
                if (file.exists()) {
                    log("Serving local cached video for: " + mediaUrl);
                    serveLocalFile(os, file, requestHeaders, "video/mp4");
                    return;
                }
            }

            StreamingService service = NewPipe.getService(serviceId);
            StreamExtractor extractor = service.getStreamExtractor(mediaUrl);
            extractor.fetchPage();

            String directUrl = null;
            
            List<VideoStream> progressiveStreams = extractor.getVideoStreams();
            if (progressiveStreams != null && !progressiveStreams.isEmpty()) {
                directUrl = progressiveStreams.get(0).getContent();
            } else {
                try {
                    String hlsUrl = extractor.getHlsUrl();
                    if (hlsUrl != null && !hlsUrl.isEmpty()) {
                        directUrl = hlsUrl;
                    }
                } catch (Exception e) {
                    // ignore
                }
                if (directUrl == null) {
                    List<AudioStream> audioStreams = extractor.getAudioStreams();
                    if (audioStreams != null && !audioStreams.isEmpty()) {
                        directUrl = audioStreams.get(0).getContent();
                    }
                }
            }

            if (directUrl != null) {
                log("Proxying stream from: " + directUrl);
                
                // Build remote request
                okhttp3.Request.Builder reqBuilder = new okhttp3.Request.Builder()
                        .url(directUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

                // Forward Range header if client sent it
                String rangeHeader = requestHeaders.get("range");
                if (rangeHeader != null) {
                    reqBuilder.header("Range", rangeHeader);
                }

                try (okhttp3.Response response = httpClient.newCall(reqBuilder.build()).execute()) {
                    int code = response.code();
                    
                    // Convert headers to browser response
                    String contentType = response.header("Content-Type", "video/mp4");
                    String contentRange = response.header("Content-Range");
                    String contentLength = response.header("Content-Length");
                    String acceptRanges = response.header("Accept-Ranges");

                    // Write HTTP status line
                    String status = code == 206 ? "Partial Content" : (code == 200 ? "OK" : "OK");
                    StringBuilder headBuilder = new StringBuilder();
                    headBuilder.append("HTTP/1.1 ").append(code).append(" ").append(status).append("\r\n");
                    headBuilder.append("Content-Type: ").append(contentType).append("\r\n");
                    if (contentLength != null) {
                        headBuilder.append("Content-Length: ").append(contentLength).append("\r\n");
                    }
                    if (contentRange != null) {
                        headBuilder.append("Content-Range: ").append(contentRange).append("\r\n");
                    }
                    if (acceptRanges != null) {
                        headBuilder.append("Accept-Ranges: ").append(acceptRanges).append("\r\n");
                    } else {
                        headBuilder.append("Accept-Ranges: bytes\r\n");
                    }
                    headBuilder.append("Connection: close\r\n\r\n");

                    os.write(headBuilder.toString().getBytes("UTF-8"));
                    os.flush();

                    // Pipe body bytes
                    if (response.body() != null) {
                        try (java.io.InputStream is = response.body().byteStream()) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                os.write(buffer, 0, read);
                            }
                        } catch (java.io.IOException e) {
                            // Client disconnected (e.g. paused/sought)
                            log("Stream proxy: Client connection closed.");
                        }
                    }
                    os.flush();
                }
            } else {
                sendResponse(os, 404, "Stream URL not found.", "text/plain; charset=UTF-8");
            }
        }

        private void handleChannel(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            int serviceId = getServiceId(params);
            String channelUrl = params.get("id");
            String tab = params.getOrDefault("tab", "videos");

            String nextPageStr = params.get("nextPage");
            Page nextPage = HtmlRenderer.deserializePage(nextPageStr);

            StreamingService service = NewPipe.getService(serviceId);
            ChannelExtractor channelExtractor = service.getChannelExtractor(channelUrl);
            channelExtractor.fetchPage();

            List<InfoItem> items;
            Page next;
            if ("playlists".equals(tab)) {
                ChannelTabExtractor tabExtractor = service.getChannelTabExtractorFromIdAndBaseUrl(channelExtractor.getId(), "playlists", channelExtractor.getBaseUrl());
                if (nextPage != null) {
                    InfoItemsPage<? extends InfoItem> page = tabExtractor.getPage(nextPage);
                    items = (List<InfoItem>) page.getItems();
                    next = page.getNextPage();
                } else {
                    tabExtractor.fetchPage();
                    InfoItemsPage<? extends InfoItem> page = tabExtractor.getInitialPage();
                    items = (List<InfoItem>) page.getItems();
                    next = page.getNextPage();
                }
            } else {
                ChannelTabExtractor tabExtractor = service.getChannelTabExtractorFromIdAndBaseUrl(channelExtractor.getId(), "videos", channelExtractor.getBaseUrl());
                if (nextPage != null) {
                    InfoItemsPage<? extends InfoItem> page = tabExtractor.getPage(nextPage);
                    items = (List<InfoItem>) page.getItems();
                    next = page.getNextPage();
                } else {
                    tabExtractor.fetchPage();
                    InfoItemsPage<? extends InfoItem> page = tabExtractor.getInitialPage();
                    items = (List<InfoItem>) page.getItems();
                    next = page.getNextPage();
                }
            }

            List<InfoItem> filtered = filterItems(items);
            boolean isSubscribed = dbHelper.isSubscribed(channelExtractor.getLinkHandler().getUrl());
            String html = HtmlRenderer.renderChannel(serviceId, channelExtractor, tab, filtered, next, isSubscribed, isTv);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handlePlaylist(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            int serviceId = getServiceId(params);
            String playlistUrl = params.get("id");

            String nextPageStr = params.get("nextPage");
            Page nextPage = HtmlRenderer.deserializePage(nextPageStr);

            StreamingService service = NewPipe.getService(serviceId);
            PlaylistExtractor extractor = service.getPlaylistExtractor(playlistUrl);
            
            List<InfoItem> items;
            Page next;
            if (nextPage != null) {
                InfoItemsPage<? extends InfoItem> page = extractor.getPage(nextPage);
                items = (List<InfoItem>) page.getItems();
                next = page.getNextPage();
            } else {
                extractor.fetchPage();
                InfoItemsPage<? extends InfoItem> page = extractor.getInitialPage();
                items = (List<InfoItem>) page.getItems();
                next = page.getNextPage();
            }

            List<InfoItem> filtered = filterItems(items);
            boolean isBookmarked = dbHelper.isPlaylistBookmarked(playlistUrl);
            String html = HtmlRenderer.renderPlaylist(serviceId, extractor, filtered, next, isBookmarked, isTv);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private int getServiceId(Map<String, String> params) {
            return 0; // Force YouTube (0)
        }

        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            if (query == null || query.isEmpty()) return params;
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                try {
                    String key = URLDecoder.decode(idx > 0 ? pair.substring(0, idx) : pair, "UTF-8");
                    String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : "";
                    params.put(key, value);
                } catch (Exception e) {
                    // ignore
                }
            }
            return params;
        }

        private void sendResponse(OutputStream os, int code, String content, String contentType) throws IOException {
            byte[] bytes = content.getBytes("UTF-8");
            String status = code == 200 ? "OK" : (code == 404 ? "Not Found" : "Internal Server Error");
            String response = "HTTP/1.1 " + code + " " + status + "\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "Content-Length: " + bytes.length + "\r\n" +
                    "Connection: close\r\n\r\n";
            os.write(response.getBytes("UTF-8"));
            os.write(bytes);
            os.flush();
        }

        private void sendRedirect(OutputStream os, String url) throws IOException {
            String response = "HTTP/1.1 302 Found\r\n" +
                    "Location: " + url + "\r\n" +
                    "Content-Length: 0\r\n" +
                    "Connection: close\r\n\r\n";
            os.write(response.getBytes("UTF-8"));
            os.flush();
        }

        private void handleCache(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            int serviceId = getServiceId(params);
            String action = params.get("action");
            String mediaUrl = params.get("id");

            if ("add".equals(action) && mediaUrl != null && !mediaUrl.isEmpty()) {
                VideoCacheManager.getInstance(context).startCaching(mediaUrl, serviceId);
                sendRedirect(os, "/watch?serviceId=" + serviceId + "&id=" + java.net.URLEncoder.encode(mediaUrl, "UTF-8"));
                return;
            } else if ("delete".equals(action) && mediaUrl != null && !mediaUrl.isEmpty()) {
                VideoCacheManager.getInstance(context).deleteCache(mediaUrl);
                sendRedirect(os, "/cache?serviceId=" + serviceId);
                return;
            }

            List<CachedVideo> cachedVideos = dbHelper.getCachedVideos();
            String html = HtmlRenderer.renderCachedList(serviceId, cachedVideos, isTv);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handleSubscriptions(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            int serviceId = getServiceId(params);
            List<InfoItem> channels = dbHelper.getSubscriptions();
            List<InfoItem> playlists = dbHelper.getBookmarkedPlaylists();
            String activeTab = params.getOrDefault("tab", "channels");
            String html = HtmlRenderer.renderSubscriptions(serviceId, channels, playlists, activeTab, isTv);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handleSubscribeAction(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            String action = params.get("action");
            String channelUrl = params.get("id");
            String back = params.get("back");

            if ("subscribe".equals(action) && channelUrl != null && !channelUrl.isEmpty()) {
                String name = params.get("name");
                String avatar = params.get("avatar");
                dbHelper.addSubscription(channelUrl, name, avatar);
            } else if ("unsubscribe".equals(action) && channelUrl != null && !channelUrl.isEmpty()) {
                dbHelper.removeSubscription(channelUrl);
            }

            if (back != null && !back.isEmpty()) {
                if (back.startsWith("/")) {
                    sendRedirect(os, back);
                } else {
                    sendRedirect(os, "/watch?serviceId=" + serviceId + "&id=" + java.net.URLEncoder.encode(back, "UTF-8"));
                }
            } else {
                sendRedirect(os, "/subscriptions?serviceId=" + serviceId);
            }
        }

        private void handlePlaylistBookmarkAction(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            String action = params.get("action");
            String playlistUrl = params.get("id");
            String back = params.get("back");

            if ("bookmark".equals(action) && playlistUrl != null && !playlistUrl.isEmpty()) {
                String name = params.get("name");
                String uploader = params.get("uploader");
                dbHelper.addPlaylistBookmark(playlistUrl, name, uploader);
            } else if ("unbookmark".equals(action) && playlistUrl != null && !playlistUrl.isEmpty()) {
                dbHelper.removePlaylistBookmark(playlistUrl);
            }

            if (back != null && !back.isEmpty()) {
                sendRedirect(os, back);
            } else {
                sendRedirect(os, "/subscriptions?serviceId=" + serviceId + "&tab=playlists");
            }
        }

        private void handleThumbnail(OutputStream os, Map<String, String> params) throws Exception {
            String mediaUrl = params.get("id");
            CachedVideo cachedVideo = dbHelper.getCachedVideo(mediaUrl);
            if (cachedVideo != null && cachedVideo.getThumbnailLocalPath() != null) {
                java.io.File thumbFile = new java.io.File(cachedVideo.getThumbnailLocalPath());
                if (thumbFile.exists()) {
                    serveLocalFile(os, thumbFile, new HashMap<>(), "image/jpeg");
                    return;
                }
            }
            sendResponse(os, 404, "Thumbnail not found", "text/plain");
        }

        private void serveLocalFile(OutputStream os, java.io.File file, Map<String, String> requestHeaders, String contentType) throws IOException {
            long fileSize = file.length();
            long start = 0;
            long end = fileSize - 1;
            boolean isRange = false;

            String rangeHeader = requestHeaders.get("range");
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String rangeValue = rangeHeader.substring(6);
                int minusIdx = rangeValue.indexOf('-');
                if (minusIdx >= 0) {
                    try {
                        String startStr = rangeValue.substring(0, minusIdx).trim();
                        if (!startStr.isEmpty()) {
                            start = Long.parseLong(startStr);
                        }
                        String endStr = rangeValue.substring(minusIdx + 1).trim();
                        if (!endStr.isEmpty()) {
                            end = Long.parseLong(endStr);
                        }
                        isRange = true;
                    } catch (NumberFormatException e) {
                        // Keep default full range
                    }
                }
            }

            if (start > end || start < 0 || end >= fileSize) {
                isRange = false;
                start = 0;
                end = fileSize - 1;
            }

            long contentLength = end - start + 1;
            int responseCode = isRange ? 206 : 200;
            String status = isRange ? "Partial Content" : "OK";

            StringBuilder headBuilder = new StringBuilder();
            headBuilder.append("HTTP/1.1 ").append(responseCode).append(" ").append(status).append("\r\n");
            headBuilder.append("Content-Type: ").append(contentType).append("\r\n");
            headBuilder.append("Content-Length: ").append(contentLength).append("\r\n");
            headBuilder.append("Accept-Ranges: bytes\r\n");
            if (isRange) {
                headBuilder.append("Content-Range: bytes ").append(start).append("-").append(end).append("/").append(fileSize).append("\r\n");
            }
            headBuilder.append("Connection: close\r\n\r\n");

            os.write(headBuilder.toString().getBytes("UTF-8"));
            os.flush();

            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
                raf.seek(start);
                byte[] buffer = new byte[8192];
                long bytesRemaining = contentLength;
                while (bytesRemaining > 0) {
                    int readSize = (int) Math.min(buffer.length, bytesRemaining);
                    int read = raf.read(buffer, 0, readSize);
                    if (read == -1) break;
                    os.write(buffer, 0, read);
                    bytesRemaining -= read;
                }
            } catch (IOException e) {
                // Client connection closed
            }
            os.flush();
        }


        private List<InfoItem> filterItems(List<InfoItem> items) {
            if (items == null) return null;
            boolean hideWatched = dbHelper.getHideWatched();
            boolean hideShorts = dbHelper.getHideShorts();
            List<String> blockedKeywords = dbHelper.getBlockedKeywords();
            List<String> blockedChannels = dbHelper.getBlockedChannels();

            List<InfoItem> filtered = new java.util.ArrayList<>();
            java.util.Set<String> watchedUrls = new java.util.HashSet<>();
            if (hideWatched) {
                for (InfoItem hist : dbHelper.getHistory()) {
                    watchedUrls.add(hist.getUrl());
                }
            }

            for (InfoItem item : items) {
                if (hideWatched && watchedUrls.contains(item.getUrl())) {
                    continue;
                }

                if (hideShorts && item instanceof StreamInfoItem) {
                    StreamInfoItem stream = (StreamInfoItem) item;
                    if (stream.getDuration() > 0 && stream.getDuration() <= 120) {
                        continue;
                    }
                }

                boolean hasBlockedKeyword = false;
                String titleLower = item.getName().toLowerCase(java.util.Locale.US);
                for (String keyword : blockedKeywords) {
                    if (!keyword.isEmpty() && titleLower.contains(keyword.toLowerCase(java.util.Locale.US))) {
                        hasBlockedKeyword = true;
                        break;
                    }
                }
                if (hasBlockedKeyword) continue;

                boolean hasBlockedChannel = false;
                if (item instanceof StreamInfoItem) {
                    StreamInfoItem stream = (StreamInfoItem) item;
                    String uploaderLower = stream.getUploaderName() != null ? stream.getUploaderName().toLowerCase(java.util.Locale.US) : "";
                    for (String channel : blockedChannels) {
                        if (!channel.isEmpty() && uploaderLower.contains(channel.toLowerCase(java.util.Locale.US))) {
                            hasBlockedChannel = true;
                            break;
                        }
                    }
                }
                if (hasBlockedChannel) continue;

                filtered.add(item);
            }
            return filtered;
        }
    }
}
