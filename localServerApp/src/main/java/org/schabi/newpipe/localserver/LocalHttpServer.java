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
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.MediaFormat;

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

    public interface LockStatusListener {
        void onLockStatusChanged();
    }

    private static String activeLockCode = null;
    private static String activeClientIp = null;
    private static String activeVideoTitle = null;
    private static LockStatusListener lockStatusListener;
    private static RemoteWebSocketServer wsServer = null;
    private static final java.util.Queue<String> pendingCommands = new java.util.concurrent.LinkedBlockingQueue<>();

    public static class ClientInfo {
        public final String name;
        public final org.java_websocket.WebSocket connection;
        public ClientInfo(String name, org.java_websocket.WebSocket connection) {
            this.name = name;
            this.connection = connection;
        }
    }

    public static List<ClientInfo> getConnectedClients() {
        List<ClientInfo> list = new ArrayList<>();
        if (wsServer != null) {
            for (org.java_websocket.WebSocket conn : wsServer.getConnections()) {
                if (conn.isOpen()) {
                    String name = conn.getAttachment();
                    if (name == null || name.isEmpty()) {
                        try {
                            name = "Client (" + conn.getRemoteSocketAddress().getAddress().getHostAddress() + ")";
                        } catch (Exception e) {
                            name = "Client (Unknown)";
                        }
                    }
                    list.add(new ClientInfo(name, conn));
                }
            }
        }
        return list;
    }

    public static void castToClient(org.java_websocket.WebSocket conn, String videoUrl) {
        if (conn != null && conn.isOpen()) {
            try {
                conn.send("play_video:" + videoUrl);
                log("Casted play_video command to client connection.");
            } catch (Exception e) {
                log("Failed to send command to specific client: " + e.getMessage());
            }
        }
    }


    public static List<String> getAndClearPendingCommands() {
        List<String> copy = new ArrayList<>();
        String cmd;
        while ((cmd = pendingCommands.poll()) != null) {
            copy.add(cmd);
        }
        return copy;
    }

    public static void addPendingCommand(String cmd) {
        if (pendingCommands.size() > 500) {
            pendingCommands.poll(); // Prevent infinite growth if client disconnects
        }
        pendingCommands.offer(cmd);
        
        if (wsServer != null) {
            wsServer.broadcastCommand(cmd);
        }
    }

    public static void setLockStatusListener(LockStatusListener listener) {
        lockStatusListener = listener;
    }

    public static boolean isLocked() {
        return activeLockCode != null;
    }

    public static String getActiveClientIp() {
        return activeClientIp;
    }

    public static String getActiveVideoTitle() {
        return activeVideoTitle;
    }

    public static String getActiveLockCode() {
        return activeLockCode;
    }

    public static void releaseLock() {
        activeLockCode = null;
        activeClientIp = null;
        activeVideoTitle = null;
        if (lockStatusListener != null) {
            lockStatusListener.onLockStatusChanged();
        }
    }

    public static boolean tryLock(String code, String clientIp, String title) {
        if (activeLockCode == null) {
            activeLockCode = code;
            activeClientIp = clientIp;
            activeVideoTitle = title;
            if (lockStatusListener != null) {
                lockStatusListener.onLockStatusChanged();
            }
            return true;
        } else if (activeLockCode.equals(code)) {
            activeVideoTitle = title;
            if (lockStatusListener != null) {
                lockStatusListener.onLockStatusChanged();
            }
            return true;
        }
        return false;
    }

    private static LogListener logListener;
    private final int port;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static final StreamUrlCache streamUrlCache = new StreamUrlCache();
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

        // Start WebSocket Server on port 8081
        if (wsServer == null) {
            wsServer = new RemoteWebSocketServer(8081);
            wsServer.start();
        }

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
        if (wsServer != null) {
            try {
                wsServer.stop();
            } catch (InterruptedException e) {
                // ignore
            }
            wsServer = null;
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

                if ("OPTIONS".equalsIgnoreCase(method)) {
                    String sb = "HTTP/1.1 204 No Content\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: *\r\n" +
                            "Access-Control-Expose-Headers: *\r\n" +
                            "Access-Control-Max-Age: 86400\r\n" +
                            "\r\n";
                    os.write(sb.getBytes("UTF-8"));
                    os.flush();
                    return;
                }

                // Read POST body if Content-Length is present
                String postBody = "";
                if ("POST".equalsIgnoreCase(method)) {
                    String contentLengthHeader = requestHeaders.get("content-length");
                    if (contentLengthHeader != null) {
                        try {
                            int contentLength = Integer.parseInt(contentLengthHeader);
                            char[] buffer = new char[contentLength];
                            int totalRead = 0;
                            while (totalRead < contentLength) {
                                int read = reader.read(buffer, totalRead, contentLength - totalRead);
                                if (read == -1) break;
                                totalRead += read;
                            }
                            postBody = new String(buffer, 0, totalRead);
                        } catch (Exception e) {
                            log("Error reading POST body: " + e.getMessage());
                        }
                    }
                }

                // Handle CORS preflight (OPTIONS)
                if ("OPTIONS".equalsIgnoreCase(method)) {
                    String corsResponse = "HTTP/1.1 200 OK\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: Content-Type, Range, Authorization\r\n" +
                            "Access-Control-Max-Age: 86400\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n\r\n";
                    os.write(corsResponse.getBytes("UTF-8"));
                    os.flush();
                    return;
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
                    } else if (path.equals("/db/export")) {
                        handleDbExport(os);
                    } else if (path.equals("/db/import")) {
                        handleDbImport(os, postBody);
                    } else if (path.equals("/search")) {
                        handleSearch(os, params, isTv);
                    } else if (path.equals("/watch")) {
                        handleWatch(os, params, isTv);
                    } else if (path.equals("/watch-content")) {
                        handleWatchContent(os, params, isTv);
                    } else if (path.equals("/send-link") || path.equals("/play")) {
                        handleSendLink(os, params, socket.getInetAddress().getHostAddress());
                    } else if (path.equals("/send-command")) {
                        handleSendCommand(os, params);
                    } else if (path.equals("/poll-commands")) {
                        handlePollCommands(os);
                    } else if (path.equals("/release-lock")) {
                        handleReleaseLock(os, params);
                    } else if (path.equals("/history")) {
                        handleHistory(os, params, isTv);
                    } else if (path.equals("/channel")) {
                        handleChannel(os, params, isTv);
                    } else if (path.equals("/playlist")) {
                        handlePlaylist(os, params, isTv);
                    } else if (path.equals("/stream")) {
                        handleStreamProxy(os, params, requestHeaders);
                    } else if (path.equals("/manifest")) {
                        handleManifestProxy(os, params);
                    } else if (path.equals("/subtitles")) {
                        handleSubtitlesProxy(os, params);
                    } else if (path.equals("/log_client_capabilities")) {
                        String supported = params.get("supported");
                        String error = params.get("error");
                        String playingQuality = params.get("playing_quality");
                        String userAgent = requestHeaders.get("user-agent");
                        if (error != null) {
                            log("Client player error: " + error + " | User-Agent: " + userAgent);
                        } else if (playingQuality != null) {
                            log("Client is playing quality: " + playingQuality + " | User-Agent: " + userAgent);
                        } else {
                            log("Client connection capability check: DASH supported = " + supported + " | User-Agent: " + userAgent);
                        }
                        sendResponse(os, 200, "OK", "text/plain; charset=UTF-8");
                    } else if (path.equals("/cache")) {
                        handleCache(os, params, isTv);
                    } else if (path.equals("/cache-status")) {
                        handleCacheStatus(os, params);
                    } else if (path.equals("/search-history")) {
                        handleSearchHistory(os, params);
                    } else if (path.equals("/subscriptions")) {
                        handleSubscriptions(os, params, isTv);
                    } else if (path.equals("/subscribe")) {
                        handleSubscribeAction(os, params);
                    } else if (path.equals("/bookmark_playlist")) {
                        handlePlaylistBookmarkAction(os, params);
                    } else if (path.equals("/download-cached")) {
                        handleDownloadCached(os, params);
                    } else if (path.equals("/thumbnail")) {
                        handleThumbnail(os, params);
                    } else if (path.equals("/subtitles")) {
                        handleSubtitlesProxy(os, params);
                    } else if (path.equals("/shorts")) {
                        handleShortsPage(os, params, isTv);
                    } else if (path.equals("/api/shorts/feed")) {
                        handleShortsApiFeed(os, params);
                    } else if (path.equals("/settings")) {
                        handleSettings(os, params, isTv);
                    } else if (path.equals("/watch-later")) {
                        handleWatchLater(os, params, isTv);
                    } else if (path.equals("/watch_later_action")) {
                        handleWatchLaterAction(os, params);
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

            if (!"ajax".equals(params.get("feed"))) {
                String html = HtmlRenderer.renderHomeSkeleton(serviceId, isTv);
                sendResponse(os, 200, html, "text/html; charset=UTF-8");
                return;
            }

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
                String html = HtmlRenderer.renderHomeFeed(serviceId, filtered, next);
                sendResponse(os, 200, html, "text/html; charset=UTF-8");
            } catch (Exception e) {
                List<CachedVideo> cachedVideos = dbHelper.getCachedVideos();
                List<InfoItem> offlineItems = new ArrayList<>();
                if (cachedVideos != null) {
                    for (CachedVideo cv : cachedVideos) {
                        if ("COMPLETED".equals(cv.getStatus())) {
                            StreamInfoItem sii = new StreamInfoItem(0, cv.getUrl(), cv.getTitle(), StreamType.VIDEO_STREAM);
                            sii.setUploaderName(cv.getUploader());
                            sii.setUploaderUrl("");
                            offlineItems.add(sii);
                        }
                    }
                }
                StringBuilder feedSb = new StringBuilder();
                feedSb.append("  <div style=\"background-color:#fce8e6; color:#c5221f; padding:16px; border-radius:12px; margin-bottom:24px; font-size:14px; font-weight:500; border: 1px solid #fad2cf;\">\n")
                      .append("    📶 You are currently offline (").append(e.getMessage()).append("). Showing your locally cached videos.\n")
                      .append("  </div>\n")
                      .append("  <h2 style=\"margin-bottom: 20px; font-weight: 700;\">📥 Offline Library</h2>\n");
                if (offlineItems.isEmpty()) {
                    feedSb.append("<div class=\"loading-placeholder\">No offline videos available. Connect to the internet to cache videos!</div>\n");
                } else {
                    HtmlRenderer.renderGrid(feedSb, serviceId, offlineItems);
                }
                sendResponse(os, 200, feedSb.toString(), "text/html; charset=UTF-8");
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
            dbHelper.addSearchQuery(query);

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

            // Immediately send the fast watch skeleton layout
            String html = HtmlRenderer.renderWatchSkeleton(serviceId, mediaUrl, isTv);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handleWatchContent(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            int serviceId = getServiceId(params);
            String mediaUrl = params.get("id");
            CachedVideo cachedVideo = dbHelper.getCachedVideo(mediaUrl);

            try {
                StreamingService service = NewPipe.getService(serviceId);
                StreamInfo info = StreamInfo.getInfo(service, mediaUrl);

                String thumbUrl = "";
                if (info.getThumbnails() != null && !info.getThumbnails().isEmpty()) {
                    thumbUrl = info.getThumbnails().get(info.getThumbnails().size() - 1).getUrl();
                }
                dbHelper.saveToHistory(info.getName(), info.getUrl(), info.getUploaderName(), thumbUrl);

                boolean isSubscribed = dbHelper.isSubscribed(info.getUploaderUrl());
                String targetQuality = dbHelper.getVideoQuality();
                
                long duration = info.getDuration();
                String html = HtmlRenderer.renderWatchContent(serviceId, info, cachedVideo, isSubscribed, isTv, targetQuality, duration);
                sendResponse(os, 200, html, "text/html; charset=UTF-8");
            } catch (Exception e) {
                if (cachedVideo != null) {
                    List<CachedVideo> otherCached = dbHelper.getCachedVideos();
                    String html = HtmlRenderer.renderCachedWatch(serviceId, cachedVideo, otherCached, isTv);
                    sendResponse(os, 200, html, "text/html; charset=UTF-8");
                } else {
                    sendResponse(os, 500, "Error: " + e.getMessage(), "text/plain; charset=UTF-8");
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
            String itagParam = params.get("itag");
            
            String rangeHeader = null;
            for (String key : requestHeaders.keySet()) {
                if ("range".equalsIgnoreCase(key)) {
                    rangeHeader = requestHeaders.get(key);
                    break;
                }
            }

            log("STREAM REQUEST itag=" + itagParam + " range=" + rangeHeader + " id=" + mediaUrl);

            CachedVideo cachedVideo = dbHelper.getCachedVideo(mediaUrl);
            if (cachedVideo != null && "COMPLETED".equals(cachedVideo.getStatus())) {
                java.io.File file = new java.io.File(cachedVideo.getVideoLocalPath());
                if (file.exists()) {
                    log("Serving local cached video for: " + mediaUrl);
                    serveLocalFile(os, file, requestHeaders, "video/mp4");
                    return;
                }
            }

            int requestedItag = -1;
            if (itagParam != null) {
                try {
                    requestedItag = Integer.parseInt(itagParam);
                } catch (Exception e) {}
            }

            String requestedTrackId = params.get("trackId");
            String cacheKey = serviceId + "_" + mediaUrl + "_" + requestedItag + (requestedTrackId != null ? "_" + requestedTrackId : "");
            String directUrl = streamUrlCache.get(cacheKey);

            if (directUrl == null) {
                StreamingService service = NewPipe.getService(serviceId);
                StreamExtractor extractor = service.getStreamExtractor(mediaUrl);
                extractor.fetchPage();

                if (requestedItag != -1) {
                    for (VideoStream stream : extractor.getVideoStreams()) {
                        if (stream.getItag() == requestedItag) {
                            directUrl = stream.getContent();
                            break;
                        }
                    }
                    if (directUrl == null) {
                        for (VideoStream stream : extractor.getVideoOnlyStreams()) {
                            if (stream.getItag() == requestedItag) {
                                directUrl = stream.getContent();
                                break;
                            }
                        }
                    }
                    if (directUrl == null) {
                        for (AudioStream stream : extractor.getAudioStreams()) {
                            if (stream.getItag() == requestedItag) {
                                String streamTrackId = stream.getAudioTrackId();
                                if (streamTrackId == null) {
                                    streamTrackId = "";
                                }
                                String reqTrackId = requestedTrackId;
                                if (reqTrackId == null) {
                                    reqTrackId = "";
                                }
                                if (java.util.Objects.equals(streamTrackId, reqTrackId)) {
                                    directUrl = stream.getContent();
                                    break;
                                }
                            }
                        }
                    }
                }

                if (directUrl == null) {
                    String qualityParam = params.get("quality");
                    String startTimeParam = params.get("start_time");
                    double startTime = 0.0;
                    if (startTimeParam != null) {
                        try {
                            startTime = Double.parseDouble(startTimeParam);
                        } catch (Exception e) {}
                    }
                    
                    String targetQuality = qualityParam != null ? qualityParam : dbHelper.getVideoQuality();
                    int targetHeight = getResolutionHeight(targetQuality);



                    // Fallback to progressive stream
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
                                // 1. Sort using NewPipe-like ranking to find the best track at index 0
                                java.util.Locale preferredLanguage = java.util.Locale.getDefault();
                                String langCode = preferredLanguage.getISO3Language();
                                java.util.Collections.sort(audioStreams, (a, b) -> {
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
                                // 2. Keep only streams of the best track
                                String bestTrackId = audioStreams.get(0).getAudioTrackId();
                                List<AudioStream> filteredStreams = audioStreams.stream()
                                        .filter(as -> java.util.Objects.equals(as.getAudioTrackId(), bestTrackId))
                                        .collect(java.util.stream.Collectors.toList());
                                if (!filteredStreams.isEmpty()) {
                                    audioStreams = filteredStreams;
                                }
                                directUrl = audioStreams.get(0).getContent();
                            }
                        }
                    }
                }

                if (directUrl != null) {
                    streamUrlCache.put(cacheKey, directUrl, 3600000);
                }
            }

            if (directUrl != null) {
                log("Proxying stream from: " + directUrl);
                
                // Use matching User-Agent for YouTube streams depending on the client (c) parameter to avoid 403 Forbidden
                String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
                if (directUrl.contains("googlevideo.com")) {
                    try {
                        if (directUrl.contains("c=IOS") || directUrl.contains("c=ios")) {
                            ua = org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getIosUserAgent(null);
                        } else if (directUrl.contains("c=VISIONOS") || directUrl.contains("c=visionos")) {
                            ua = org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getVisionOsUserAgent(null);
                        } else {
                            ua = org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getAndroidUserAgent(null);
                        }
                    } catch (Exception e) {}
                }

                // Build remote request
                okhttp3.Request.Builder reqBuilder = new okhttp3.Request.Builder()
                        .url(directUrl)
                        .header("User-Agent", ua);

                // Forward Range header if client sent it
                if (rangeHeader != null) {
                    reqBuilder.removeHeader("Range");
                    reqBuilder.addHeader("Range", rangeHeader);
                    log("Forwarding Range to CDN: " + rangeHeader);
                }

                try (okhttp3.Response response = httpClient.newCall(reqBuilder.build()).execute()) {
                    int code = response.code();
                    log("Incoming Range = " + rangeHeader + " CDN status=" + code + " itag=" + requestedItag);
                    
                    if (rangeHeader != null && code != 206) {
                        log("WARNING: Range requested (" + rangeHeader + ") but CDN returned " + code);
                    }

                    StringBuilder headBuilder = new StringBuilder();
                    String statusText = (code == 206) ? "Partial Content" : "OK";
                    headBuilder.append("HTTP/1.1 ").append(code).append(" ").append(statusText).append("\r\n");

                    String[] headersToForward = {
                            "Content-Type",
                            "Content-Length",
                            "Content-Range",
                            "Accept-Ranges"
                    };

                    for (String h : headersToForward) {
                        String val = response.header(h);
                        if (val != null) {
                            headBuilder.append(h).append(": ").append(val).append("\r\n");
                        }
                    }

                    // Ensure Content-Type is set if missing
                    if (response.header("Content-Type") == null) {
                        String defaultType = requestedItag == 140 ? "audio/mp4" : "video/mp4";
                        if (requestedItag == -1) defaultType = "application/octet-stream";
                        headBuilder.append("Content-Type: ").append(defaultType).append("\r\n");
                    }
                    
                    // Ensure Accept-Ranges is set for DASH
                    if (response.header("Accept-Ranges") == null) {
                        headBuilder.append("Accept-Ranges: bytes\r\n");
                    }

                    headBuilder.append("Access-Control-Allow-Origin: *\r\n");
                    headBuilder.append("Access-Control-Allow-Headers: *\r\n");
                    headBuilder.append("Access-Control-Expose-Headers: *\r\n");
                    headBuilder.append("\r\n");
                    
                    if (code == 206) log("Successfully returning 206 Partial Content to client");

                    os.write(headBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
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

        private void handleManifestProxy(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            String mediaUrl = params.get("id");

            StreamingService service = NewPipe.getService(serviceId);
            StreamExtractor extractor = service.getStreamExtractor(mediaUrl);
            extractor.fetchPage();

            // Construct standard DASH manifest (MPD) locally using extracted stream lists
            double durationSec = extractor.getLength();
            if (durationSec <= 0) {
                durationSec = 1800.0; // fallback 30 mins if length not available
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            sb.append("<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\" profiles=\"urn:mpeg:dash:profile:isoff-on-demand:2011\" type=\"static\" mediaPresentationDuration=\"PT").append(durationSec).append("S\" minBufferTime=\"PT1.5S\">\n");
            sb.append("  <Period duration=\"PT").append(durationSec).append("S\">\n");

            // Video AdaptationSet (Adaptive / Video-Only streams)
            List<VideoStream> videoStreams = extractor.getVideoOnlyStreams();
            if (videoStreams != null) {
                videoStreams = videoStreams.stream()
                        .filter(vs -> vs.getFormat() == org.schabi.newpipe.extractor.MediaFormat.MPEG_4)
                        .collect(java.util.stream.Collectors.toList());
            }

            if (videoStreams != null && !videoStreams.isEmpty()) {
                sb.append("    <AdaptationSet id=\"0\" mimeType=\"video/mp4\" subsegmentAlignment=\"true\" subsegmentStartsWithSAP=\"1\">\n");
                java.util.Set<Integer> seenVideoItags = new java.util.HashSet<>();
                for (VideoStream vs : videoStreams) {
                    int itag = vs.getItag();
                    if (seenVideoItags.contains(itag)) {
                        continue;
                    }
                    seenVideoItags.add(itag);
                    
                    long bitrate = vs.getBitrate();
                    if (bitrate <= 0) {
                        bitrate = 1000000;
                    }
                    // Extractors usually return bps. If it's suspiciously low, we might scale, 
                    // but the previous scaling (bitrate < 100000) was causing 360p (83kbps) 
                    // to be scaled to 83Mbps while 1080p (495kbps) was left at 0.5Mbps.
                    // Let's use a much lower threshold or just trust the extractor.
                    if (bitrate < 5000) { 
                        bitrate *= 1000;
                    }
                    String codec = vs.getCodec();
                    int width = vs.getWidth();
                    int height = vs.getHeight();
                    int fps = vs.getFps();

                    int initStart = vs.getInitStart();
                    int initEnd = vs.getInitEnd();
                    int indexStart = vs.getIndexStart();
                    int indexEnd = vs.getIndexEnd();

                    if (initStart < 0 || initEnd < 0 || indexStart < 0 || indexEnd < 0) {
                        continue; // Skip streams without correct index range markers
                    }

                    String proxyUrl = "/stream?serviceId=" + serviceId + "&amp;id=" + java.net.URLEncoder.encode(mediaUrl, "UTF-8") + "&amp;itag=" + itag;
                    sb.append("      <Representation id=\"").append(itag).append("\" bandwidth=\"").append(bitrate).append("\" codecs=\"").append(codec).append("\" width=\"").append(width).append("\" height=\"").append(height).append("\" frameRate=\"").append(fps).append("\" sar=\"1:1\">\n");
                    sb.append("        <BaseURL>").append(proxyUrl).append("</BaseURL>\n");
                    sb.append("        <SegmentBase indexRange=\"").append(indexStart).append("-").append(indexEnd).append("\" indexRangeExact=\"true\">\n");
                    sb.append("          <Initialization range=\"").append(initStart).append("-").append(initEnd).append("\"/>\n");
                    sb.append("        </SegmentBase>\n");
                    sb.append("      </Representation>\n");
                }
                sb.append("    </AdaptationSet>\n");
            }

            // Audio AdaptationSet
            List<AudioStream> audioStreams = extractor.getAudioStreams();
            if (audioStreams != null && !audioStreams.isEmpty()) {
                audioStreams = audioStreams.stream()
                        .filter(as -> as.getFormat() == org.schabi.newpipe.extractor.MediaFormat.M4A)
                        .collect(java.util.stream.Collectors.toList());
            }
            if (audioStreams != null && !audioStreams.isEmpty()) {
                String targetAudioTrack = params.get("audio_track");
                String selectedTrackId = "";
                
                // If a track is explicitly selected, find it
                if (targetAudioTrack != null) {
                    selectedTrackId = targetAudioTrack;
                } else {
                    // Default to best track using NewPipe comparator
                    java.util.Locale preferredLanguage = java.util.Locale.getDefault();
                    String langCode = preferredLanguage.getISO3Language();
                    java.util.Collections.sort(audioStreams, (a, b) -> {
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
                    
                    String bestId = audioStreams.get(0).getAudioTrackId();
                    selectedTrackId = bestId != null ? bestId : "";
                }
                
                // Filter to only keep streams matching the selectedTrackId
                final String finalTrackId = selectedTrackId;
                audioStreams = audioStreams.stream()
                        .filter(as -> java.util.Objects.equals(as.getAudioTrackId() == null ? "" : as.getAudioTrackId(), finalTrackId))
                        .collect(java.util.stream.Collectors.toList());
                
                if (!audioStreams.isEmpty()) {
                    // Sort by quality (bitrate descending)
                    java.util.Collections.sort(audioStreams, (a, b) -> {
                        long brA = a.getAverageBitrate() > 0 ? a.getAverageBitrate() : a.getBitrate();
                        long brB = b.getAverageBitrate() > 0 ? b.getAverageBitrate() : b.getBitrate();
                        return Long.compare(brB, brA);
                    });
                    
                    AudioStream firstStream = audioStreams.get(0);
                    java.util.Locale locale = firstStream.getAudioLocale();
                    String langStr = "";
                    if (locale != null) {
                        langStr = " lang=\"" + locale.getLanguage() + "\"";
                    } else if (!finalTrackId.isEmpty()) {
                        int dotIdx = finalTrackId.indexOf(".");
                        if (dotIdx != -1) {
                            langStr = " lang=\"" + finalTrackId.substring(0, dotIdx) + "\"";
                        } else {
                            langStr = " lang=\"" + finalTrackId + "\"";
                        }
                    }
                    
                    String labelStr = "";
                    String trackName = firstStream.getAudioTrackName();
                    if (trackName != null && !trackName.isEmpty()) {
                        labelStr = " label=\"" + trackName.replace("\"", "&quot;") + "\"";
                    }
                    
                    sb.append("    <AdaptationSet id=\"1\" mimeType=\"audio/mp4\" subsegmentAlignment=\"true\" subsegmentStartsWithSAP=\"1\"").append(langStr).append(labelStr).append(">\n");
                    
                    // Add Role element if track type is known
                    org.schabi.newpipe.extractor.stream.AudioTrackType type = firstStream.getAudioTrackType();
                    if (type != null) {
                        String roleVal = "main";
                        if (type == org.schabi.newpipe.extractor.stream.AudioTrackType.DUBBED) {
                            roleVal = "dub";
                        } else if (type == org.schabi.newpipe.extractor.stream.AudioTrackType.DESCRIPTIVE) {
                            roleVal = "description";
                        } else if (type == org.schabi.newpipe.extractor.stream.AudioTrackType.SECONDARY) {
                            roleVal = "alternate";
                        }
                        sb.append("      <Role schemeIdUri=\"urn:mpeg:dash:role:2011\" value=\"").append(roleVal).append("\"/>\n");
                    }
                    
                    java.util.Set<Integer> seenAudioItags = new java.util.HashSet<>();
                    for (AudioStream as : audioStreams) {
                        int itag = as.getItag();
                        if (seenAudioItags.contains(itag)) {
                            continue;
                        }
                        seenAudioItags.add(itag);
                        
                        long bitrate = as.getAverageBitrate();
                        if (bitrate <= 0) {
                            bitrate = as.getBitrate();
                        }
                        if (bitrate <= 0) {
                            bitrate = 128000;
                        }
                        if (bitrate < 1000) {
                            bitrate *= 1000;
                        }
                        String codec = as.getCodec();
                        
                        int initStart = as.getInitStart();
                        int initEnd = as.getInitEnd();
                        int indexStart = as.getIndexStart();
                        int indexEnd = as.getIndexEnd();
                        
                        if (initStart < 0 || initEnd < 0 || indexStart < 0 || indexEnd < 0) {
                            continue; // Skip streams without index range markers
                        }
                        
                        String proxyUrl = "/stream?serviceId=" + serviceId + "&amp;id=" + java.net.URLEncoder.encode(mediaUrl, "UTF-8") + "&amp;itag=" + itag + (!finalTrackId.isEmpty() ? "&amp;trackId=" + java.net.URLEncoder.encode(finalTrackId, "UTF-8") : "");
                        sb.append("      <Representation id=\"").append(itag).append("\" bandwidth=\"").append(bitrate).append("\" codecs=\"").append(codec).append("\" audioSamplingRate=\"44100\">\n");
                        sb.append("        <AudioChannelConfiguration schemeIdUri=\"urn:mpeg:dash:23003:3:audio_channel_configuration:2011\" value=\"2\"/>\n");
                        sb.append("        <BaseURL>").append(proxyUrl).append("</BaseURL>\n");
                        sb.append("        <SegmentBase indexRange=\"").append(indexStart).append("-").append(indexEnd).append("\" indexRangeExact=\"true\">\n");
                        sb.append("          <Initialization range=\"").append(initStart).append("-").append(initEnd).append("\"/>\n");
                        sb.append("        </SegmentBase>\n");
                        sb.append("      </Representation>\n");
                    }
                    sb.append("    </AdaptationSet>\n");
                }
            }

            sb.append("  </Period>\n");
            sb.append("</MPD>\n");

            String manifestXml = sb.toString();
            log("Generated local DASH manifest:\n" + manifestXml);

            byte[] bodyBytes = manifestXml.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/dash+xml; charset=UTF-8\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n";
            os.write(responseHeaders.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            os.write(bodyBytes);
            os.flush();
        }

        private void handleSubtitlesProxy(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            String mediaUrl = params.get("id");
            String lang = params.get("lang");
            boolean isAuto = "true".equals(params.get("auto"));

            StreamingService service = NewPipe.getService(serviceId);
            StreamInfo info = StreamInfo.getInfo(service, mediaUrl);

            SubtitlesStream targetStream = null;
            List<SubtitlesStream> subs = null;
            try {
                subs = info.getSubtitles();
            } catch (Exception e) {}

            if (subs != null) {
                for (SubtitlesStream sub : subs) {
                    if (sub.getLanguageTag().equals(lang) && sub.isAutoGenerated() == isAuto) {
                        targetStream = sub;
                        break;
                    }
                }
                if (targetStream == null) {
                    for (SubtitlesStream sub : subs) {
                        if (sub.getLanguageTag().equals(lang)) {
                            targetStream = sub;
                            break;
                        }
                    }
                }
            }

            if (targetStream != null) {
                String subUrl = targetStream.getContent();
                if (subUrl != null) {
                    subUrl = subUrl.replaceAll("&fmt=[^&]*", "") + "&fmt=vtt";
                }
                okhttp3.Request req = new okhttp3.Request.Builder()
                        .url(subUrl)
                        .header("User-Agent", "Mozilla/5.0")
                        .build();
                try (okhttp3.Response response = httpClient.newCall(req).execute()) {
                    byte[] bodyBytes = response.body() != null ? response.body().bytes() : new byte[0];
                    String contentType = "text/vtt";
                    String headers = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + contentType + "; charset=UTF-8\r\n" +
                            "Content-Length: " + bodyBytes.length + "\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Connection: close\r\n\r\n";
                    os.write(headers.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    os.write(bodyBytes);
                    os.flush();
                }
            } else {
                sendResponse(os, 404, "Subtitles not found", "text/plain; charset=UTF-8");
            }
        }

        private void handleDbExport(OutputStream os) throws Exception {
            String json = dbHelper.exportToJson();
            byte[] bodyBytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json; charset=UTF-8\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "Content-Disposition: attachment; filename=\"localtube_backup.json\"\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n";
            os.write(responseHeaders.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            os.write(bodyBytes);
            os.flush();
        }

        private void handleDbImport(OutputStream os, String postBody) throws Exception {
            boolean success = dbHelper.importFromJson(postBody);
            String responseText = success ? "SUCCESS" : "FAIL";
            byte[] bodyBytes = responseText.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n";
            os.write(responseHeaders.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            os.write(bodyBytes);
            os.flush();
        }

        private void handleSettings(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            String action = params.get("action");
            if ("save".equals(action)) {
                String quality = params.get("video_quality");
                String hideWatched = params.get("hide_watched");
                String hideShorts = params.get("hide_shorts");

                dbHelper.setSetting("video_quality", quality != null ? quality : "360p");
                dbHelper.setSetting("hide_watched", "on".equals(hideWatched) ? "true" : "false");
                dbHelper.setSetting("hide_shorts", "on".equals(hideShorts) ? "true" : "false");

                String redirectHeader = "HTTP/1.1 303 See Other\r\n" +
                        "Location: /settings?saved=true\r\n" +
                        "Connection: close\r\n\r\n";
                os.write(redirectHeader.getBytes("UTF-8"));
                os.flush();
                return;
            }

            String currentQuality = dbHelper.getVideoQuality();
            boolean hideWatched = dbHelper.getHideWatched();
            boolean hideShorts = dbHelper.getHideShorts();
            boolean saved = "true".equals(params.get("saved"));

            String html = HtmlRenderer.renderSettings(0, currentQuality, hideWatched, hideShorts, saved, isTv);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
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
            boolean isAjax = "ajax".equals(params.get("back"));

            if ("add".equals(action) && mediaUrl != null && !mediaUrl.isEmpty()) {
                String quality = params.get("quality");
                String audioTrack = params.get("audio_track");
                VideoCacheManager.getInstance(context).startCaching(mediaUrl, serviceId, quality, audioTrack);
                if (isAjax) {
                    sendResponse(os, 200, "{\"status\":\"success\"}", "application/json");
                } else {
                    sendRedirect(os, "/watch?serviceId=" + serviceId + "&id=" + java.net.URLEncoder.encode(mediaUrl, "UTF-8"));
                }
                return;
            } else if ("delete".equals(action) && mediaUrl != null && !mediaUrl.isEmpty()) {
                VideoCacheManager.getInstance(context).deleteCache(mediaUrl);
                if (isAjax) {
                    sendResponse(os, 200, "{\"status\":\"success\"}", "application/json");
                } else {
                    sendRedirect(os, "/cache?serviceId=" + serviceId);
                }
                return;
            }

            List<CachedVideo> cachedVideos = dbHelper.getCachedVideos();
            String html = HtmlRenderer.renderCachedList(serviceId, cachedVideos, isTv);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handleCacheStatus(OutputStream os, Map<String, String> params) throws Exception {
            String mediaUrl = params.get("id");
            if (mediaUrl == null || mediaUrl.isEmpty()) {
                sendResponse(os, 400, "{\"error\":\"Missing id\"}", "application/json");
                return;
            }
            CachedVideo cached = dbHelper.getCachedVideo(mediaUrl);
            if (cached == null) {
                sendResponse(os, 200, "{\"status\":\"NONE\",\"progress\":0}", "application/json");
            } else {
                sendResponse(os, 200, "{\"status\":\"" + cached.getStatus() + "\",\"progress\":" + cached.getProgress() + "}", "application/json");
            }
        }

        private void handleSearchHistory(OutputStream os, Map<String, String> params) throws Exception {
            String deleteQuery = params.get("delete");
            if (deleteQuery != null && !deleteQuery.isEmpty()) {
                dbHelper.deleteSearchQuery(deleteQuery);
                sendResponse(os, 200, "{\"status\":\"success\"}", "application/json");
                return;
            }
            List<String> history = dbHelper.getSearchHistory();
            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < history.size(); i++) {
                json.append("\"").append(history.get(i).replace("\"", "\\\"")).append("\"");
                if (i < history.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");
            sendResponse(os, 200, json.toString(), "application/json");
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

            if ("ajax".equals(back)) {
                sendResponse(os, 200, "{\"status\":\"success\"}", "application/json");
            } else if (back != null && !back.isEmpty()) {
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

            String rangeHeader = null;
            for (String key : requestHeaders.keySet()) {
                if ("range".equalsIgnoreCase(key)) {
                    rangeHeader = requestHeaders.get(key);
                    break;
                }
            }

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
            headBuilder.append("\r\n");

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

        private void handleSendLink(OutputStream os, Map<String, String> params, String clientIp) throws Exception {
            String videoUrl = params.get("id");
            String clientReleaseCode = params.get("release_code");
            String videoTitle = params.get("title");
            if (videoTitle == null || videoTitle.isEmpty()) {
                videoTitle = "Video";
            }

            if (videoUrl == null || videoUrl.isEmpty()) {
                sendResponse(os, 400, "{\"status\":\"error\",\"message\":\"Missing 'id' parameter\"}", "application/json; charset=UTF-8");
                return;
            }

            synchronized (LocalHttpServer.class) {
                boolean hasLock = false;
                String currentLockCode = LocalHttpServer.getActiveLockCode();
                
                if (currentLockCode == null) {
                    String newLockCode = java.util.UUID.randomUUID().toString();
                    LocalHttpServer.tryLock(newLockCode, clientIp, videoTitle);
                    currentLockCode = newLockCode;
                    hasLock = true;
                } else if (currentLockCode.equals(clientReleaseCode)) {
                    LocalHttpServer.tryLock(currentLockCode, clientIp, videoTitle);
                    hasLock = true;
                }

                if (hasLock) {
                    log("Casting link: " + videoUrl + " from client IP " + clientIp);
                    
                    addPendingCommand("play_video:" + videoUrl);

                    String json = "{\"status\":\"success\",\"release_code\":\"" + currentLockCode + "\"}";
                    sendResponse(os, 200, json, "application/json; charset=UTF-8");
                } else {
                    String busyMsg = "Server is currently controlled by device at IP " + LocalHttpServer.getActiveClientIp();
                    if (LocalHttpServer.getActiveVideoTitle() != null) {
                        busyMsg += " playing: " + LocalHttpServer.getActiveVideoTitle();
                    }
                    String json = "{\"status\":\"busy\",\"message\":\"" + busyMsg.replace("\"", "\\\"") + "\"}";
                    sendResponse(os, 200, json, "application/json; charset=UTF-8");
                }
            }
        }

        private void handleReleaseLock(OutputStream os, Map<String, String> params) throws Exception {
            String clientReleaseCode = params.get("release_code");
            if (clientReleaseCode == null || clientReleaseCode.isEmpty()) {
                sendResponse(os, 400, "{\"status\":\"error\",\"message\":\"Missing 'release_code' parameter\"}", "application/json; charset=UTF-8");
                return;
            }

            synchronized (LocalHttpServer.class) {
                String currentLockCode = LocalHttpServer.getActiveLockCode();
                if (currentLockCode != null && currentLockCode.equals(clientReleaseCode)) {
                    LocalHttpServer.releaseLock();
                    log("Lock released by client.");
                    sendResponse(os, 200, "{\"status\":\"success\"}", "application/json; charset=UTF-8");
                } else {
                    sendResponse(os, 200, "{\"status\":\"error\",\"message\":\"Invalid or expired lock code\"}", "application/json; charset=UTF-8");
                }
            }
        }

        private void handleSendCommand(OutputStream os, Map<String, String> params) throws Exception {
            String cmd = params.get("command");
            String clientReleaseCode = params.get("release_code");
            if (cmd == null || cmd.isEmpty()) {
                sendResponse(os, 400, "{\"status\":\"error\",\"message\":\"Missing 'command' parameter\"}", "application/json; charset=UTF-8");
                return;
            }

            synchronized (LocalHttpServer.class) {
                String currentLockCode = LocalHttpServer.getActiveLockCode();
                if (currentLockCode != null && currentLockCode.equals(clientReleaseCode)) {
                    addPendingCommand(cmd);
                    sendResponse(os, 200, "{\"status\":\"success\"}", "application/json; charset=UTF-8");
                } else {
                    sendResponse(os, 200, "{\"status\":\"error\",\"message\":\"Not authorized / lock expired\"}", "application/json; charset=UTF-8");
                }
            }
        }

        private void handlePollCommands(OutputStream os) throws Exception {
            List<String> cmds = getAndClearPendingCommands();
            StringBuilder sb = new StringBuilder();
            sb.append("{\"commands\":[");
            for (int i = 0; i < cmds.size(); i++) {
                sb.append("\"").append(cmds.get(i).replace("\"", "\\\"")).append("\"");
                if (i < cmds.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]}");
            sendResponse(os, 200, sb.toString(), "application/json; charset=UTF-8");
        }

        private void handleWatchLater(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            int serviceId = getServiceId(params);
            List<InfoItem> items = dbHelper.getWatchLaterItems();
            String html = HtmlRenderer.renderWatchLater(serviceId, items, isTv);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handleWatchLaterAction(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            String action = params.get("action");
            String url = params.get("url");
            String back = params.get("back");

            if ("add".equals(action) && url != null && !url.isEmpty()) {
                String title = params.get("title");
                String uploader = params.get("uploader");
                String thumbnail = params.get("thumbnail");
                String type = params.get("type"); // "video" or "playlist"
                if (title == null || title.isEmpty()) title = "Shared Item";
                if (uploader == null) uploader = "";
                if (thumbnail == null) thumbnail = "";
                if (type == null) type = "video";
                dbHelper.addWatchLater(url, title, uploader, thumbnail, type);
            } else if ("remove".equals(action) && url != null && !url.isEmpty()) {
                dbHelper.removeWatchLater(url);
            }

            if (back != null && !back.isEmpty()) {
                if (back.startsWith("/")) {
                    sendRedirect(os, back);
                } else {
                    sendRedirect(os, "/watch?serviceId=" + serviceId + "&id=" + java.net.URLEncoder.encode(back, "UTF-8"));
                }
            } else {
                sendRedirect(os, "/watch-later?serviceId=" + serviceId);
            }
        }

        private void handleDownloadCached(OutputStream os, Map<String, String> params) throws Exception {
            String mediaUrl = params.get("id");
            if (mediaUrl == null || mediaUrl.isEmpty()) {
                sendResponse(os, 400, "Missing id parameter", "text/plain; charset=UTF-8");
                return;
            }
            CachedVideo cachedVideo = dbHelper.getCachedVideo(mediaUrl);
            if (cachedVideo == null || !"COMPLETED".equals(cachedVideo.getStatus())) {
                sendResponse(os, 404, "Cached video not found or not completed yet", "text/plain; charset=UTF-8");
                return;
            }
            java.io.File file = new java.io.File(cachedVideo.getVideoLocalPath());
            if (!file.exists()) {
                sendResponse(os, 404, "Cached file not found on disk", "text/plain; charset=UTF-8");
                return;
            }
            String title = cachedVideo.getTitle();
            if (title == null || title.isEmpty()) {
                title = "video";
            }
            String filename = title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp4";
            long fileLength = file.length();
            String headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: video/mp4\r\n" +
                    "Content-Length: " + fileLength + "\r\n" +
                    "Content-Disposition: attachment; filename=\"" + java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20") + "\"\r\n" +
                    "Connection: close\r\n\r\n";
            os.write(headers.getBytes("UTF-8"));
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
            os.flush();
        }

        private void handleShortsPage(OutputStream os, Map<String, String> params, boolean isTv) throws Exception {
            int serviceId = getServiceId(params);
            String html = HtmlRenderer.renderShortsPage(serviceId, isTv);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handleShortsApiFeed(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            // Parse page index — each page does search queries
            int pageIndex = 0;
            try { pageIndex = Integer.parseInt(params.getOrDefault("page", "0")); } catch (Exception ignored) {}
            final int PAGE_SIZE = 5;

            List<StreamInfoItem> shortsItems = new ArrayList<>();
            java.util.Set<String> watchedUrls = new java.util.HashSet<>();
            for (InfoItem item : dbHelper.getHistory()) {
                watchedUrls.add(item.getUrl());
            }

            try {
                StreamingService service = NewPipe.getService(serviceId);

                // Build ordered list of queries: subscriptions first, then topics
                List<String> queries = new ArrayList<>();
                List<InfoItem> subs = dbHelper.getSubscriptions();
                if (subs != null) {
                    for (InfoItem sub : subs) {
                        String name = sub.getName();
                        if (name != null && !name.isEmpty()) queries.add(name + " shorts");
                    }
                }
                List<String> userTopics = dbHelper.getPreferredKeywords();
                if (userTopics != null && !userTopics.isEmpty()) {
                    for (String t : userTopics) queries.add(t + " shorts");
                }
                if (queries.isEmpty()) queries.add("shorts");

                // Pick exactly one query using round-robin by page index, but try up to 3 queries if empty
                int queryIdx = pageIndex % queries.size();
                int queriesTried = 0;
                final StreamingService finalService = service;
                final java.util.Set<String> finalWatched = watchedUrls;

                while (shortsItems.isEmpty() && queriesTried < Math.min(3, queries.size())) {
                    String query = queries.get((queryIdx + queriesTried) % queries.size());
                    log("Shorts feed page=" + pageIndex + " attempt=" + queriesTried + " query=" + query);

                    final String finalQuery = query;
                    java.util.concurrent.Future<List<StreamInfoItem>> future = executorService.submit(
                        new java.util.concurrent.Callable<List<StreamInfoItem>>() {
                            @Override
                            public List<StreamInfoItem> call() {
                                List<StreamInfoItem> results = new ArrayList<>();
                                try {
                                    SearchExtractor ext = finalService.getSearchExtractor(finalQuery);
                                    ext.fetchPage();
                                    if (ext.getInitialPage() != null && ext.getInitialPage().getItems() != null) {
                                        for (InfoItem itemObj : ext.getInitialPage().getItems()) {
                                            if (results.size() >= PAGE_SIZE) break;
                                            if (itemObj instanceof StreamInfoItem) {
                                                StreamInfoItem stream = (StreamInfoItem) itemObj;
                                                if (!finalWatched.contains(stream.getUrl())) {
                                                    long duration = stream.getDuration();
                                                    if (duration <= 0 || duration <= 180) {
                                                        results.add(stream);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log("Shorts search thread error: " + e.getMessage());
                                }
                                return results;
                            }
                        }
                    );
                    try {
                        shortsItems.addAll(future.get(10, java.util.concurrent.TimeUnit.SECONDS));
                    } catch (java.util.concurrent.TimeoutException e) {
                        future.cancel(true);
                        log("Shorts search timed out for query: " + query);
                    } catch (Exception e) {
                        log("Shorts future error: " + e.getMessage());
                    }
                    queriesTried++;
                }

                // If still empty and we haven't tried the generic "shorts" query, try it as fallback
                if (shortsItems.isEmpty() && !queries.contains("shorts")) {
                    log("Shorts feed page=" + pageIndex + " falling back to generic 'shorts' query");
                    java.util.concurrent.Future<List<StreamInfoItem>> future = executorService.submit(
                        new java.util.concurrent.Callable<List<StreamInfoItem>>() {
                            @Override
                            public List<StreamInfoItem> call() {
                                List<StreamInfoItem> results = new ArrayList<>();
                                try {
                                    SearchExtractor ext = finalService.getSearchExtractor("shorts");
                                    ext.fetchPage();
                                    if (ext.getInitialPage() != null && ext.getInitialPage().getItems() != null) {
                                        for (InfoItem itemObj : ext.getInitialPage().getItems()) {
                                            if (results.size() >= PAGE_SIZE) break;
                                            if (itemObj instanceof StreamInfoItem) {
                                                StreamInfoItem stream = (StreamInfoItem) itemObj;
                                                if (!finalWatched.contains(stream.getUrl())) {
                                                    long duration = stream.getDuration();
                                                    if (duration <= 0 || duration <= 180) {
                                                        results.add(stream);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log("Shorts fallback search thread error: " + e.getMessage());
                                }
                                return results;
                            }
                        }
                    );
                    try {
                        shortsItems.addAll(future.get(10, java.util.concurrent.TimeUnit.SECONDS));
                    } catch (java.util.concurrent.TimeoutException e) {
                        future.cancel(true);
                        log("Shorts fallback search timed out");
                    } catch (Exception e) {
                        log("Shorts fallback future error: " + e.getMessage());
                    }
                }

            } catch (Exception e) {
                log("Shorts API Feed error: " + e.getMessage());
            }

            StringBuilder json = new StringBuilder();
            json.append("{\"items\":[");
            for (int i = 0; i < shortsItems.size(); i++) {
                StreamInfoItem item = shortsItems.get(i);
                json.append("{")
                    .append("\"url\":\"").append(escapeJson(item.getUrl())).append("\",")
                    .append("\"name\":\"").append(escapeJson(item.getName())).append("\",")
                    .append("\"uploaderName\":\"").append(escapeJson(item.getUploaderName())).append("\",")
                    .append("\"uploaderUrl\":\"").append(escapeJson(item.getUploaderUrl())).append("\",")
                    .append("\"thumbnailUrl\":\"").append(escapeJson(item.getThumbnails() != null && !item.getThumbnails().isEmpty() ? item.getThumbnails().get(0).getUrl() : "")).append("\"")
                    .append("}");
                if (i < shortsItems.size() - 1) json.append(",");
            }
            json.append("]}");
            sendResponse(os, 200, json.toString(), "application/json; charset=UTF-8");
        }

        private String escapeJson(String input) {
            if (input == null) return "";
            return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        }
    }

    private static final class CacheData {
        final String value;
        final long expireTimestamp;

        CacheData(String value, long timeoutMillis) {
            this.value = value;
            this.expireTimestamp = System.currentTimeMillis() + timeoutMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTimestamp;
        }
    }

    private static final class StreamUrlCache {
        private static final int MAX_ITEMS = 60;
        private final java.util.LinkedHashMap<String, CacheData> map = new java.util.LinkedHashMap<String, CacheData>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheData> eldest) {
                return size() > MAX_ITEMS;
            }
        };

        public synchronized String get(String key) {
            CacheData data = map.get(key);
            if (data == null) {
                return null;
            }
            if (data.isExpired()) {
                map.remove(key);
                return null;
            }
            return data.value;
        }

        public synchronized void put(String key, String value, long timeoutMillis) {
            removeStale();
            map.put(key, new CacheData(value, timeoutMillis));
        }

        private void removeStale() {
            map.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }

        public synchronized void clear() {
            map.clear();
        }
    }
}
