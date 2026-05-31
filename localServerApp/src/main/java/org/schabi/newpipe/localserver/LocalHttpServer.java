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
                        threadPool.execute(new ClientHandler(socket, dbHelper));
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

        public ClientHandler(Socket socket, HistoryDbHelper dbHelper) {
            this.socket = socket;
            this.dbHelper = dbHelper;
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

                try {
                    if (path.equals("/")) {
                        handleHome(os, params);
                    } else if (path.equals("/search")) {
                        handleSearch(os, params);
                    } else if (path.equals("/watch")) {
                        handleWatch(os, params);
                    } else if (path.equals("/history")) {
                        handleHistory(os, params);
                    } else if (path.equals("/channel")) {
                        handleChannel(os, params);
                    } else if (path.equals("/playlist")) {
                        handlePlaylist(os, params);
                    } else if (path.equals("/stream")) {
                        handleStreamProxy(os, params, requestHeaders);
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

        private void handleHome(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            String nextPageStr = params.get("nextPage");
            Page nextPage = HtmlRenderer.deserializePage(nextPageStr);

            StreamingService service = NewPipe.getService(serviceId);
            SearchExtractor extractor = service.getSearchExtractor("trending");

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

            String html = HtmlRenderer.renderHome(serviceId, items, next);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handleSearch(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            String query = params.get("q");
            if (query == null || query.isEmpty()) {
                sendRedirect(os, "/?serviceId=" + serviceId);
                return;
            }

            String nextPageStr = params.get("nextPage");
            Page nextPage = HtmlRenderer.deserializePage(nextPageStr);

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

            String html = HtmlRenderer.renderSearch(serviceId, query, items, next);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handleWatch(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            String mediaUrl = params.get("id");

            StreamingService service = NewPipe.getService(serviceId);
            StreamInfo info = StreamInfo.getInfo(service, mediaUrl);

            String thumbUrl = "";
            if (info.getThumbnails() != null && !info.getThumbnails().isEmpty()) {
                thumbUrl = info.getThumbnails().get(info.getThumbnails().size() - 1).getUrl();
            }
            dbHelper.saveToHistory(info.getName(), info.getUrl(), info.getUploaderName(), thumbUrl);

            String html = HtmlRenderer.renderWatch(serviceId, info);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handleHistory(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            List<InfoItem> items = dbHelper.getHistory();
            String html = HtmlRenderer.renderHistory(serviceId, items);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handleStreamProxy(OutputStream os, Map<String, String> params, Map<String, String> requestHeaders) throws Exception {
            int serviceId = getServiceId(params);
            String mediaUrl = params.get("id");

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

        private void handleChannel(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            String channelUrl = params.get("id");
            String tab = params.getOrDefault("tab", "videos");

            String nextPageStr = params.get("nextPage");
            Page nextPage = HtmlRenderer.deserializePage(nextPageStr);

            StreamingService service = NewPipe.getService(serviceId);
            ChannelExtractor channelExtractor = service.getChannelExtractor(channelUrl);
            channelExtractor.fetchPage();

            InfoItemsPage<? extends InfoItem> itemsPage;
            if ("playlists".equals(tab)) {
                ChannelTabExtractor tabExtractor = service.getChannelTabExtractorFromIdAndBaseUrl(channelExtractor.getId(), "playlists", channelExtractor.getBaseUrl());
                if (nextPage != null) {
                    itemsPage = tabExtractor.getPage(nextPage);
                } else {
                    tabExtractor.fetchPage();
                    itemsPage = tabExtractor.getInitialPage();
                }
            } else {
                ChannelTabExtractor tabExtractor = service.getChannelTabExtractorFromIdAndBaseUrl(channelExtractor.getId(), "videos", channelExtractor.getBaseUrl());
                if (nextPage != null) {
                    itemsPage = tabExtractor.getPage(nextPage);
                } else {
                    tabExtractor.fetchPage();
                    itemsPage = tabExtractor.getInitialPage();
                }
            }

            String html = HtmlRenderer.renderChannel(serviceId, channelExtractor, tab, itemsPage);
            sendResponse(os, 200, html, "text/html; charset=UTF-8");
        }

        private void handlePlaylist(OutputStream os, Map<String, String> params) throws Exception {
            int serviceId = getServiceId(params);
            String playlistUrl = params.get("id");

            String nextPageStr = params.get("nextPage");
            Page nextPage = HtmlRenderer.deserializePage(nextPageStr);

            StreamingService service = NewPipe.getService(serviceId);
            PlaylistExtractor extractor = service.getPlaylistExtractor(playlistUrl);
            
            InfoItemsPage<? extends InfoItem> itemsPage;
            if (nextPage != null) {
                itemsPage = extractor.getPage(nextPage);
            } else {
                extractor.fetchPage();
                itemsPage = extractor.getInitialPage();
            }

            String html = HtmlRenderer.renderPlaylist(serviceId, extractor, itemsPage);
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
    }
}
