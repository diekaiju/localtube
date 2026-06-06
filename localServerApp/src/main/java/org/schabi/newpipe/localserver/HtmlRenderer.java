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

    // Global CSS stylesheet for a premium, dark-mode, responsive user experience
    private static final String CSS = 
            "@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');\n" +
            "* { box-sizing: border-box; margin: 0; padding: 0; }\n" +
            "body { font-family: 'Outfit', sans-serif; background-color: #08070d; color: #e5e7eb; padding-bottom: 50px; -webkit-font-smoothing: antialiased; }\n" +
            "a { color: inherit; text-decoration: none; }\n" +
            "header { display: flex; flex-direction: column; background: rgba(18, 17, 26, 0.85); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); padding: 16px 28px; position: sticky; top: 0; z-index: 1000; border-bottom: 1px solid rgba(255, 255, 255, 0.07); }\n" +
            ".top-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; }\n" +
            ".logo { font-size: 24px; font-weight: 700; color: #ffffff; display: flex; align-items: center; gap: 10px; letter-spacing: -0.5px; }\n" +
            ".logo svg { filter: drop-shadow(0 0 8px rgba(255, 0, 85, 0.6)); }\n" +
            ".search-form { display: flex; flex-grow: 1; max-width: 600px; position: relative; }\n" +
            ".search-input { width: 100%; padding: 12px 20px; border-radius: 30px 0 0 30px; border: 1px solid rgba(255, 255, 255, 0.1); background-color: rgba(255, 255, 255, 0.05); color: #ffffff; font-size: 15px; outline: none; transition: all 0.3s ease; }\n" +
            ".search-input:focus { border-color: #a78bfa; background-color: rgba(255, 255, 255, 0.1); box-shadow: 0 0 15px rgba(167, 139, 250, 0.2); }\n" +
            ".search-btn { padding: 12px 28px; border-radius: 0 30px 30px 0; border: 1px solid rgba(255, 255, 255, 0.1); border-left: none; background: linear-gradient(135deg, #7c3aed, #db2777); color: #ffffff; cursor: pointer; font-size: 16px; transition: all 0.3s ease; }\n" +
            ".search-btn:hover { opacity: 0.9; transform: scale(1.02); }\n" +
            ".service-selector { display: flex; gap: 10px; margin-top: 14px; overflow-x: auto; padding-bottom: 4px; }\n" +
            ".service-tab { padding: 8px 20px; border-radius: 20px; font-size: 14px; font-weight: 600; background-color: rgba(255, 255, 255, 0.05); color: #d1d5db; cursor: pointer; border: 1px solid rgba(255, 255, 255, 0.05); transition: all 0.3s ease; }\n" +
            ".service-tab:hover { background-color: rgba(255, 255, 255, 0.12); color: #ffffff; }\n" +
            ".service-tab.active { background: linear-gradient(135deg, #7c3aed, #db2777); color: #ffffff; border-color: transparent; box-shadow: 0 4px 12px rgba(124, 58, 237, 0.3); }\n" +
            ".container { max-width: 1280px; margin: 28px auto; padding: 0 20px; }\n" +
            ".grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 24px; }\n" +
            ".card { background-color: #12111a; border-radius: 18px; overflow: hidden; border: 1px solid rgba(255, 255, 255, 0.05); transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); display: flex; flex-direction: column; box-shadow: 0 4px 20px rgba(0,0,0,0.2); }\n" +
            ".card:hover { transform: translateY(-6px); border-color: rgba(167, 139, 250, 0.4); box-shadow: 0 12px 30px rgba(124, 58, 237, 0.2); }\n" +
            ".card-thumbnail { width: 100%; aspect-ratio: 16/9; background-color: #1a1a26; object-fit: cover; border-bottom: 1px solid rgba(255, 255, 255, 0.05); transition: transform 0.5s ease; }\n" +
            ".card:hover .card-thumbnail { transform: scale(1.02); }\n" +
            ".card-details { padding: 16px; display: flex; flex-direction: column; flex-grow: 1; }\n" +
            ".card-title { font-size: 15px; font-weight: 600; line-height: 1.4; max-height: 2.8em; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin-bottom: 8px; color: #ffffff; transition: color 0.2s; }\n" +
            ".card-title:hover { color: #c084fc; }\n" +
            ".card-meta { font-size: 12px; color: #9ca3af; margin-top: auto; display: flex; flex-direction: column; gap: 4px; }\n" +
            ".card-uploader { font-weight: 600; color: #c084fc; }\n" +
            ".card-uploader:hover { color: #e9d5ff; }\n" +
            ".pagination { display: flex; justify-content: center; margin: 48px 0; }\n" +
            ".btn-page { display: inline-block; padding: 12px 32px; border-radius: 30px; font-weight: 600; background: linear-gradient(135deg, #7c3aed, #db2777); color: #ffffff; border: none; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 4px 15px rgba(124, 58, 237, 0.3); }\n" +
            ".btn-page:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(124, 58, 237, 0.4); }\n" +
            ".player-container { display: flex; flex-direction: column; gap: 28px; margin-top: 16px; }\n" +
            ".main-content { flex-grow: 3; display: flex; flex-direction: column; gap: 20px; }\n" +
            ".sidebar { flex-grow: 1; display: flex; flex-direction: column; gap: 20px; }\n" +
            "@media(min-width: 900px) { .player-layout { display: flex; gap: 28px; } .sidebar { width: 380px; flex-shrink: 0; } }\n" +
            ".native-player { width: 100%; aspect-ratio: 16/9; border-radius: 18px; background-color: #000; outline: none; border: 1px solid rgba(255, 255, 255, 0.08); box-shadow: 0 10px 30px rgba(0,0,0,0.5); }\n" +
            ".native-audio { width: 100%; margin: 24px 0; outline: none; filter: invert(0.9); }\n" +
            ".media-info { background-color: #12111a; padding: 24px; border-radius: 18px; border: 1px solid rgba(255, 255, 255, 0.05); }\n" +
            ".media-title { font-size: 22px; font-weight: 700; margin-bottom: 12px; color: #ffffff; line-height: 1.3; }\n" +
            ".media-stats { display: flex; justify-content: space-between; font-size: 13px; color: #9ca3af; padding-bottom: 18px; border-bottom: 1px solid rgba(255, 255, 255, 0.08); margin-bottom: 18px; flex-wrap: wrap; gap: 12px; }\n" +
            ".uploader-profile { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; flex-wrap: wrap; }\n" +
            ".uploader-avatar { width: 48px; height: 48px; border-radius: 50%; object-fit: cover; border: 2px solid rgba(255, 255, 255, 0.1); }\n" +
            ".uploader-info { display: flex; flex-direction: column; }\n" +
            ".uploader-name { font-size: 16px; font-weight: 700; color: #ffffff; }\n" +
            ".uploader-subs { font-size: 13px; color: #9ca3af; }\n" +
            ".subscribe-btn { padding: 10px 22px; border-radius: 25px; font-size: 14px; font-weight: 600; border: none; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 4px 12px rgba(0,0,0,0.2); text-align: center; }\n" +
            ".subscribe-btn:hover { transform: translateY(-1px); }\n" +
            ".media-description { font-size: 14px; line-height: 1.6; color: #d1d5db; white-space: pre-wrap; background-color: rgba(255, 255, 255, 0.03); padding: 20px; border-radius: 14px; border: 1px solid rgba(255, 255, 255, 0.04); max-height: 250px; overflow-y: auto; }\n" +
            ".comments-section { background-color: #12111a; padding: 24px; border-radius: 18px; border: 1px solid rgba(255, 255, 255, 0.05); margin-top: 12px; }\n" +
            ".comment-count { font-size: 18px; font-weight: 700; margin-bottom: 24px; color: #ffffff; }\n" +
            ".comment { display: flex; gap: 16px; margin-bottom: 24px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); padding-bottom: 18px; }\n" +
            ".comment:last-child { border-bottom: none; }\n" +
            ".comment-avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; background-color: #1a1a26; }\n" +
            ".comment-details { display: flex; flex-direction: column; gap: 6px; }\n" +
            ".comment-header { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }\n" +
            ".comment-author { font-size: 14px; font-weight: 600; color: #ffffff; }\n" +
            ".comment-time { font-size: 12px; color: #9ca3af; }\n" +
            ".comment-text { font-size: 14px; line-height: 1.5; color: #d1d5db; white-space: pre-wrap; }\n" +
            ".channel-header { background-color: #12111a; border-radius: 18px; overflow: hidden; border: 1px solid rgba(255, 255, 255, 0.05); margin-bottom: 28px; box-shadow: 0 4px 20px rgba(0,0,0,0.2); }\n" +
            ".channel-banner { width: 100%; height: 220px; object-fit: cover; background: linear-gradient(90deg, #1f1e2e, #12111a); }\n" +
            ".channel-details { display: flex; padding: 28px; align-items: center; gap: 24px; flex-wrap: wrap; }\n" +
            ".channel-avatar { width: 96px; height: 96px; border-radius: 50%; object-fit: cover; border: 3px solid rgba(255, 255, 255, 0.1); }\n" +
            ".channel-info-block { display: flex; flex-direction: column; gap: 6px; flex-grow: 1; }\n" +
            ".channel-name { font-size: 26px; font-weight: 700; color: #ffffff; }\n" +
            ".channel-desc { font-size: 14px; color: #9ca3af; max-width: 700px; margin-top: 10px; line-height: 1.5; }\n" +
            ".channel-tabs-selector { display: flex; background-color: rgba(255, 255, 255, 0.02); border-top: 1px solid rgba(255, 255, 255, 0.06); padding: 4px 20px; }\n" +
            ".channel-tab-btn { padding: 14px 24px; font-size: 14px; font-weight: 600; color: #9ca3af; border-bottom: 3px solid transparent; cursor: pointer; transition: all 0.3s ease; }\n" +
            ".channel-tab-btn:hover { color: #ffffff; }\n" +
            ".channel-tab-btn.active { color: #ffffff; border-bottom-color: #c084fc; }\n" +
            ".loading-placeholder { text-align: center; font-size: 16px; padding: 60px 0; color: #9ca3af; background-color: #12111a; border-radius: 18px; border: 1px solid rgba(255, 255, 255, 0.05); }\n" +
            ".settings-card { background-color: #12111a; padding: 32px; border-radius: 20px; border: 1px solid rgba(255, 255, 255, 0.05); max-width: 650px; margin: 0 auto; box-shadow: 0 10px 30px rgba(0,0,0,0.3); }\n" +
            ".settings-title { font-size: 24px; font-weight: 700; margin-bottom: 28px; color: #ffffff; }\n" +
            ".settings-section { margin-bottom: 28px; padding-bottom: 24px; border-bottom: 1px solid rgba(255, 255, 255, 0.06); }\n" +
            ".settings-section:last-child { border-bottom: none; }\n" +
            ".settings-section-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; color: #ffffff; }\n" +
            ".setting-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }\n" +
            ".setting-label-group { display: flex; flex-direction: column; gap: 4px; }\n" +
            ".setting-label { font-size: 15px; font-weight: 500; color: #ffffff; }\n" +
            ".setting-desc { font-size: 12px; color: #9ca3af; }\n" +
            ".switch { position: relative; display: inline-block; width: 48px; height: 26px; }\n" +
            ".switch input { opacity: 0; width: 0; height: 0; }\n" +
            ".slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #374151; transition: .3s; border-radius: 26px; }\n" +
            ".slider:before { position: absolute; content: ''; height: 18px; width: 18px; left: 4px; bottom: 4px; background-color: white; transition: .3s; border-radius: 50%; }\n" +
            "input:checked + .slider { background: linear-gradient(135deg, #7c3aed, #db2777); }\n" +
            "input:checked + .slider:before { transform: translateX(22px); }\n" +
            ".textarea-group { display: flex; flex-direction: column; gap: 10px; margin-bottom: 20px; }\n" +
            ".textarea-label { font-size: 15px; font-weight: 500; color: #ffffff; }\n" +
            ".settings-textarea { width: 100%; height: 110px; padding: 14px; border-radius: 12px; border: 1px solid rgba(255, 255, 255, 0.1); background-color: rgba(255, 255, 255, 0.03); color: #ffffff; font-size: 14px; outline: none; transition: all 0.3s ease; resize: vertical; font-family: inherit; }\n" +
            ".settings-textarea:focus { border-color: #a78bfa; background-color: rgba(255, 255, 255, 0.06); }\n" +
            ".btn-save { display: inline-block; width: 100%; padding: 14px; border-radius: 30px; font-size: 15px; font-weight: 600; text-align: center; border: none; cursor: pointer; transition: all 0.3s ease; }\n" +
            ".btn-save-primary { background: linear-gradient(135deg, #7c3aed, #db2777); color: #ffffff; box-shadow: 0 4px 15px rgba(124, 58, 237, 0.3); }\n" +
            ".btn-save-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(124, 58, 237, 0.4); }\n" +
            ".alert-banner { background-color: rgba(16, 185, 129, 0.1); color: #34d399; padding: 14px 20px; border-radius: 12px; margin-bottom: 24px; font-size: 14px; font-weight: 600; border: 1px solid rgba(52, 211, 153, 0.2); display: flex; align-items: center; gap: 10px; }\n" +
            "body.is-tv { font-size: 18px; padding-bottom: 80px; }\n" +
            "body.is-tv .container { max-width: 100%; margin: 40px auto; padding: 0 40px; }\n" +
            "body.is-tv .grid { grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 32px; }\n" +
            "body.is-tv .card-title { font-size: 18px; }\n" +
            "body.is-tv .card-meta { font-size: 14px; }\n" +
            "body.is-tv header { padding: 24px 40px; }\n" +
            "body.is-tv .logo { font-size: 32px; }\n" +
            "body.is-tv .search-input { font-size: 18px; padding: 16px 24px; }\n" +
            "body.is-tv .search-btn { font-size: 20px; padding: 16px 36px; }\n" +
            "body.is-tv .service-tab { font-size: 16px; padding: 10px 24px; }\n" +
            "body.is-tv .native-player { border-radius: 24px; }\n" +
            "body.is-tv .media-title { font-size: 30px; }\n" +
            "body.is-tv .media-stats { font-size: 16px; }\n" +
            "body.is-tv .uploader-name { font-size: 20px; }\n" +
            "body.is-tv .uploader-subs { font-size: 16px; }\n" +
            "body.is-tv .subscribe-btn { padding: 14px 28px; font-size: 16px; }\n" +
            "body.is-tv .media-description { font-size: 16px; max-height: 350px; }\n" +
            ".bottom-nav { display: none; }\n" +
            "@media (max-width: 768px) {\n" +
            "    body.is-phone { padding-bottom: 80px; }\n" +
            "    body.is-phone .service-selector { display: none; }\n" +
            "    body.is-phone .bottom-nav { display: flex; position: fixed; bottom: 0; left: 0; right: 0; background: rgba(18, 17, 26, 0.95); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border-top: 1px solid rgba(255, 255, 255, 0.08); justify-content: space-around; padding: 12px 0; z-index: 1000; }\n" +
            "    body.is-phone .bottom-nav-item { display: flex; flex-direction: column; align-items: center; gap: 4px; color: #9ca3af; font-size: 11px; font-weight: 500; transition: color 0.3s; }\n" +
            "    body.is-phone .bottom-nav-item.active { color: #ffffff; }\n" +
            "    body.is-phone .bottom-nav-icon { font-size: 20px; }\n" +
            "}\n" +
            "@media (max-width: 500px) {\n" +
            "    body.is-phone .container { padding: 0; margin-top: 12px; }\n" +
            "    body.is-phone .grid { grid-template-columns: 1fr; gap: 16px; }\n" +
            "    body.is-phone .card { border-radius: 0; border: none; background: transparent; box-shadow: none; }\n" +
            "    body.is-phone .card-thumbnail { border-radius: 0; border-bottom: none; }\n" +
            "    body.is-phone .card-details { padding: 12px 16px; }\n" +
            "    body.is-phone h2 { padding-left: 16px; }\n" +
            "}";

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
        String subsActive = "subscriptions".equals(activeTab) ? "active" : "";

        sb.append("    <a href=\"/\" class=\"service-tab ").append(ytActive).append("\">YouTube</a>\n")
          .append("    <a href=\"/subscriptions\" class=\"service-tab ").append(subsActive).append("\">🔔 Subscriptions</a>\n")
          .append("    <a href=\"/history\" class=\"service-tab ").append(histActive).append("\">📜 History</a>\n")
          .append("    <a href=\"/cache\" class=\"service-tab ").append(cachedActive).append("\">📥 Cached</a>\n");

        sb.append("  </div>\n")
          .append("</header>\n");

        sb.append("<div class=\"bottom-nav\">\n")
          .append("  <a href=\"/\" class=\"bottom-nav-item ").append(ytActive).append("\">\n")
          .append("    <span class=\"bottom-nav-icon\">🏠</span>\n")
          .append("    <span>Home</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/subscriptions\" class=\"bottom-nav-item ").append(subsActive).append("\">\n")
          .append("    <span class=\"bottom-nav-icon\">🔔</span>\n")
          .append("    <span>Library</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/history\" class=\"bottom-nav-item ").append(histActive).append("\">\n")
          .append("    <span class=\"bottom-nav-icon\">📜</span>\n")
          .append("    <span>History</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/cache\" class=\"bottom-nav-item ").append(cachedActive).append("\">\n")
          .append("    <span class=\"bottom-nav-icon\">📥</span>\n")
          .append("    <span>Cached</span>\n")
          .append("  </a>\n")
          .append("</div>\n");

        return sb.toString();
    }

    private static String wrapInTemplate(String title, String bodyContent, boolean isTv) {
        String bodyClass = isTv ? "is-tv" : "is-phone";
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + title + "</title>\n" +
                "    <link rel=\"icon\" type=\"image/svg+xml\" href=\"data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 24 24%22 fill=%22%23FF0000%22><path d=%22M23.498 6.163a3.003 3.003 0 0 0-2.11-2.11C19.518 3.545 12 3.545 12 3.545s-7.518 0-9.388.508a3.003 3.003 0 0 0-2.11 2.11C0 8.033 0 12 0 12s0 3.967.502 5.837a3.003 3.003 0 0 0 2.11 2.11c1.87.508 9.388.508 9.388.508s7.518 0 9.388-.508a3.003 3.003 0 0 0 2.11-2.11C24 15.967 24 12 24 12s0-3.967-.502-5.837zM9.545 15.568V8.432L15.818 12l-6.273 3.568z%22/></svg>\">\n" +
                "    <style>\n" + CSS + "\n    </style>\n" +
                "</head>\n" +
                "<body class=\"" + bodyClass + "\">\n" +
                bodyContent + "\n" +
                "</body>\n" +
                "</html>";
    }

    public static String renderHome(int serviceId, List<InfoItem> items, Page nextPage, boolean isTv) {
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
        return wrapInTemplate(SERVICE_NAMES[serviceId] + " - LocalTube", sb.toString(), isTv);
    }

    public static String renderHistory(int serviceId, List<InfoItem> items, boolean isTv) {
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
        return wrapInTemplate("Watch History - LocalTube", sb.toString(), isTv);
    }

    public static String renderSearch(int serviceId, String query, List<InfoItem> items, Page nextPage, boolean isTv) {
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
        return wrapInTemplate("Search: " + query, sb.toString(), isTv);
    }

    public static String renderWatch(int serviceId, StreamInfo info, CachedVideo cachedVideo, boolean isSubscribed, boolean isTv) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, ""));
        sb.append("<div class=\"container\">\n")
          .append("  <div class=\"player-container\">\n")
          .append("    <div class=\"player-layout\">\n")
          .append("      <div class=\"main-content\">\n");

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

        sb.append("        <div class=\"media-info\">\n")
          .append("          <h1 class=\"media-title\">").append(info.getName()).append("</h1>\n")
          .append("          <div class=\"media-stats\">\n")
          .append("            <span>👁️ ").append(info.getViewCount() >= 0 ? info.getViewCount() + " views" : "Unknown views").append("</span>\n")
          .append("            <span>📅 ").append(info.getTextualUploadDate() != null ? info.getTextualUploadDate() : "Unknown upload date").append("</span>\n")
          .append("            <span>👍 ").append(info.getLikeCount() >= 0 ? info.getLikeCount() : "N/A").append(" | 👎 ").append(info.getDislikeCount() >= 0 ? info.getDislikeCount() : "N/A").append("</span>\n")
          .append("          </div>\n");

        sb.append("          <div class=\"uploader-profile\">\n")
          .append("            <img class=\"uploader-avatar\" src=\"").append(getThumbnailUrl(info.getUploaderAvatars())).append("\">\n")
          .append("            <div class=\"uploader-info\">\n")
          .append("              <a href=\"/channel?serviceId=").append(serviceId).append("&id=").append(info.getUploaderUrl()).append("\" class=\"uploader-name\">")
          .append(info.getUploaderName()).append("</a>\n")
          .append("              <span class=\"uploader-subs\">").append(info.getUploaderSubscriberCount() >= 0 ? info.getUploaderSubscriberCount() + " subscribers" : "").append("</span>\n")
          .append("            </div>\n");

        if (isSubscribed) {
            sb.append("            <a href=\"/subscribe?action=unsubscribe&id=").append(encodeUrl(info.getUploaderUrl())).append("&back=").append(encodeUrl(info.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#606060; text-decoration:none; margin-left:auto; margin-right:8px;\">🔔 Subscribed</a>\n");
        } else {
            String uploaderAvatar = getThumbnailUrl(info.getUploaderAvatars());
            sb.append("            <a href=\"/subscribe?action=subscribe&id=").append(encodeUrl(info.getUploaderUrl())).append("&name=").append(encodeUrl(info.getUploaderName())).append("&avatar=").append(encodeUrl(uploaderAvatar)).append("&back=").append(encodeUrl(info.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#cc0000; text-decoration:none; margin-left:auto; margin-right:8px;\">🔔 Subscribe</a>\n");
        }

        if (cachedVideo == null) {
            sb.append("            <a href=\"/cache?action=add&id=").append(encodeUrl(info.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#007acc; text-decoration:none; margin-left:0;\">📥 Cache Offline</a>\n");
        } else if ("COMPLETED".equals(cachedVideo.getStatus())) {
            sb.append("            <a href=\"/cache?action=delete&id=").append(encodeUrl(info.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#d9534f; text-decoration:none; margin-left:0;\">🗑️ Delete Cache</a>\n");
        } else if ("DOWNLOADING".equals(cachedVideo.getStatus()) || "PENDING".equals(cachedVideo.getStatus())) {
            sb.append("            <span class=\"subscribe-btn\" style=\"background-color:#f0ad4e; text-decoration:none; cursor:default; pointer-events:none; margin-left:0;\">⏳ Caching (").append(cachedVideo.getProgress()).append("%)</span>\n");
        } else if ("FAILED".equals(cachedVideo.getStatus())) {
            sb.append("            <a href=\"/cache?action=add&id=").append(encodeUrl(info.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#d9534f; text-decoration:none; margin-left:0;\">❌ Retry Cache</a>\n");
        }

        sb.append("          </div>\n");
        sb.append("          <div class=\"media-description\">")
          .append(info.getDescription() != null ? info.getDescription().getContent() : "No description provided.")
          .append("          </div>\n")
          .append("        </div>\n");

        sb.append("        <div class=\"comments-section\">\n")
          .append("          <h3 class=\"comment-count\">💬 Comments</h3>\n");
        sb.append("          <div class=\"loading-placeholder\">Access comments by opening the related section below or scrolling.</div>\n")
          .append("        </div>\n")
          .append("      </div>\n");

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
        sb.append("      </div>\n");
        sb.append("    </div>\n")
          .append("  </div>\n")
          .append("</div>\n");

        return wrapInTemplate(info.getName(), sb.toString(), isTv);
    }

    public static String renderChannel(int serviceId, ChannelExtractor channel, String activeTab, List<InfoItem> items, Page nextPage, boolean isSubscribed, boolean isTv) throws Exception {
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
          .append("      </div>\n");

        if (isSubscribed) {
            sb.append("      <a href=\"/subscribe?action=unsubscribe&id=").append(encodeUrl(channel.getLinkHandler().getUrl())).append("&back=").append(encodeUrl("/channel?serviceId=" + serviceId + "&id=" + channel.getLinkHandler().getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#606060; text-decoration:none; margin-left:auto;\">🔔 Subscribed</a>\n");
        } else {
            String channelAvatar = getThumbnailUrl(channel.getAvatars());
            sb.append("      <a href=\"/subscribe?action=subscribe&id=").append(encodeUrl(channel.getLinkHandler().getUrl())).append("&name=").append(encodeUrl(channel.getName())).append("&avatar=").append(encodeUrl(channelAvatar)).append("&back=").append(encodeUrl("/channel?serviceId=" + serviceId + "&id=" + channel.getLinkHandler().getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#cc0000; text-decoration:none; margin-left:auto;\">🔔 Subscribe</a>\n");
        }

        sb.append("    </div>\n")
          .append("    <div class=\"channel-tabs-selector\">\n")
          .append("      <a href=\"/channel?serviceId=").append(serviceId).append("&id=").append(channel.getLinkHandler().getUrl()).append("&tab=videos\" class=\"channel-tab-btn ").append("videos".equals(activeTab) ? "active" : "").append("\">Uploads</a>\n")
          .append("      <a href=\"/channel?serviceId=").append(serviceId).append("&id=").append(channel.getLinkHandler().getUrl()).append("&tab=playlists\" class=\"channel-tab-btn ").append("playlists".equals(activeTab) ? "active" : "").append("\">Playlists</a>\n")
          .append("    </div>\n")
          .append("  </div>\n");

        if (items != null && !items.isEmpty()) {
            renderGrid(sb, serviceId, items);

            if (nextPage != null) {
                String serializedPage = serializePage(nextPage);
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

        sb.append("</div>\n");
        return wrapInTemplate(channel.getName(), sb.toString(), isTv);
    }

    public static String renderPlaylist(int serviceId, PlaylistExtractor playlist, List<InfoItem> items, Page nextPage, boolean isBookmarked, boolean isTv) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, ""));
        sb.append("<div class=\"container\">\n")
          .append("  <div class=\"channel-header\" style=\"padding:28px; display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:16px;\">\n")
          .append("    <div style=\"display:flex; flex-direction:column; gap:6px;\">\n")
          .append("      <h1 class=\"channel-name\">").append(playlist.getName()).append("</h1>\n")
          .append("      <span class=\"uploader-subs\">Playlist by ").append(playlist.getUploaderName()).append(" • ")
          .append(playlist.getStreamCount() >= 0 ? playlist.getStreamCount() + " items" : "").append("</span>\n")
          .append("    </div>\n");

        if (isBookmarked) {
            sb.append("    <a href=\"/bookmark_playlist?action=unbookmark&id=").append(encodeUrl(playlist.getLinkHandler().getUrl())).append("&back=").append(encodeUrl("/playlist?serviceId=" + serviceId + "&id=" + playlist.getLinkHandler().getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#606060; text-decoration:none;\">⭐ Bookmarked</a>\n");
        } else {
            sb.append("    <a href=\"/bookmark_playlist?action=bookmark&id=").append(encodeUrl(playlist.getLinkHandler().getUrl())).append("&name=").append(encodeUrl(playlist.getName())).append("&uploader=").append(encodeUrl(playlist.getUploaderName())).append("&back=").append(encodeUrl("/playlist?serviceId=" + serviceId + "&id=" + playlist.getLinkHandler().getUrl())).append("\" class=\"subscribe-btn\" style=\"background:linear-gradient(135deg, #7c3aed, #db2777); text-decoration:none; color:white;\">⭐ Bookmark Playlist</a>\n");
        }

        sb.append("  </div>\n");

        if (items != null && !items.isEmpty()) {
            renderGrid(sb, serviceId, items);

            if (nextPage != null) {
                String serializedPage = serializePage(nextPage);
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

        sb.append("</div>\n");
        return wrapInTemplate("Playlist: " + playlist.getName(), sb.toString(), isTv);
    }

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
            return "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?q=80&w=300&auto=format&fit=crop";
        }
        return thumbnails.get(thumbnails.size() - 1).getUrl();
    }

    private static String encodeUrl(String url) {
        try {
            return java.net.URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return url;
        }
    }

    public static String renderCachedWatch(int serviceId, CachedVideo video, List<CachedVideo> otherCached, boolean isTv) {
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

        sb.append("        <div class=\"media-info\">\n")
          .append("          <h1 class=\"media-title\">").append(video.getTitle()).append("</h1>\n")
          .append("          <div class=\"media-stats\">\n")
          .append("            <span>💾 Cached Offline</span>\n")
          .append("            <span>📅 ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(new java.util.Date(video.getTimestamp()))).append("</span>\n")
          .append("          </div>\n");

        sb.append("          <div class=\"uploader-profile\">\n")
          .append("            <div class=\"uploader-info\">\n")
          .append("              <span class=\"uploader-name\">").append(video.getUploader()).append("</span>\n")
          .append("            </div>\n")
          .append("            <a href=\"/cache?action=delete&id=").append(encodeUrl(video.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#d9534f; text-decoration:none;\">🗑️ Delete Cache</a>\n")
          .append("          </div>\n");

        sb.append("          <div class=\"media-description\">")
          .append(video.getDescription() != null && !video.getDescription().isEmpty() ? video.getDescription() : "No description cached.")
          .append("          </div>\n")
          .append("        </div>\n");

        sb.append("      </div>\n");

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
        sb.append("      </div>\n");

        sb.append("    </div>\n")
          .append("  </div>\n")
          .append("</div>\n");

        return wrapInTemplate(video.getTitle() + " - LocalTube", sb.toString(), isTv);
    }

    public static String renderCachedList(int serviceId, List<CachedVideo> items, boolean isTv) {
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
        return wrapInTemplate("Cached Videos - LocalTube", sb.toString(), isTv);
    }

    public static String renderSubscriptions(int serviceId, List<InfoItem> channels, List<InfoItem> playlists, String activeTab, boolean isTv) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, "", "subscriptions"));
        sb.append("<div class=\"container\">\n");

        boolean isPlaylists = "playlists".equals(activeTab);
        String channelsClass = !isPlaylists ? "active" : "";
        String playlistsClass = isPlaylists ? "active" : "";

        sb.append("  <div class=\"channel-header\" style=\"margin-bottom: 28px;\">\n")
          .append("    <div class=\"channel-tabs-selector\">\n")
          .append("      <a href=\"/subscriptions?serviceId=").append(serviceId).append("&tab=channels\" class=\"channel-tab-btn ").append(channelsClass).append("\">👤 Subscribed Channels (").append(channels.size()).append(")</a>\n")
          .append("      <a href=\"/subscriptions?serviceId=").append(serviceId).append("&tab=playlists\" class=\"channel-tab-btn ").append(playlistsClass).append("\">⭐ Saved Playlists (").append(playlists.size()).append(")</a>\n")
          .append("    </div>\n")
          .append("  </div>\n");

        if (isPlaylists) {
            if (playlists == null || playlists.isEmpty()) {
                sb.append("<div class=\"loading-placeholder\">You haven't saved any playlists yet.</div>\n");
            } else {
                renderGrid(sb, serviceId, playlists);
            }
        } else {
            if (channels == null || channels.isEmpty()) {
                sb.append("<div class=\"loading-placeholder\">You haven't subscribed to any channels yet.</div>\n");
            } else {
                renderGrid(sb, serviceId, channels);
            }
        }

        sb.append("</div>\n");
        return wrapInTemplate("Library - LocalTube", sb.toString(), isTv);
    }

    public static String renderOfflineHome(int serviceId, String errorMessage, List<CachedVideo> items, boolean isTv) {
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
        return wrapInTemplate("Offline Dashboard - LocalTube", sb.toString(), isTv);
    }
}
