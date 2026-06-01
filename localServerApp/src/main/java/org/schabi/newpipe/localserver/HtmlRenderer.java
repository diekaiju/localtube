package org.schabi.newpipe.localserver;

import android.util.Base64;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class HtmlRenderer {

    // Global CSS stylesheet for a premium, light-mode, responsive user experience
    private static final String CSS = 
            "@import url('https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700&display=swap');\n" +
            "* { box-sizing: border-box; margin: 0; padding: 0; }\n" +
            "body { font-family: 'Roboto', sans-serif; background-color: #f9f9f9; color: #0f0f0f; padding-bottom: 50px; }\n" +
            "a { color: inherit; text-decoration: none; }\n" +
            "header { display: flex; flex-direction: column; background-color: #ffffff; padding: 12px 24px; position: sticky; top: 0; z-index: 1000; border-bottom: 1px solid #e5e5e5; }\n" +
            ".top-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }\n" +
            ".logo { font-size: 20px; font-weight: 700; color: #0f0f0f; display: flex; align-items: center; gap: 8px; }\n" +
            ".logo span { color: #ff0000; font-size: 22px; }\n" +
            ".search-form { display: flex; flex-grow: 1; max-width: 600px; position: relative; }\n" +
            ".search-input { width: 100%; padding: 10px 16px; border-radius: 40px 0 0 40px; border: 1px solid #cccccc; background-color: #ffffff; color: #0f0f0f; font-size: 14px; outline: none; transition: border-color 0.15s ease; }\n" +
            ".search-input:focus { border-color: #1c62b9; box-shadow: inset 0 1px 2px rgba(0,0,0,0.05); }\n" +
            ".search-btn { padding: 10px 24px; border-radius: 0 40px 40px 0; border: 1px solid #cccccc; border-left: none; background-color: #f8f8f8; color: #0f0f0f; cursor: pointer; transition: background 0.15s; }\n" +
            ".search-btn:hover { background-color: #f0f0f0; }\n" +
            ".service-selector { display: flex; gap: 8px; margin-top: 10px; overflow-x: auto; padding-bottom: 4px; }\n" +
            ".service-tab { padding: 8px 16px; border-radius: 8px; font-size: 14px; font-weight: 500; background-color: #f2f2f2; color: #0f0f0f; cursor: pointer; border: none; transition: all 0.2s; }\n" +
            ".service-tab:hover { background-color: #e6e6e6; }\n" +
            ".service-tab.active { background-color: #0f0f0f; color: #ffffff; font-weight: 600; }\n" +
            ".container { max-width: 1280px; margin: 24px auto; padding: 0 16px; }\n" +
            ".grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }\n" +
            ".card { background-color: transparent; border-radius: 0; overflow: hidden; border: none; transition: transform 0.2s; display: flex; flex-direction: column; }\n" +
            ".card:hover { transform: translateY(-2px); }\n" +
            ".card-thumbnail { width: 100%; aspect-ratio: 16/9; background-color: #e5e5e5; object-fit: cover; border-radius: 12px; }\n" +
            ".card-details { padding: 12px 4px; display: flex; flex-direction: column; flex-grow: 1; }\n" +
            ".card-title { font-size: 14px; font-weight: 600; line-height: 1.4; max-height: 2.8em; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin-bottom: 6px; color: #0f0f0f; }\n" +
            ".card-meta { font-size: 12px; color: #606060; margin-top: auto; display: flex; flex-direction: column; gap: 3px; }\n" +
            ".card-uploader { font-weight: 500; color: #606060; }\n" +
            ".card-uploader:hover { color: #0f0f0f; }\n" +
            ".pagination { display: flex; justify-content: center; margin: 40px 0; }\n" +
            ".btn-page { display: inline-block; padding: 10px 24px; border-radius: 20px; font-weight: 600; background-color: #0f0f0f; color: #ffffff; transition: transform 0.2s; }\n" +
            ".btn-page:hover { transform: scale(1.05); }\n" +
            ".player-container { display: flex; flex-direction: column; gap: 24px; margin-top: 16px; }\n" +
            ".main-content { flex-grow: 3; display: flex; flex-direction: column; gap: 16px; }\n" +
            ".sidebar { flex-grow: 1; display: flex; flex-direction: column; gap: 16px; }\n" +
            "@media(min-width: 900px) { .player-layout { display: flex; gap: 24px; } .sidebar { width: 350px; flex-shrink: 0; } }\n" +
            ".native-player { width: 100%; aspect-ratio: 16/9; border-radius: 12px; background-color: #000; outline: none; border: 1px solid #e5e5e5; }\n" +
            ".native-audio { width: 100%; margin: 20px 0; outline: none; }\n" +
            ".media-info { background-color: transparent; padding: 0px; border-radius: 0; border: none; }\n" +
            ".media-title { font-size: 20px; font-weight: 700; margin-bottom: 12px; color: #0f0f0f; }\n" +
            ".media-stats { display: flex; justify-content: space-between; font-size: 13px; color: #606060; padding-bottom: 16px; border-bottom: 1px solid #e5e5e5; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }\n" +
            ".uploader-profile { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }\n" +
            ".uploader-avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; border: 1px solid #e5e5e5; }\n" +
            ".uploader-info { display: flex; flex-direction: column; }\n" +
            ".uploader-name { font-size: 15px; font-weight: 600; color: #0f0f0f; }\n" +
            ".uploader-subs { font-size: 12px; color: #606060; }\n" +
            ".subscribe-btn { margin-left: auto; padding: 10px 18px; border-radius: 20px; background-color: #0f0f0f; color: #ffffff; font-size: 14px; font-weight: 600; border: none; cursor: pointer; transition: background 0.15s; }\n" +
            ".subscribe-btn:hover { background-color: #272727; }\n" +
            ".media-description { font-size: 14px; line-height: 1.6; color: #0f0f0f; white-space: pre-wrap; background-color: #f2f2f2; padding: 16px; border-radius: 12px; border: none; max-height: 200px; overflow-y: auto; }\n" +
            ".comments-section { background-color: transparent; padding: 20px 0; border-radius: 0; border: none; border-top: 1px solid #e5e5e5; margin-top: 24px; }\n" +
            ".comment-count { font-size: 16px; font-weight: 700; margin-bottom: 20px; color: #0f0f0f; }\n" +
            ".comment { display: flex; gap: 12px; margin-bottom: 20px; border-bottom: 1px solid #f2f2f2; padding-bottom: 14px; }\n" +
            ".comment:last-child { border-bottom: none; }\n" +
            ".comment-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; background-color: #eee; }\n" +
            ".comment-details { display: flex; flex-direction: column; gap: 4px; }\n" +
            ".comment-header { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }\n" +
            ".comment-author { font-size: 13px; font-weight: 600; color: #0f0f0f; }\n" +
            ".comment-time { font-size: 11px; color: #606060; }\n" +
            ".comment-text { font-size: 13px; line-height: 1.5; color: #0f0f0f; white-space: pre-wrap; }\n" +
            ".channel-header { background-color: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e5e5e5; margin-bottom: 24px; }\n" +
            ".channel-banner { width: 100%; height: 180px; object-fit: cover; background: linear-gradient(90deg, #e5e5e5, #f2f2f2); }\n" +
            ".channel-details { display: flex; padding: 24px; align-items: center; gap: 20px; flex-wrap: wrap; }\n" +
            ".channel-avatar { width: 80px; height: 80px; border-radius: 50%; object-fit: cover; border: 1px solid #e5e5e5; }\n" +
            ".channel-info-block { display: flex; flex-direction: column; gap: 4px; }\n" +
            ".channel-name { font-size: 24px; font-weight: 700; color: #0f0f0f; }\n" +
            ".channel-desc { font-size: 13px; color: #606060; max-width: 600px; margin-top: 8px; }\n" +
            ".channel-tabs-selector { display: flex; background-color: #ffffff; border-top: 1px solid #e5e5e5; padding: 4px 16px; }\n" +
            ".channel-tab-btn { padding: 12px 20px; font-size: 14px; font-weight: 600; color: #606060; border-bottom: 3px solid transparent; cursor: pointer; }\n" +
            ".channel-tab-btn.active { color: #0f0f0f; border-bottom-color: #0f0f0f; }\n" +
            ".loading-placeholder { text-align: center; font-size: 16px; padding: 50px 0; color: #606060; }";

    // Supported platform details
    public static final String[] SERVICE_NAMES = {"YouTube"};

    // Serialize a Page object to a Base64 string for URL injection
    public static String serializePage(Page page) {
        if (page == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(page);
            oos.close();
            return Base64.encodeToString(baos.toByteArray(), Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Deserialize a Page object from a Base64 URL parameter
    public static Page deserializePage(String b64) {
        if (b64 == null || b64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(b64, Base64.URL_SAFE);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Page page = (Page) ois.readObject();
            ois.close();
            return page;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getHeaderHtml(int activeServiceId, String query) {
        return getHeaderHtml(activeServiceId, query, "youtube");
    }

    private static String getHeaderHtml(int activeServiceId, String query, String activeTab) {
        StringBuilder sb = new StringBuilder();
        sb.append("<header>\n")
          .append("  <div class=\"top-bar\">\n")
          .append("    <a href=\"/\" class=\"logo\"><svg viewBox=\"0 0 24 24\" width=\"28\" height=\"28\" fill=\"#FF0000\" style=\"display:inline-block; vertical-align:middle; margin-right:6px;\"><path d=\"M23.498 6.163a3.003 3.003 0 0 0-2.11-2.11C19.518 3.545 12 3.545 12 3.545s-7.518 0-9.388.508a3.003 3.003 0 0 0-2.11 2.11C0 8.033 0 12 0 12s0 3.967.502 5.837a3.003 3.003 0 0 0 2.11 2.11c1.87.508 9.388.508 9.388.508s7.518 0 9.388-.508a3.003 3.003 0 0 0 2.11-2.11C24 15.967 24 12 24 12s0-3.967-.502-5.837zM9.545 15.568V8.432L15.818 12l-6.273 3.568z\"/></svg>LocalTube</a>\n")
          .append("    <form action=\"/search\" method=\"GET\" class=\"search-form\">\n")
          .append("      <input type=\"hidden\" name=\"serviceId\" value=\"").append(activeServiceId).append("\">\n")
          .append("      <input type=\"text\" name=\"q\" class=\"search-input\" placeholder=\"Search...\" value=\"")
          .append(query != null ? query.replace("\"", "&quot;") : "").append("\" required>\n")
          .append("      <button type=\"submit\" class=\"search-btn\">🔎</button>\n")
          .append("    </form>\n")
          .append("  </div>\n")
          .append("  <div class=\"service-selector\">\n");

        String ytActive = "youtube".equals(activeTab) ? "active" : "";
        String histActive = "history".equals(activeTab) ? "active" : "";
        String cachedActive = "cached".equals(activeTab) ? "active" : "";

        sb.append("    <a href=\"/\" class=\"service-tab ").append(ytActive).append("\">YouTube</a>\n")
          .append("    <a href=\"/history\" class=\"service-tab ").append(histActive).append("\">📜 History</a>\n")
          .append("    <a href=\"/cache\" class=\"service-tab ").append(cachedActive).append("\">📥 Cached</a>\n");

        sb.append("  </div>\n")
          .append("</header>\n");
        return sb.toString();
    }

    private static String wrapInTemplate(String title, String bodyContent) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + title + "</title>\n" +
                "    <link rel=\"icon\" type=\"image/svg+xml\" href=\"data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 24 24%22 fill=%22%23FF0000%22><path d=%22M23.498 6.163a3.003 3.003 0 0 0-2.11-2.11C19.518 3.545 12 3.545 12 3.545s-7.518 0-9.388.508a3.003 3.003 0 0 0-2.11 2.11C0 8.033 0 12 0 12s0 3.967.502 5.837a3.003 3.003 0 0 0 2.11 2.11c1.87.508 9.388.508 9.388.508s7.518 0 9.388-.508a3.003 3.003 0 0 0 2.11-2.11C24 15.967 24 12 24 12s0-3.967-.502-5.837zM9.545 15.568V8.432L15.818 12l-6.273 3.568z%22/></svg>\">\n" +
                "    <style>\n" + CSS + "\n    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                bodyContent + "\n" +
                "</body>\n" +
                "</html>";
    }

    // Render Home/Kiosk Grid view
    public static String renderHome(int serviceId, List<InfoItem> items, Page nextPage) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, ""));
        sb.append("<div class=\"container\">\n")
          .append("  <h2 style=\"margin-bottom: 20px; font-weight: 700;\">🔥 Trending</h2>\n");

        renderGrid(sb, serviceId, items);

        if (nextPage != null) {
            String serializedPage = serializePage(nextPage);
            if (serializedPage != null) {
                sb.append("  <div class=\"pagination\">\n")
                  .append("    <a href=\"/?serviceId=").append(serviceId).append("&nextPage=").append(serializedPage)
                  .append("\" class=\"btn-page\">Load More</a>\n")
                  .append("  </div>\n");
            }
        }

        sb.append("</div>\n");
        return wrapInTemplate(SERVICE_NAMES[serviceId] + " - LocalTube", sb.toString());
    }

    // Render History Page
    public static String renderHistory(int serviceId, List<InfoItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, "", "history"));
        sb.append("<div class=\"container\">\n")
          .append("  <h2 style=\"margin-bottom: 20px; font-weight: 700;\">📜 Watch History</h2>\n");

        if (items == null || items.isEmpty()) {
            sb.append("<div class=\"loading-placeholder\">Your watch history is empty. Start watching videos to see them here!</div>\n");
        } else {
            renderGrid(sb, serviceId, items);
        }

        sb.append("</div>\n");
        return wrapInTemplate("Watch History - LocalTube", sb.toString());
    }

    // Render Search Results view
    public static String renderSearch(int serviceId, String query, List<InfoItem> items, Page nextPage) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, query));
        sb.append("<div class=\"container\">\n")
          .append("  <h2 style=\"margin-bottom: 20px; font-weight: 700;\">🔍 Search Results for: ")
          .append(query).append("</h2>\n");

        renderGrid(sb, serviceId, items);

        if (nextPage != null) {
            String serializedPage = serializePage(nextPage);
            if (serializedPage != null) {
                sb.append("  <div class=\"pagination\">\n")
                  .append("    <a href=\"/search?serviceId=").append(serviceId).append("&q=")
                  .append(java.net.URLEncoder.encode(query)).append("&nextPage=").append(serializedPage)
                  .append("\" class=\"btn-page\">Load More</a>\n")
                  .append("  </div>\n");
            }
        }

        sb.append("</div>\n");
        return wrapInTemplate("Search: " + query, sb.toString());
    }

    // Renders Watch Media page
    public static String renderWatch(int serviceId, StreamInfo info, CachedVideo cachedVideo) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, ""));
        sb.append("<div class=\"container\">\n")
          .append("  <div class=\"player-container\">\n")
          .append("    <div class=\"player-layout\">\n")
          .append("      <div class=\"main-content\">\n");

        // Renders Video or Audio Native Player
        boolean hasVideo = !info.getVideoStreams().isEmpty() || !info.getVideoOnlyStreams().isEmpty() || (info.getHlsUrl() != null && !info.getHlsUrl().isEmpty());
        if (hasVideo) {
            String videoMime = "video/mp4";
            if (!info.getVideoStreams().isEmpty()) {
                VideoStream stream = info.getVideoStreams().get(0);
                if (stream.getFormat() != null) {
                    videoMime = stream.getFormat().mimeType;
                }
            } else if (info.getHlsUrl() != null && !info.getHlsUrl().isEmpty()) {
                videoMime = "application/x-mpegURL";
            }
            sb.append("        <video controls autoplay class=\"native-player\" poster=\"").append(getThumbnailUrl(info.getThumbnails())).append("\">\n")
              .append("          <source src=\"/stream?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUrl())).append("\" type=\"").append(videoMime).append("\">\n")
              .append("          Your browser does not support the HTML5 video tag.\n")
              .append("        </video>\n");
        } else {
            String audioMime = "audio/mpeg";
            if (!info.getAudioStreams().isEmpty()) {
                AudioStream stream = info.getAudioStreams().get(0);
                if (stream.getFormat() != null) {
                    audioMime = stream.getFormat().mimeType;
                }
            }
            sb.append("        <div class=\"media-info\">\n")
              .append("          <img src=\"").append(getThumbnailUrl(info.getThumbnails())).append("\" style=\"width:100%; max-height:300px; object-fit:contain; border-radius:8px; background:#000;\">\n")
              .append("        </div>\n")
              .append("        <audio controls autoplay class=\"native-audio\">\n")
              .append("          <source src=\"/stream?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUrl())).append("\" type=\"").append(audioMime).append("\">\n")
              .append("          Your browser does not support the HTML5 audio tag.\n")
              .append("        </audio>\n");
        }

        // Title and Stats
        sb.append("        <div class=\"media-info\">\n")
          .append("          <h1 class=\"media-title\">").append(info.getName()).append("</h1>\n")
          .append("          <div class=\"media-stats\">\n")
          .append("            <span>👁️ ").append(info.getViewCount() >= 0 ? info.getViewCount() + " views" : "Unknown views").append("</span>\n")
          .append("            <span>📅 ").append(info.getTextualUploadDate() != null ? info.getTextualUploadDate() : "Unknown upload date").append("</span>\n")
          .append("            <span>👍 ").append(info.getLikeCount() >= 0 ? info.getLikeCount() : "N/A").append(" | 👎 ").append(info.getDislikeCount() >= 0 ? info.getDislikeCount() : "N/A").append("</span>\n")
          .append("          </div>\n");

        // Uploader profile card
        sb.append("          <div class=\"uploader-profile\">\n")
          .append("            <img class=\"uploader-avatar\" src=\"").append(getThumbnailUrl(info.getUploaderAvatars())).append("\">\n")
          .append("            <div class=\"uploader-info\">\n")
          .append("              <a href=\"/channel?serviceId=").append(serviceId).append("&id=").append(info.getUploaderUrl()).append("\" class=\"uploader-name\">")
          .append(info.getUploaderName()).append("</a>\n")
          .append("              <span class=\"uploader-subs\">").append(info.getUploaderSubscriberCount() >= 0 ? info.getUploaderSubscriberCount() + " subscribers" : "").append("</span>\n")
          .append("            </div>\n");

        if (cachedVideo == null) {
            sb.append("            <a href=\"/cache?action=add&id=").append(encodeUrl(info.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#007acc; text-decoration:none;\">📥 Cache Offline</a>\n");
        } else if ("COMPLETED".equals(cachedVideo.getStatus())) {
            sb.append("            <a href=\"/cache?action=delete&id=").append(encodeUrl(info.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#d9534f; text-decoration:none;\">🗑️ Delete Cache</a>\n");
        } else if ("DOWNLOADING".equals(cachedVideo.getStatus()) || "PENDING".equals(cachedVideo.getStatus())) {
            sb.append("            <span class=\"subscribe-btn\" style=\"background-color:#f0ad4e; text-decoration:none; cursor:default; pointer-events:none;\">⏳ Caching (").append(cachedVideo.getProgress()).append("%)</span>\n");
        } else if ("FAILED".equals(cachedVideo.getStatus())) {
            sb.append("            <a href=\"/cache?action=add&id=").append(encodeUrl(info.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#d9534f; text-decoration:none;\">❌ Retry Cache</a>\n");
        }

        sb.append("          </div>\n");

        // Description
        sb.append("          <div class=\"media-description\">")
          .append(info.getDescription() != null ? info.getDescription().getContent() : "No description provided.")
          .append("          </div>\n")
          .append("        </div>\n");

        // Render Comments list
        sb.append("        <div class=\"comments-section\">\n")
          .append("          <h3 class=\"comment-count\">💬 Comments</h3>\n");
        
        sb.append("          <div class=\"loading-placeholder\">Access comments by opening the related section below or scrolling.</div>\n");
        sb.append("        </div>\n")
          .append("      </div>\n"); // Close main-content

        // Related Items Sidebar
        sb.append("      <div class=\"sidebar\">\n")
          .append("        <h3 style=\"font-size: 16px; font-weight: 700; margin-bottom: 12px;\">Related Content</h3>\n");
        for (InfoItem related : info.getRelatedItems()) {
            sb.append("        <div class=\"card\" style=\"margin-bottom:12px; flex-direction:row; height:90px;\">\n")
              .append("          <img src=\"").append(getThumbnailUrl(related.getThumbnails())).append("\" style=\"width:120px; height:100%; object-fit:cover;\">\n")
              .append("          <div class=\"card-details\" style=\"padding:8px; justify-content:space-between;\">\n")
              .append("            <a href=\"/watch?serviceId=").append(serviceId).append("&id=").append(related.getUrl()).append("\" class=\"card-title\" style=\"font-size:12px; -webkit-line-clamp:2;\">")
              .append(related.getName()).append("</a>\n")
              .append("            <span class=\"card-meta\" style=\"font-size:10px;\">").append(related.getName()).append("</span>\n")
              .append("          </div>\n")
              .append("        </div>\n");
        }
        sb.append("      </div>\n"); // Close sidebar

        sb.append("    </div>\n") // Close player-layout
          .append("  </div>\n") // Close player-container
          .append("</div>\n"); // Close container

        return wrapInTemplate(info.getName(), sb.toString());
    }

    // Render Channel Profile page
    public static String renderChannel(int serviceId, ChannelExtractor channel, String activeTab, InfoItemsPage<? extends InfoItem> itemsPage) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, ""));
        sb.append("<div class=\"container\">\n")
          .append("  <div class=\"channel-header\">\n")
          .append("    <img class=\"channel-banner\" src=\"").append(getThumbnailUrl(channel.getBanners())).append("\">\n")
          .append("    <div class=\"channel-details\">\n")
          .append("      <img class=\"channel-avatar\" src=\"").append(getThumbnailUrl(channel.getAvatars())).append("\">\n")
          .append("      <div class=\"channel-info-block\">\n")
          .append("        <h1 class=\"channel-name\">").append(channel.getName()).append("</h1>\n")
          .append("        <span class=\"uploader-subs\">").append(channel.getSubscriberCount() >= 0 ? channel.getSubscriberCount() + " subscribers" : "").append("</span>\n")
          .append("        <p class=\"channel-desc\">").append(channel.getDescription() != null ? channel.getDescription() : "").append("</p>\n")
          .append("      </div>\n")
          .append("    </div>\n")
          // Pure CSS Tab Selector
          .append("    <div class=\"channel-tabs-selector\">\n")
          .append("      <a href=\"/channel?serviceId=").append(serviceId).append("&id=").append(channel.getLinkHandler().getUrl()).append("&tab=videos\" class=\"channel-tab-btn ").append("videos".equals(activeTab) ? "active" : "").append("\">Uploads</a>\n")
          .append("      <a href=\"/channel?serviceId=").append(serviceId).append("&id=").append(channel.getLinkHandler().getUrl()).append("&tab=playlists\" class=\"channel-tab-btn ").append("playlists".equals(activeTab) ? "active" : "").append("\">Playlists</a>\n")
          .append("    </div>\n")
          .append("  </div>\n"); // Close channel-header

        // Render uploads/playlist grid
        if (itemsPage != null && !itemsPage.getItems().isEmpty()) {
            renderGrid(sb, serviceId, (List) itemsPage.getItems());

            if (itemsPage.hasNextPage()) {
                String serializedPage = serializePage(itemsPage.getNextPage());
                if (serializedPage != null) {
                    sb.append("  <div class=\"pagination\">\n")
                      .append("    <a href=\"/channel?serviceId=").append(serviceId).append("&id=").append(channel.getLinkHandler().getUrl())
                      .append("&tab=").append(activeTab).append("&nextPage=").append(serializedPage)
                      .append("\" class=\"btn-page\">Load More</a>\n")
                      .append("  </div>\n");
                }
            }
        } else {
            sb.append("<div class=\"loading-placeholder\">No items found under this tab.</div>\n");
        }

        sb.append("</div>\n"); // Close container
        return wrapInTemplate(channel.getName(), sb.toString());
    }

    // Render Playlist view
    public static String renderPlaylist(int serviceId, PlaylistExtractor playlist, InfoItemsPage<? extends InfoItem> itemsPage) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, ""));
        sb.append("<div class=\"container\">\n")
          .append("  <div class=\"channel-header\" style=\"padding:24px;\">\n")
          .append("    <h1 class=\"channel-name\">").append(playlist.getName()).append("</h1>\n")
          .append("    <span class=\"uploader-subs\">Playlist by ").append(playlist.getUploaderName()).append(" • ")
          .append(playlist.getStreamCount() >= 0 ? playlist.getStreamCount() + " items" : "").append("</span>\n")
          .append("  </div>\n");

        if (itemsPage != null && !itemsPage.getItems().isEmpty()) {
            renderGrid(sb, serviceId, (List) itemsPage.getItems());

            if (itemsPage.hasNextPage()) {
                String serializedPage = serializePage(itemsPage.getNextPage());
                if (serializedPage != null) {
                    sb.append("  <div class=\"pagination\">\n")
                      .append("    <a href=\"/playlist?serviceId=").append(serviceId).append("&id=").append(playlist.getLinkHandler().getUrl())
                      .append("&nextPage=").append(serializedPage)
                      .append("\" class=\"btn-page\">Load More</a>\n")
                      .append("  </div>\n");
                }
            }
        } else {
            sb.append("<div class=\"loading-placeholder\">No streams in this playlist.</div>\n");
        }

        sb.append("</div>\n"); // Close container
        return wrapInTemplate("Playlist: " + playlist.getName(), sb.toString());
    }

    // Renders Comment sections dynamically
    public static String renderCommentsListHtml(List<CommentsInfoItem> commentItems) {
        StringBuilder sb = new StringBuilder();
        for (CommentsInfoItem comment : commentItems) {
            sb.append("  <div class=\"comment\">\n")
              .append("    <img class=\"comment-avatar\" src=\"").append(getThumbnailUrl(comment.getUploaderAvatars())).append("\">\n")
              .append("    <div class=\"comment-details\">\n")
              .append("      <div class=\"comment-header\">\n")
              .append("        <span class=\"comment-author\">").append(comment.getUploaderName()).append("</span>\n")
              .append("        <span class=\"comment-time\">").append(comment.getTextualUploadDate() != null ? comment.getTextualUploadDate() : "").append("</span>\n")
              .append("      </div>\n")
              .append("      <div class=\"comment-text\">").append(comment.getCommentText()).append("</div>\n")
              .append("    </div>\n")
              .append("  </div>\n");
        }
        return sb.toString();
    }

    // Render Grid helper
    private static void renderGrid(StringBuilder sb, int serviceId, List<InfoItem> items) {
        sb.append("  <div class=\"grid\">\n");
        for (InfoItem item : items) {
            String clickUrl;
            String typeBadge = "";
            switch (item.getInfoType()) {
                case PLAYLIST:
                    clickUrl = "/playlist?serviceId=" + serviceId + "&id=" + item.getUrl();
                    typeBadge = "📁 Playlist";
                    break;
                case CHANNEL:
                    clickUrl = "/channel?serviceId=" + serviceId + "&id=" + item.getUrl();
                    typeBadge = "👤 Channel";
                    break;
                case STREAM:
                default:
                    clickUrl = "/watch?serviceId=" + serviceId + "&id=" + item.getUrl();
                    break;
            }

            sb.append("    <div class=\"card\">\n")
              .append("      <a href=\"").append(clickUrl).append("\">\n")
              .append("        <img class=\"card-thumbnail\" src=\"").append(getThumbnailUrl(item.getThumbnails())).append("\">\n")
              .append("      </a>\n")
              .append("      <div class=\"card-details\">\n")
              .append("        <a href=\"").append(clickUrl).append("\" class=\"card-title\">").append(item.getName()).append("</a>\n")
              .append("        <div class=\"card-meta\">\n");

            if (!typeBadge.isEmpty()) {
                sb.append("          <span style=\"color:#ff4e50; font-weight:bold; font-size:11px;\">").append(typeBadge).append("</span>\n");
            } else if (item instanceof StreamInfoItem) {
                StreamInfoItem stream = (StreamInfoItem) item;
                sb.append("          <a href=\"/channel?serviceId=").append(serviceId).append("&id=").append(stream.getUploaderUrl()).append("\" class=\"card-uploader\">")
                  .append(stream.getUploaderName()).append("</a>\n")
                  .append("          <span>👁️ ").append(stream.getViewCount() >= 0 ? stream.getViewCount() + " views" : "Live / Dynamic").append(" • ")
                  .append(stream.getTextualUploadDate() != null ? stream.getTextualUploadDate() : "").append("</span>\n");
            } else {
                sb.append("          <span class=\"card-uploader\">").append(item.getName()).append("</span>\n");
            }

            sb.append("        </div>\n")
              .append("      </div>\n")
              .append("    </div>\n");
        }
        sb.append("  </div>\n");
    }

    private static String getThumbnailUrl(List<Image> thumbnails) {
        if (thumbnails == null || thumbnails.isEmpty()) {
            return "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?q=80&w=300&auto=format&fit=crop"; // fall back generic thumbnail
        }
        // Grab highest resolution thumbnail available
        return thumbnails.get(thumbnails.size() - 1).getUrl();
    }

    private static String encodeUrl(String url) {
        try {
            return java.net.URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return url;
        }
    }

    public static String renderCachedWatch(int serviceId, CachedVideo video, List<CachedVideo> otherCached) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, "", "cached"));
        sb.append("<div class=\"container\">\n")
          .append("  <div class=\"player-container\">\n")
          .append("    <div class=\"player-layout\">\n")
          .append("      <div class=\"main-content\">\n");

        sb.append("        <video controls autoplay class=\"native-player\" poster=\"/thumbnail?id=").append(encodeUrl(video.getUrl())).append("\">\n")
          .append("          <source src=\"/stream?serviceId=").append(serviceId).append("&id=").append(encodeUrl(video.getUrl())).append("\" type=\"video/mp4\">\n")
          .append("          Your browser does not support the HTML5 video tag.\n")
          .append("        </video>\n");

        // Title and Stats
        sb.append("        <div class=\"media-info\">\n")
          .append("          <h1 class=\"media-title\">").append(video.getTitle()).append("</h1>\n")
          .append("          <div class=\"media-stats\">\n")
          .append("            <span>💾 Cached Offline</span>\n")
          .append("            <span>📅 ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(new java.util.Date(video.getTimestamp()))).append("</span>\n")
          .append("          </div>\n");

        // Uploader profile card
        sb.append("          <div class=\"uploader-profile\">\n")
          .append("            <div class=\"uploader-info\">\n")
          .append("              <span class=\"uploader-name\">").append(video.getUploader()).append("</span>\n")
          .append("            </div>\n")
          .append("            <a href=\"/cache?action=delete&id=").append(encodeUrl(video.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#d9534f; text-decoration:none;\">🗑️ Delete Cache</a>\n")
          .append("          </div>\n");

        // Description
        sb.append("          <div class=\"media-description\">")
          .append(video.getDescription() != null && !video.getDescription().isEmpty() ? video.getDescription() : "No description cached.")
          .append("          </div>\n")
          .append("        </div>\n");

        sb.append("      </div>\n"); // Close main-content

        // Related Items Sidebar
        sb.append("      <div class=\"sidebar\">\n")
          .append("        <h3 style=\"font-size: 16px; font-weight: 700; margin-bottom: 12px;\">Other Cached Videos</h3>\n");
        int count = 0;
        for (CachedVideo other : otherCached) {
            if (other.getUrl().equals(video.getUrl())) continue;
            count++;
            sb.append("        <div class=\"card\" style=\"margin-bottom:12px; flex-direction:row; height:90px;\">\n")
              .append("          <img src=\"/thumbnail?id=").append(encodeUrl(other.getUrl())).append("\" style=\"width:120px; height:100%; object-fit:cover; border-radius:8px;\">\n")
              .append("          <div class=\"card-details\" style=\"padding:8px; justify-content:space-between;\">\n")
              .append("            <a href=\"/watch?serviceId=").append(serviceId).append("&id=").append(encodeUrl(other.getUrl())).append("\" class=\"card-title\" style=\"font-size:12px; -webkit-line-clamp:2;\">")
              .append(other.getTitle()).append("</a>\n")
              .append("            <span class=\"card-meta\" style=\"font-size:10px;\">").append(other.getUploader()).append("</span>\n")
              .append("          </div>\n")
              .append("        </div>\n");
        }
        if (count == 0) {
            sb.append("<div style=\"font-size:13px; color:#606060;\">No other cached videos.</div>\n");
        }
        sb.append("      </div>\n"); // Close sidebar

        sb.append("    </div>\n") // Close player-layout
          .append("  </div>\n") // Close player-container
          .append("</div>\n"); // Close container

        return wrapInTemplate(video.getTitle() + " - LocalTube", sb.toString());
    }

    public static String renderCachedList(int serviceId, List<CachedVideo> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, "", "cached"));
        sb.append("<div class=\"container\">\n")
          .append("  <h2 style=\"margin-bottom: 20px; font-weight: 700;\">📥 Cached Videos</h2>\n");

        if (items == null || items.isEmpty()) {
            sb.append("<div class=\"loading-placeholder\">No cached videos found. Browse videos and click \"Cache Offline\" to save them!</div>\n");
        } else {
            sb.append("  <div class=\"grid\">\n");
            for (CachedVideo item : items) {
                String clickUrl = "/watch?serviceId=" + serviceId + "&id=" + encodeUrl(item.getUrl());
                String statusLabel = "";
                String statusColor = "#606060";
                if ("COMPLETED".equals(item.getStatus())) {
                    statusLabel = "✅ Saved Offline";
                    statusColor = "#2b8a3e";
                } else if ("DOWNLOADING".equals(item.getStatus())) {
                    statusLabel = "⏳ Caching (" + item.getProgress() + "%)";
                    statusColor = "#e67e22";
                } else if ("PENDING".equals(item.getStatus())) {
                    statusLabel = "⏳ Pending...";
                    statusColor = "#9b59b6";
                } else if ("FAILED".equals(item.getStatus())) {
                    statusLabel = "❌ Failed";
                    statusColor = "#c0392b";
                }

                sb.append("    <div class=\"card\" style=\"position: relative;\">\n")
                  .append("      <a href=\"").append(clickUrl).append("\">\n")
                  .append("        <img class=\"card-thumbnail\" src=\"/thumbnail?id=").append(encodeUrl(item.getUrl())).append("\">\n")
                  .append("      </a>\n")
                  .append("      <div class=\"card-details\">\n")
                  .append("        <a href=\"").append(clickUrl).append("\" class=\"card-title\">").append(item.getTitle()).append("</a>\n")
                  .append("        <div class=\"card-meta\">\n")
                  .append("          <span class=\"card-uploader\">").append(item.getUploader()).append("</span>\n")
                  .append("          <div style=\"display:flex; justify-content:space-between; align-items:center; margin-top:8px;\">\n")
                  .append("            <span style=\"color:").append(statusColor).append("; font-weight:bold; font-size:12px;\">").append(statusLabel).append("</span>\n")
                  .append("            <a href=\"/cache?action=delete&id=").append(encodeUrl(item.getUrl())).append("\" style=\"color:#d9534f; font-weight:bold; font-size:12px;\">🗑️ Delete</a>\n")
                  .append("          </div>\n")
                  .append("        </div>\n")
                  .append("      </div>\n")
                  .append("    </div>\n");
            }
            sb.append("  </div>\n");
        }

        sb.append("</div>\n");
        return wrapInTemplate("Cached Videos - LocalTube", sb.toString());
    }

    public static String renderOfflineHome(int serviceId, String errorMessage, List<CachedVideo> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, "", "cached"));
        sb.append("<div class=\"container\">\n")
          .append("  <div style=\"background-color:#fce8e6; color:#c5221f; padding:16px; border-radius:12px; margin-bottom:24px; font-size:14px; font-weight:500; border: 1px solid #fad2cf;\">\n")
          .append("    📶 You are currently offline (").append(errorMessage).append("). Showing your locally cached videos.\n")
          .append("  </div>\n")
          .append("  <h2 style=\"margin-bottom: 20px; font-weight: 700;\">📥 Offline Library</h2>\n");

        boolean hasItems = false;
        if (items != null) {
            for (CachedVideo item : items) {
                if ("COMPLETED".equals(item.getStatus())) {
                    hasItems = true;
                    break;
                }
            }
        }

        if (!hasItems) {
            sb.append("<div class=\"loading-placeholder\">No offline videos available. Connect to the internet to cache videos!</div>\n");
        } else {
            sb.append("  <div class=\"grid\">\n");
            for (CachedVideo item : items) {
                if (!"COMPLETED".equals(item.getStatus())) continue;
                String clickUrl = "/watch?serviceId=" + serviceId + "&id=" + encodeUrl(item.getUrl());
                sb.append("    <div class=\"card\">\n")
                  .append("      <a href=\"").append(clickUrl).append("\">\n")
                  .append("        <img class=\"card-thumbnail\" src=\"/thumbnail?id=").append(encodeUrl(item.getUrl())).append("\">\n")
                  .append("      </a>\n")
                  .append("      <div class=\"card-details\">\n")
                  .append("        <a href=\"").append(clickUrl).append("\" class=\"card-title\">").append(item.getTitle()).append("</a>\n")
                  .append("        <div class=\"card-meta\">\n")
                  .append("          <span class=\"card-uploader\">").append(item.getUploader()).append("</span>\n")
                  .append("        </div>\n")
                  .append("      </div>\n")
                  .append("    </div>\n");
            }
            sb.append("  </div>\n");
        }

        sb.append("</div>\n");
        return wrapInTemplate("Offline Dashboard - LocalTube", sb.toString());
    }
}
