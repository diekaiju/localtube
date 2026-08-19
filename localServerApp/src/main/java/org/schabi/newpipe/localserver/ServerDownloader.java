package org.schabi.newpipe.localserver;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class ServerDownloader extends Downloader {

    private final OkHttpClient client;

    public ServerDownloader() {
        this.client = new OkHttpClient.Builder()
                .connectionPool(new okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .cookieJar(new CookieJar() {
                    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                    @Override
                    public void saveFromResponse(@Nonnull HttpUrl url, @Nonnull List<Cookie> cookies) {
                        cookieStore.put(url.host(), cookies);
                    }

                    @Nonnull
                    @Override
                    public List<Cookie> loadForRequest(@Nonnull HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url.host());
                        return cookies != null ? cookies : new ArrayList<>();
                    }
                })
                .build();
    }

    @Override
    public Response execute(@Nonnull Request request) throws IOException, ReCaptchaException {
        String method = request.httpMethod();
        String url = request.url();

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(url);

        // Add custom headers
        if (request.headers() != null) {
            for (Map.Entry<String, List<String>> entry : request.headers().entrySet()) {
                String name = entry.getKey();
                for (String val : entry.getValue()) {
                    builder.addHeader(name, val);
                }
            }
        }

        // Set User-Agent if not present (spoof desktop client or mobile depending on request)
        if (request.headers() == null || !request.headers().containsKey("User-Agent")) {
            builder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        }

        RequestBody body = null;
        if (method.equalsIgnoreCase("POST")) {
            byte[] data = request.dataToSend();
            body = RequestBody.create(data != null ? data : new byte[0], MediaType.parse("application/octet-stream"));
        }

        builder.method(method, body);

        try (okhttp3.Response okResponse = client.newCall(builder.build()).execute()) {
            if (okResponse.code() == 429) {
                throw new ReCaptchaException("ReCaptcha requested by host.", url);
            }

            int responseCode = okResponse.code();
            String responseMessage = okResponse.message();
            String latestUrl = okResponse.request().url().toString();

            // Convert headers
            Map<String, List<String>> responseHeaders = new HashMap<>();
            Headers headers = okResponse.headers();
            for (String name : headers.names()) {
                responseHeaders.put(name, headers.values(name));
            }

            String responseBody = "";
            if (okResponse.body() != null) {
                responseBody = okResponse.body().string();
            }

            return new Response(responseCode, responseMessage, responseHeaders, responseBody, latestUrl);
        }
    }
}
