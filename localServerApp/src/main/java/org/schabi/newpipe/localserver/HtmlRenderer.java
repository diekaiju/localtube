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
import org.schabi.newpipe.extractor.stream.SubtitlesStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class HtmlRenderer {

    public static java.util.Map<String, String> lightColors = new java.util.HashMap<>();
    public static java.util.Map<String, String> darkColors = new java.util.HashMap<>();

    // Global CSS stylesheet for a premium, themeable, responsive user experience
    private static final String CSS = 
            "@import url('https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700&display=swap');\n" +
            ":root {\n" +
            "  --bg-color: #fbfafe;\n" +
            "  --text-color: #1d1b20;\n" +
            "  --header-bg: #f3f4f9;\n" +
            "  --header-border: transparent;\n" +
            "  --logo-color: #6750A4;\n" +
            "  --search-input-border: transparent;\n" +
            "  --search-input-bg: #ece6f0;\n" +
            "  --search-input-color: #1d1b20;\n" +
            "  --search-btn-bg: #ece6f0;\n" +
            "  --search-btn-hover: #e8def8;\n" +
            "  --service-tab-bg: #ece6f0;\n" +
            "  --service-tab-color: #49454f;\n" +
            "  --service-tab-hover-bg: #e8def8;\n" +
            "  --service-tab-hover-color: #1d1b20;\n" +
            "  --card-bg: #ffffff;\n" +
            "  --card-border: transparent;\n" +
            "  --card-thumbnail-bg: #ece6f0;\n" +
            "  --card-title-color: #1d1b20;\n" +
            "  --card-meta-color: #49454f;\n" +
            "  --media-info-bg: transparent;\n" +
            "  --media-info-border: rgba(0, 0, 0, 0.05);\n" +
            "  --media-title-color: #1d1b20;\n" +
            "  --media-stats-color: #49454f;\n" +
            "  --uploader-name-color: #1d1b20;\n" +
            "  --uploader-subs-color: #49454f;\n" +
            "  --media-desc-color: #1d1b20;\n" +
            "  --media-desc-bg: #f3f4f9;\n" +
            "  --media-desc-border: transparent;\n" +
            "  --comments-bg: transparent;\n" +
            "  --comments-border: rgba(0, 0, 0, 0.05);\n" +
            "  --comment-count-color: #1d1b20;\n" +
            "  --comment-border: rgba(0, 0, 0, 0.03);\n" +
            "  --comment-author-color: #1d1b20;\n" +
            "  --comment-time-color: #49454f;\n" +
            "  --comment-text-color: #1d1b20;\n" +
            "  --channel-header-bg: transparent;\n" +
            "  --channel-header-border: rgba(0, 0, 0, 0.05);\n" +
            "  --channel-name-color: #1d1b20;\n" +
            "  --channel-desc-color: #49454f;\n" +
            "  --bottom-nav-bg: #f3f4f9;\n" +
            "  --bottom-nav-border: transparent;\n" +
            "  --bottom-nav-item-color: #49454f;\n" +
            "  --bottom-nav-item-active-color: #21005d;\n" +
            "  --bottom-nav-active-pill-bg: #e8def8;\n" +
            "  --settings-card-bg: #ffffff;\n" +
            "  --settings-card-border: transparent;\n" +
            "  --settings-title-color: #1d1b20;\n" +
            "  --settings-section-border: rgba(0, 0, 0, 0.05);\n" +
            "  --settings-section-title-color: #6750A4;\n" +
            "  --setting-label-color: #1d1b20;\n" +
            "  --setting-desc-color: #49454f;\n" +
            "  --textarea-label-color: #1d1b20;\n" +
            "  --textarea-border: #79747e;\n" +
            "  --textarea-bg: #ffffff;\n" +
            "  --textarea-color: #1d1b20;\n" +
            "  --slider-bg: #e8def8;\n" +
            "}\n" +
            "[data-theme=\"dark\"] {\n" +
            "  --bg-color: #141218;\n" +
            "  --text-color: #e6e1e5;\n" +
            "  --header-bg: #1d1b20;\n" +
            "  --header-border: transparent;\n" +
            "  --logo-color: #d0bcff;\n" +
            "  --search-input-border: transparent;\n" +
            "  --search-input-bg: #2b2930;\n" +
            "  --search-input-color: #e6e1e5;\n" +
            "  --search-btn-bg: #2b2930;\n" +
            "  --search-btn-hover: #4a4458;\n" +
            "  --service-tab-bg: #2b2930;\n" +
            "  --service-tab-color: #cac4d0;\n" +
            "  --service-tab-hover-bg: #4a4458;\n" +
            "  --service-tab-hover-color: #e8def8;\n" +
            "  --card-bg: #1d1b20;\n" +
            "  --card-border: transparent;\n" +
            "  --card-thumbnail-bg: #2b2930;\n" +
            "  --card-title-color: #e6e1e5;\n" +
            "  --card-meta-color: #cac4d0;\n" +
            "  --media-info-bg: transparent;\n" +
            "  --media-info-border: rgba(255, 255, 255, 0.05);\n" +
            "  --media-title-color: #e6e1e5;\n" +
            "  --media-stats-color: #cac4d0;\n" +
            "  --uploader-name-color: #e6e1e5;\n" +
            "  --uploader-subs-color: #cac4d0;\n" +
            "  --media-desc-color: #e6e1e5;\n" +
            "  --media-desc-bg: #2b2930;\n" +
            "  --media-desc-border: transparent;\n" +
            "  --comments-bg: transparent;\n" +
            "  --comments-border: rgba(255, 255, 255, 0.05);\n" +
            "  --comment-count-color: #e6e1e5;\n" +
            "  --comment-border: rgba(255, 255, 255, 0.03);\n" +
            "  --comment-author-color: #e6e1e5;\n" +
            "  --comment-time-color: #cac4d0;\n" +
            "  --comment-text-color: #e6e1e5;\n" +
            "  --channel-header-bg: transparent;\n" +
            "  --channel-header-border: rgba(255, 255, 255, 0.05);\n" +
            "  --channel-name-color: #e6e1e5;\n" +
            "  --channel-desc-color: #cac4d0;\n" +
            "  --bottom-nav-bg: #1d1b20;\n" +
            "  --bottom-nav-border: transparent;\n" +
            "  --bottom-nav-item-color: #cac4d0;\n" +
            "  --bottom-nav-item-active-color: #e8def8;\n" +
            "  --bottom-nav-active-pill-bg: #4a4458;\n" +
            "  --settings-card-bg: #1d1b20;\n" +
            "  --settings-card-border: transparent;\n" +
            "  --settings-title-color: #e6e1e5;\n" +
            "  --settings-section-border: rgba(255, 255, 255, 0.05);\n" +
            "  --settings-section-title-color: #d0bcff;\n" +
            "  --setting-label-color: #e6e1e5;\n" +
            "  --setting-desc-color: #cac4d0;\n" +
            "  --textarea-label-color: #e6e1e5;\n" +
            "  --textarea-border: #938f99;\n" +
            "  --textarea-bg: #1d1b20;\n" +
            "  --textarea-color: #e6e1e5;\n" +
            "  --slider-bg: #4a4458;\n" +
            "}\n" +
            "* { box-sizing: border-box; margin: 0; padding: 0; }\n" +
            "body { font-family: 'Roboto', sans-serif; background-color: var(--bg-color); color: var(--text-color); -webkit-font-smoothing: antialiased; transition: background-color 0.2s, color 0.2s; overflow-x: hidden; overflow-wrap: break-word; word-wrap: break-word; }\n" +
            "a { color: inherit; text-decoration: none; }\n" +
            "header { display: flex; align-items: center; justify-content: space-between; background: var(--header-bg); padding: 0 16px; position: fixed; top: 0; left: 0; right: 0; height: 56px; z-index: 1000; border-bottom: 1px solid var(--header-border); transition: background 0.2s, border-bottom 0.2s; }\n" +
            ".top-bar { display: flex; align-items: center; justify-content: space-between; width: 100%; height: 100%; gap: 16px; }\n" +
            ".logo { font-size: 20px; font-weight: 700; color: var(--logo-color); display: flex; align-items: center; gap: 4px; letter-spacing: -0.8px; transition: color 0.2s; font-family: 'Roboto', sans-serif; }\n" +
            ".search-form { display: flex; flex-grow: 1; max-width: 640px; position: relative; margin: 0 16px; border-radius: 28px; background-color: var(--search-input-bg); overflow: hidden; height: 48px; align-items: center; padding-left: 8px; }\n" +
            ".search-input { flex-grow: 1; height: 100%; border: none; background: transparent; color: var(--search-input-color); padding: 0 16px; font-size: 16px; outline: none; }\n" +
            ".search-input:focus { border: none; }\n" +
            ".search-btn { height: 40px; width: 48px; border-radius: 24px; border: none; background: transparent; color: var(--text-color); cursor: pointer; display: flex; align-items: center; justify-content: center; margin-right: 4px; }\n" +
            ".search-btn:hover { background-color: var(--search-btn-hover); }\n" +
            ".service-selector { display: none; }\n" +
            ".container { transition: all 0.2s ease; }\n" +
            ".grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 40px 16px; }\n" +
            ".card { display: flex; flex-direction: column; cursor: pointer; background-color: var(--card-bg); border-radius: 24px; padding: 12px; border: 1px solid var(--card-border); transition: transform 0.2s, box-shadow 0.2s; min-width: 0; overflow: hidden; word-break: break-word; overflow-wrap: break-word; }\n" +
            ".card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }\n" +
            ".card-thumbnail { width: 100%; aspect-ratio: 16/9; background-color: var(--card-thumbnail-bg); object-fit: cover; border-radius: 16px; transition: border-radius 0.2s; flex-shrink: 0; max-height: 240px; }\n" +
            ".card-details { display: flex; gap: 12px; padding: 12px 0 0 0; min-width: 0; overflow: hidden; }\n" +
            ".card-avatar { width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; color: white; font-size: 15px; flex-shrink: 0; aspect-ratio: 1 / 1; object-fit: cover; }\n" +
            ".card-info { display: flex; flex-direction: column; flex-grow: 1; min-width: 0; overflow: hidden; word-break: break-word; overflow-wrap: break-word; }\n" +
            ".card-title { font-size: 15px; font-weight: 500; line-height: 1.4; max-height: 2.8em; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin-bottom: 4px; color: var(--card-title-color); word-break: break-word; overflow-wrap: break-word; min-width: 0; }\n" +
            ".card-meta { font-size: 13px; color: var(--card-meta-color); display: flex; flex-direction: column; gap: 2px; word-break: break-word; overflow-wrap: break-word; min-width: 0; }\n" +
            ".card-uploader { font-weight: 500; color: var(--card-meta-color); text-decoration: none; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; display: inline-block; }\n" +
            ".card-uploader:hover { color: var(--text-color); }\n" +
            ".pagination { display: flex; justify-content: center; margin: 32px 0; }\n" +
            ".btn-page { display: inline-block; padding: 10px 24px; border-radius: 100px; font-weight: 500; font-size: 14px; background-color: var(--service-tab-bg); color: var(--text-color); border: none; cursor: pointer; transition: background-color 0.2s; }\n" +
            ".btn-page:hover { background-color: var(--service-tab-hover-bg); }\n" +
            ".sidebar-nav { position: fixed; top: 56px; left: 0; bottom: 0; width: 240px; background-color: var(--bg-color); padding: 12px 4px; display: flex; flex-direction: column; gap: 4px; z-index: 99; overflow-y: auto; }\n" +
            ".sidebar-item { display: flex; align-items: center; gap: 24px; padding: 12px 24px; border-radius: 100px; font-size: 14px; font-weight: 500; color: var(--text-color); transition: background-color 0.2s; cursor: pointer; margin: 0 12px; }\n" +
            ".sidebar-item:hover { background-color: var(--service-tab-hover-bg); }\n" +
            ".sidebar-item.active { font-weight: 700; background-color: var(--bottom-nav-active-pill-bg); color: var(--bottom-nav-item-active-color); }\n" +
            ".sidebar-icon { font-size: 18px; }\n" +
            "@media (min-width: 769px) {\n" +
            "  .bottom-nav { display: none !important; }\n" +
            "  .sidebar-nav { display: flex !important; }\n" +
            "  .container { margin-left: 240px; max-width: calc(100% - 240px); padding: 24px 40px; margin-top: 56px; }\n" +
            "}\n" +
            "@media (max-width: 768px) {\n" +
            "  .sidebar-nav { display: none !important; }\n" +
            "  .bottom-nav { display: flex !important; position: fixed; bottom: 0; left: 0; right: 0; height: 80px; background: var(--bottom-nav-bg); border-top: none; box-shadow: 0 -1px 3px rgba(0,0,0,0.05); justify-content: space-around; align-items: center; z-index: 1000; padding-bottom: 8px; }\n" +
            "  .container { margin-left: 0; max-width: 100%; padding: 0 12px; margin-top: 56px; padding-bottom: 96px; }\n" +
            "  .container h2 { padding: 16px 4px 0 4px; margin: 0 !important; }\n" +
            "  .grid { grid-template-columns: 1fr; gap: 20px; }\n" +
            "  .card { padding: 0; }\n" +
            "  .card-thumbnail { border-radius: 16px; }\n" +
            "  .card-details { padding: 12px 4px; }\n" +
            "  .bottom-nav-item { display: flex; flex-direction: column; align-items: center; gap: 4px; color: var(--bottom-nav-item-color); font-size: 12px; font-weight: 500; text-decoration: none; flex-grow: 1; justify-content: center; }\n" +
            "  .bottom-nav-item .bottom-nav-icon { display: flex; align-items: center; justify-content: center; width: 64px; height: 32px; border-radius: 16px; transition: background-color 0.2s ease, color 0.2s ease; color: var(--bottom-nav-item-color); }\n" +
            "  .bottom-nav-item.active .bottom-nav-icon { background-color: var(--bottom-nav-active-pill-bg); color: var(--bottom-nav-item-active-color); }\n" +
            "  .bottom-nav-item.active { color: var(--text-color); }\n" +
            "  #theme-toggle { display: none !important; }\n" +
            "  #mobile-theme-row, #mobile-history-row { display: flex !important; }\n" +
            "  .search-form { max-width: 40px; margin: 0; overflow: hidden; transition: max-width 0.3s ease; border-radius: 40px; }\n" +
            "  .search-form.search-active { max-width: 100%; width: 100%; margin-left: 8px; }\n" +
            "  .search-input { width: 0; padding: 0; border: none; transition: width 0.3s ease, opacity 0.3s ease; opacity: 0; }\n" +
            "  .search-form.search-active .search-input { width: calc(100% - 48px); padding: 0 16px; border: 1px solid var(--search-input-border); opacity: 1; }\n" +
            "  .search-btn { border-radius: 40px; border: none; background: transparent; }\n" +
            "  .search-form.search-active .search-btn { border-radius: 0 40px 40px 0; border: 1px solid var(--search-input-border); border-left: none; background-color: var(--search-btn-bg); }\n" +
            "  .top-bar.search-active div:first-child, .top-bar.search-active div:last-child { display: none !important; }\n" +
            "}\n" +
            ".player-container { display: flex; flex-direction: column; gap: 20px; margin-top: 16px; }\n" +
            ".player-layout { display: flex; flex-direction: column; gap: 24px; }\n" +
            "@media (min-width: 1024px) {\n" +
            "  .player-layout { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }\n" +
            "}\n" +
            ".main-content { display: flex; flex-direction: column; gap: 16px; }\n" +
            ".sidebar { display: flex; flex-direction: column; gap: 16px; }\n" +
            ".native-player { width: 100%; aspect-ratio: 16/9; border-radius: 12px; background-color: #000; outline: none; }\n" +
            ".media-info { padding: 16px 0; border-bottom: 1px solid var(--media-info-border); min-width: 0; overflow: hidden; }\n" +
            ".media-title { font-size: 20px; font-weight: 700; margin-bottom: 8px; color: var(--media-title-color); line-height: 1.4; word-break: break-word; overflow-wrap: break-word; min-width: 0; }\n" +
            ".media-stats { font-size: 14px; color: var(--media-stats-color); margin-bottom: 12px; }\n" +
            ".uploader-profile { display: flex; flex-direction: column; gap: 12px; margin-bottom: 16px; }\n" +
            "@media (min-width: 768px) {\n" +
            "  .uploader-profile { flex-direction: row; align-items: center; justify-content: space-between; gap: 16px; }\n" +
            "}\n" +
            ".uploader-main { display: flex; align-items: center; gap: 12px; width: 100%; min-width: 0; }\n" +
            "@media (min-width: 768px) {\n" +
            "  .uploader-main { width: auto; }\n" +
            "}\n" +
            ".uploader-avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; flex-shrink: 0; aspect-ratio: 1 / 1; }\n" +
            ".uploader-info { display: flex; flex-direction: column; justify-content: center; flex-grow: 1; min-width: 0; overflow: hidden; word-break: break-word; overflow-wrap: break-word; }\n" +
            ".uploader-name { font-size: 15px; font-weight: 600; color: var(--uploader-name-color, var(--text-color)); text-decoration: none; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }\n" +
            ".uploader-subs { font-size: 12px; color: var(--uploader-subs-color, #a0a0a0); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }\n" +
            ".subscribe-btn { padding: 8px 20px; border-radius: 100px; font-size: 13px; font-weight: 600; border: none; cursor: pointer; text-decoration: none; text-align: center; color: #ffffff; background-color: var(--logo-color, #6750A4); flex-shrink: 0; margin-left: auto; white-space: nowrap; transition: opacity 0.2s; }\n" +
            ".subscribe-btn.subscribed { background-color: var(--service-tab-bg, rgba(255,255,255,0.1)); color: var(--text-color); }\n" +
            ".action-buttons-group { display: flex; align-items: center; gap: 8px; overflow-x: auto; padding-bottom: 4px; scrollbar-width: none; width: 100%; }\n" +
            "@media (min-width: 768px) {\n" +
            "  .action-buttons-group { width: auto; }\n" +
            "}\n" +
            ".action-buttons-group::-webkit-scrollbar { display: none; }\n" +
            ".like-dislike-pill { display: inline-flex; align-items: center; background-color: var(--service-tab-bg, rgba(255,255,255,0.1)); border-radius: 100px; height: 36px; overflow: hidden; flex-shrink: 0; }\n" +
            ".pill-btn { background: none; border: none; padding: 0 14px; height: 100%; color: var(--text-color); font-weight: 500; font-size: 13px; display: flex; align-items: center; gap: 6px; cursor: default; white-space: nowrap; }\n" +
            ".pill-divider { width: 1px; height: 18px; background-color: rgba(255,255,255,0.15); }\n" +
            ".action-pill-btn { background-color: var(--service-tab-bg, rgba(255,255,255,0.1)); color: var(--text-color, #fff); height: 36px; line-height: 36px; padding: 0 18px; border-radius: 100px; font-size: 13px; font-weight: 500; display: inline-flex; align-items: center; text-decoration: none; flex-shrink: 0; border: none; cursor: pointer; white-space: nowrap; }\n" +
            ".action-pill-btn.danger { background-color: #c00c0c; color: #ffffff; }\n" +
            ".action-pill-btn.disabled { opacity: 0.6; cursor: default; pointer-events: none; }\n" +
            ".settings-card { background-color: var(--settings-card-bg); border-radius: 24px; padding: 24px; border: 1px solid var(--settings-card-border); box-shadow: 0 4px 12px rgba(0,0,0,0.02); min-width: 0; overflow: hidden; word-break: break-word; overflow-wrap: break-word; }\n" +
            ".subscribe-btn:hover { opacity: 0.9; }\n" +
            ".video-js { font-family: inherit; color: #ffffff; border-radius: 16px; overflow: hidden; }\n" +
            ".video-js .vjs-big-play-button { background-color: var(--logo-color); border: none; width: 64px; height: 64px; line-height: 64px; border-radius: 50%; margin-top: -32px; margin-left: -32px; box-shadow: 0 4px 10px rgba(0,0,0,0.3); transition: background-color 0.2s, transform 0.2s; }\n" +
            ".video-js:hover .vjs-big-play-button, .video-js .vjs-big-play-button:focus { background-color: var(--bottom-nav-active-pill-bg); color: var(--bottom-nav-item-active-color); transform: scale(1.1); }\n" +
            ".video-js .vjs-control-bar { background-color: rgba(29, 27, 32, 0.85); backdrop-filter: blur(8px); height: 48px; border-radius: 0 0 16px 16px; }\n" +
            ".video-js .vjs-slider { background-color: rgba(255,255,255,0.2); }\n" +
            ".video-js .vjs-load-progress { background-color: rgba(255,255,255,0.3); }\n" +
            ".video-js .vjs-play-progress { background: var(--logo-color); }\n" +
            ".video-js .vjs-play-progress:before { color: var(--logo-color); font-size: 14px; top: 50% !important; transform: translateY(-50%) !important; }\n" +
            ".video-js.vjs-fullscreen .vjs-control-bar { height: 64px !important; padding: 0 48px !important; font-size: 16px !important; }\n" +
            ".video-js.vjs-fullscreen .vjs-button { font-size: 22px !important; width: 56px !important; }\n" +
            ".video-js.vjs-fullscreen .vjs-time-control { font-size: 14px !important; line-height: 64px !important; }\n" +
            ".channel-card-avatar { width: 80px !important; height: 80px !important; min-width: 80px !important; min-height: 80px !important; border-radius: 50% !important; object-fit: cover !important; aspect-ratio: 1 / 1 !important; flex-shrink: 0; background-color: var(--service-tab-bg, #2b2930); }\n" +
            "body.pip-mode header, body.pip-mode .sidebar-nav, body.pip-mode .bottom-nav, body.pip-mode .media-info, body.pip-mode .comments-section, body.pip-mode .sidebar { display: none !important; }\n" +
            "body.pip-mode .container { margin: 0 !important; padding: 0 !important; max-width: 100% !important; margin-top: 0 !important; }\n" +
            "body.pip-mode .player-container { margin-top: 0 !important; }\n" +
            "body.pip-mode .native-player, body.pip-mode .video-js { height: 100vh !important; width: 100vw !important; border-radius: 0 !important; }\n" +
            ".media-description { font-size: 14px; line-height: 1.5; color: var(--media-desc-color); white-space: pre-wrap; word-break: break-word; overflow-wrap: break-word; min-width: 0; background-color: var(--media-desc-bg); padding: 12px; border-radius: 12px; border: 1px solid var(--media-desc-border); margin-top: 12px; }\n" +
            ".comments-section { padding-top: 16px; min-width: 0; }\n" +
            ".comment-count { font-size: 16px; font-weight: 500; margin-bottom: 16px; color: var(--comment-count-color); }\n" +
            ".comment { display: flex; gap: 12px; margin-bottom: 16px; min-width: 0; }\n" +
            ".comment-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; flex-shrink: 0; aspect-ratio: 1 / 1; background-color: var(--card-thumbnail-bg); }\n" +
            ".comment-details { display: flex; flex-direction: column; gap: 4px; min-width: 0; overflow: hidden; word-break: break-word; overflow-wrap: break-word; }\n" +
            ".comment-header { display: flex; gap: 8px; align-items: center; min-width: 0; }\n" +
            ".comment-author { font-size: 13px; font-weight: 500; color: var(--comment-author-color); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n" +
            ".comment-time { font-size: 12px; color: var(--comment-time-color); flex-shrink: 0; }\n" +
            ".comment-text { font-size: 14px; line-height: 1.4; color: var(--comment-text-color); white-space: pre-wrap; word-break: break-word; overflow-wrap: break-word; min-width: 0; }\n" +
            ".channel-header { background-color: var(--channel-header-bg); border-radius: 12px; overflow: hidden; margin-bottom: 24px; border: 1px solid var(--channel-header-border); min-width: 0; }\n" +
            ".channel-banner { width: 100%; height: 160px; object-fit: cover; background: #272727; }\n" +
            ".channel-details { display: flex; padding: 16px; align-items: center; gap: 16px; flex-wrap: wrap; min-width: 0; }\n" +
            ".channel-avatar { width: 80px; height: 80px; border-radius: 50%; object-fit: cover; flex-shrink: 0; aspect-ratio: 1 / 1; }\n" +
            ".channel-info-block { display: flex; flex-direction: column; gap: 4px; flex-grow: 1; min-width: 0; overflow: hidden; word-break: break-word; overflow-wrap: break-word; }\n" +
            ".channel-name { font-size: 24px; font-weight: 700; color: var(--channel-name-color); word-break: break-word; overflow-wrap: break-word; min-width: 0; }\n" +
            ".channel-desc { font-size: 14px; color: var(--channel-desc-color); max-width: 600px; margin-top: 8px; line-height: 1.4; word-break: break-word; overflow-wrap: break-word; min-width: 0; }\n" +
            ".channel-tabs-selector { display: flex; border-top: 1px solid var(--media-info-border); padding: 0 16px; }\n" +
            ".channel-tab-btn { padding: 12px 16px; font-size: 14px; font-weight: 500; color: var(--card-meta-color); border-bottom: 3px solid transparent; cursor: pointer; text-decoration: none; }\n" +
            ".channel-tab-btn:hover { color: var(--text-color); }\n" +
            ".channel-tab-btn.active { color: var(--text-color); border-bottom-color: #0f0f0f; }\n" +
            "[data-theme=\"dark\"] .channel-tab-btn.active { border-bottom-color: #ffffff; }\n" +
            ".loading-placeholder { text-align: center; font-size: 15px; padding: 48px 16px; color: var(--card-meta-color); background-color: var(--card-bg); border-radius: 16px; border: 1px solid var(--media-info-border); margin: 16px 0; }\n" +
            ".m3-spinner { width: 40px; height: 40px; border: 4px solid var(--search-input-bg); border-top: 4px solid var(--logo-color); border-radius: 50%; animation: m3-spin 0.8s linear infinite; margin: 24px auto; }\n" +
            "@keyframes m3-spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }\n" +
            ".settings-card { background-color: var(--settings-card-bg); padding: 24px; border-radius: 12px; border: 1px solid var(--settings-card-border); max-width: 600px; margin: 0 auto; }\n" +
            ".settings-title { font-size: 20px; font-weight: 700; margin-bottom: 24px; color: var(--settings-title-color); }\n" +
            ".settings-section { margin-bottom: 24px; padding-bottom: 20px; border-bottom: 1px solid var(--settings-section-border); }\n" +
            ".settings-section:last-child { border-bottom: none; }\n" +
            ".settings-section-title { font-size: 16px; font-weight: 500; margin-bottom: 12px; color: var(--settings-section-title-color); }\n" +
            ".setting-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }\n" +
            ".setting-label-group { display: flex; flex-direction: column; gap: 2px; }\n" +
            ".setting-label { font-size: 14px; font-weight: 500; color: var(--setting-label-color); }\n" +
            ".setting-desc { font-size: 12px; color: var(--setting-desc-color); }\n" +
            ".switch { position: relative; display: inline-block; width: 40px; height: 20px; }\n" +
            ".switch input { opacity: 0; width: 0; height: 0; }\n" +
            ".slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: var(--slider-bg); transition: .2s; border-radius: 20px; }\n" +
            ".slider:before { position: absolute; content: ''; height: 14px; width: 14px; left: 3px; bottom: 3px; background-color: white; transition: .2s; border-radius: 50%; }\n" +
            "input:checked + .slider { background-color: #cc0000; }\n" +
            "input:checked + .slider:before { transform: translateX(20px); }\n" +
            ".textarea-group { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }\n" +
            ".textarea-label { font-size: 14px; font-weight: 500; color: var(--textarea-label-color); }\n" +
            ".settings-textarea { width: 100%; height: 100px; padding: 12px; border-radius: 8px; border: 1px solid var(--textarea-border); background-color: var(--textarea-bg); color: var(--textarea-color); font-size: 14px; outline: none; transition: border-color 0.2s; resize: vertical; font-family: inherit; }\n" +
            ".settings-textarea:focus { border-color: #1a73e8; }\n" +
            ".btn-save { display: inline-block; width: 100%; padding: 12px; border-radius: 24px; font-size: 14px; font-weight: 500; text-align: center; border: none; cursor: pointer; transition: background-color 0.2s; }\n" +
            ".btn-save-primary { background-color: #cc0000; color: #ffffff; }\n" +
            ".btn-save-primary:hover { opacity: 0.9; }\n" +
            ".alert-banner { background-color: rgba(43, 138, 62, 0.1); color: #2b8a3e; padding: 12px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; font-weight: 500; border: 1px solid rgba(43, 138, 62, 0.2); }\n" +
            ".theme-toggle-btn { background: none; border: none; font-size: 20px; cursor: pointer; padding: 8px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: var(--text-color); }\n" +
            ".theme-toggle-btn:hover { background-color: var(--service-tab-hover-bg); }\n" +
            "#connect-remote-btn { background: none; border: none; font-size: 20px; cursor: pointer; padding: 8px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: var(--text-color); }\n" +
            "#connect-remote-btn:hover { background-color: var(--service-tab-hover-bg); }\n" +
            ".connect-text { display: none; }\n" +
            "[data-theme=\"dark\"] .theme-icon-light { display: block; }\n" +
            "[data-theme=\"dark\"] .theme-icon-dark { display: none; }\n" +
            ".theme-icon-light { display: none; }\n" +
            ".theme-icon-dark { display: block; }\n" +
            "body.has-banner header { top: 40px; }\n" +
            "body.has-banner .sidebar-nav { top: 96px; }\n" +
            "body.has-banner .container { margin-top: 96px; }\n" +
            "@media (max-width: 768px) {\n" +
            "  body.has-banner .container { margin-top: 96px; }\n" +
            "  #sidebar-toggle-btn { display: none !important; }\n" +
            "}\n" +
            "@media (min-width: 769px) {\n" +
            "  body.sidebar-collapsed .sidebar-nav { width: 72px; }\n" +
            "  body.sidebar-collapsed .sidebar-nav .sidebar-label { display: none; }\n" +
            "  body.sidebar-collapsed .sidebar-nav .sidebar-item { justify-content: center; padding: 12px; }\n" +
            "  body.sidebar-collapsed .container { margin-left: 72px; max-width: calc(100% - 72px); }\n" +
            "}\n" +
            ".sidebar-nav, .container { transition: width 0.2s ease, margin-left 0.2s ease, max-width 0.2s ease; }\n" +
            ".search-suggestions { position: absolute; top: 42px; left: 0; right: 0; background-color: var(--bg-color); border: 1px solid var(--search-input-border); border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); z-index: 10000; display: none; flex-direction: column; padding: 8px 0; max-height: 350px; overflow-y: auto; }\n" +
            ".search-suggestion-item { display: flex; align-items: center; justify-content: space-between; padding: 8px 16px; cursor: pointer; font-size: 14px; color: var(--text-color); }\n" +
            ".search-suggestion-item:hover { background-color: var(--service-tab-hover-bg); }\n" +
            ".search-suggestion-text { display: flex; align-items: center; gap: 12px; flex-grow: 1; }\n" +
            ".search-suggestion-delete { color: #cc0000; font-size: 12px; cursor: pointer; padding: 4px 8px; border-radius: 4px; }\n" +
            ".search-suggestion-delete:hover { background-color: rgba(204,0,0,0.1); }\n" +
            ".vjs-player-wrapper { position: relative; width: 100%; border-radius: 12px; overflow: hidden; background: #000; }\n" +
            ".double-tap-indicator {\n" +
            "  position: absolute;\n" +
            "  top: 0;\n" +
            "  bottom: 0;\n" +
            "  width: 35%;\n" +
            "  display: flex;\n" +
            "  flex-direction: column;\n" +
            "  align-items: center;\n" +
            "  justify-content: center;\n" +
            "  background: rgba(0, 0, 0, 0.4);\n" +
            "  color: #fff;\n" +
            "  opacity: 0;\n" +
            "  pointer-events: none;\n" +
            "  transition: opacity 0.25s ease-in-out;\n" +
            "  z-index: 10;\n" +
            "}\n" +
            ".double-tap-indicator.left { left: 0; border-top-left-radius: 12px; border-bottom-left-radius: 12px; }\n" +
            ".double-tap-indicator.right { right: 0; border-top-right-radius: 12px; border-bottom-right-radius: 12px; }\n" +
            ".double-tap-indicator.show { opacity: 1; }\n" +
            ".double-tap-indicator svg { width: 44px; height: 44px; fill: #fff; animation: bounceGlow 0.5s infinite alternate; }\n" +
            ".double-tap-text { font-size: 14px; font-weight: bold; margin-top: 6px; }\n" +
            "@keyframes bounceGlow {\n" +
            "  0% { transform: scale(1); filter: drop-shadow(0 0 2px rgba(255,255,255,0.6)); }\n" +
            "  100% { transform: scale(1.08); filter: drop-shadow(0 0 8px rgba(255,255,255,0.9)); }\n" +
            "}\n" +
            ".volume-hud {\n" +
            "  position: absolute;\n" +
            "  top: 24px;\n" +
            "  left: 50%;\n" +
            "  transform: translateX(-50%);\n" +
            "  background: rgba(0, 0, 0, 0.8);\n" +
            "  color: #fff;\n" +
            "  padding: 8px 16px;\n" +
            "  border-radius: 20px;\n" +
            "  font-size: 13px;\n" +
            "  font-weight: 500;\n" +
            "  z-index: 15;\n" +
            "  display: flex;\n" +
            "  align-items: center;\n" +
            "  gap: 8px;\n" +
            "  opacity: 0;\n" +
            "  transition: opacity 0.2s ease;\n" +
            "  pointer-events: none;\n" +
            "}\n" +
            ".volume-hud.show { opacity: 1; }\n" +
            ".up-next-overlay {\n" +
            "  position: absolute;\n" +
            "  top: 0; left: 0; right: 0; bottom: 0;\n" +
            "  background: rgba(0,0,0,0.88);\n" +
            "  z-index: 20;\n" +
            "  display: flex;\n" +
            "  flex-direction: column;\n" +
            "  align-items: center;\n" +
            "  justify-content: center;\n" +
            "  color: #fff;\n" +
            "  opacity: 0;\n" +
            "  pointer-events: none;\n" +
            "  transition: opacity 0.3s ease;\n" +
            "}\n" +
            ".up-next-overlay.show { opacity: 1; pointer-events: auto; }\n" +
            ".up-next-title { font-size: 12px; text-transform: uppercase; color: #bbb; letter-spacing: 1.5px; margin-bottom: 6px; }\n" +
            ".up-next-name { font-size: 18px; font-weight: bold; text-align: center; max-width: 80%; margin-bottom: 12px; }\n" +
            ".up-next-thumb { width: 160px; aspect-ratio: 16/9; border-radius: 8px; object-fit: cover; box-shadow: 0 4px 12px rgba(0,0,0,0.5); margin-bottom: 16px; }\n" +
            ".up-next-btn-row { display: flex; gap: 12px; }\n" +
            ".up-next-btn { padding: 8px 20px; border-radius: 20px; border: none; font-weight: 600; cursor: pointer; font-size: 13px; }\n" +
            ".up-next-btn-play { background: #7c3aed; color: #fff; }\n" +
            ".up-next-btn-cancel { background: rgba(255,255,255,0.18); color: #fff; }\n" +
            ".up-next-circle { position: relative; width: 56px; height: 56px; margin-bottom: 12px; }\n" +
            ".up-next-circle svg { transform: rotate(-90deg); }\n" +
            ".up-next-circle circle { fill: none; stroke-width: 4; }\n" +
            ".up-next-circle-bg { stroke: rgba(255,255,255,0.2); }\n" +
            ".up-next-circle-val { stroke: #7c3aed; stroke-dasharray: 138; stroke-dashoffset: 0; transition: stroke-dashoffset 0.1s linear; }\n" +
            "video, .video-js video { object-fit: fill !important; }";

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
        String ytActive = "youtube".equals(activeTab) ? "active" : "";
        String shortsActive = "shorts".equals(activeTab) ? "active" : "";
        String histActive = "history".equals(activeTab) ? "active" : "";
        String cachedActive = "cached".equals(activeTab) ? "active" : "";
        String subsActive = "subscriptions".equals(activeTab) ? "active" : "";
        String settingsActive = "settings".equals(activeTab) ? "active" : "";

            sb.append("<header>\n")
          .append("  <div class=\"top-bar\">\n")
          .append("    <div style=\"display:flex; align-items:center;\">\n")
          .append("      <button id=\"sidebar-toggle-btn\" class=\"theme-toggle-btn\" aria-label=\"Toggle Sidebar\" style=\"margin-right:8px;\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M21 6H3V5h18v1zm0 5H3v1h18v-1zm0 6H3v1h18v-1z\"/></svg></button>\n")
          .append("      <a href=\"/\" class=\"logo\"><svg viewBox=\"0 0 24 24\" width=\"28\" height=\"28\" fill=\"#FF0000\" style=\"display:inline-block; vertical-align:middle; margin-right:6px;\"><path d=\"M23.498 6.163a3.003 3.003 0 0 0-2.11-2.11C19.518 3.545 12 3.545 12 3.545s-7.518 0-9.388.508a3.003 3.003 0 0 0-2.11 2.11C0 8.033 0 12 0 12s0 3.967.502 5.837a3.003 3.003 0 0 0 2.11 2.11c1.87.508 9.388.508 9.388.508s7.518 0 9.388-.508a3.003 3.003 0 0 0 2.11-2.11C24 15.967 24 12 24 12s0-3.967-.502-5.837zM9.545 15.568V8.432L15.818 12l-6.273 3.568z\"/></svg>LocalTube</a>\n")
          .append("    </div>\n")
          .append("    <form action=\"/search\" method=\"GET\" class=\"search-form\">\n")
          .append("      <input type=\"hidden\" name=\"serviceId\" value=\"").append(activeServiceId).append("\">\n")
          .append("      <input type=\"text\" name=\"q\" class=\"search-input\" placeholder=\"Search\" value=\"")
          .append(query != null ? query.replace("\"", "&quot;") : "").append("\" required>\n")
          .append("      <button type=\"submit\" class=\"search-btn\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"20\" height=\"20\"><path d=\"M20.87 20.17l-5.59-5.59C16.31 13.33 17 11.75 17 10c0-3.87-3.13-7-7-7s-7 3.13-7 7 3.13 7 7 7c1.75 0 3.33-.69 4.58-1.72l5.59 5.59.7-.71zM10 16c-3.31 0-6-2.69-6-6s2.69-6 6-6 6 2.69 6 6-2.69 6-6 6z\"/></svg></button>\n")
          .append("      <div class=\"search-suggestions\" id=\"search-suggestions-box\"></div>\n")
          .append("    </form>\n")
          .append("    <div style=\"display:flex; align-items:center; gap:10px;\">\n")
          .append("      <button id=\"connect-remote-btn\" onclick=\"playOnTV(window.location.href, document.title)\" aria-label=\"Connect Remote\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"22\" height=\"22\"><path d=\"M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.9-.9 1.9-2l.01-12c0-1.1-.9-2-2-2zm0 14H3V5h18v12zm-10 7v-2h2v2h-2z\"/></svg></button>\n")
          .append("      <button id=\"theme-toggle\" class=\"theme-toggle-btn\" aria-label=\"Toggle Theme\">\n")
          .append("        <span class=\"theme-icon-light\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"22\" height=\"22\"><path d=\"M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58c-.39-.39-1.03-.39-1.41 0s-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37c-.39-.39-1.03-.39-1.41 0s-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41l-1.06-1.06zm1.06-12.37c-.39-.39-1.02-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06c.38-.38.38-1.02 0-1.41zM6.63 17.65l-1.06 1.06c-.39.39-.39 1.02 0 1.41.39.39 1.02.39 1.41 0l1.06-1.06c.39-.39.39-1.03 0-1.41s-1.02-.39-1.41 0z\"/></svg></span>\n")
          .append("        <span class=\"theme-icon-dark\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"22\" height=\"22\"><path d=\"M12.1 21.4c-4.7 0-8.6-3.9-8.6-8.6 0-3.3 1.9-6.2 4.9-7.5.3-.1.5.1.4.4-.8 2.5-.2 5.2 1.6 7.1s4.6 2.4 7.1 1.6c.3-.1.5.2.4.4-1.3 3-4.2 4.9-7.5 4.9-1.3 0-2.6-.3-3.7-.9.5.4 1 .7 1.6.8.5.1.9.1 1.4 0 3-.5 5.2-3 5.2-6.1V12c.1 4.7-3.8 8.6-8.5 8.6z\"/></svg></span>\n")
          .append("      </button>\n")
          .append("    </div>\n")
          .append("  </div>\n")
          .append("</header>\n");

        sb.append("<div class=\"sidebar-nav\">\n")
          .append("  <a href=\"/\" class=\"sidebar-item ").append(ytActive).append("\">\n")
          .append("    <span class=\"sidebar-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M4 21V10.08l8-6.92 8 6.92V21h-6v-6h-4v6H4z\"/></svg></span>\n")
          .append("    <span class=\"sidebar-label\">Home</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/shorts\" class=\"sidebar-item ").append(shortsActive).append("\">\n")
          .append("    <span class=\"sidebar-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M10 14.65v-5.3L15 12l-5 2.65zM17.77 10.32c-.77-.32-1.2-.5-1.2-.5L18 8.8c1.37-.8 1.83-2.58 1.03-3.95-.8-1.37-2.58-1.83-3.95-1.03l-10.3 6c-1.37.8-1.83 2.58-1.03 3.95.8 1.37 2.58 1.83 3.95 1.03l1.2.5s-.43.18-1.2.5c-1.37.8-1.83 2.58-1.03 3.95.8 1.37 2.58 1.83 3.95 1.03l10.3-6c1.37-.8 1.83-2.58 1.03-3.95-.8-1.37-2.58-1.83-3.95-1.03z\"/></svg></span>\n")
          .append("    <span class=\"sidebar-label\">Reels</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/subscriptions\" class=\"sidebar-item ").append(subsActive).append("\">\n")
          .append("    <span class=\"sidebar-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M10 14.65v-5.3L15 12l-5 2.65zM19 4H5v1h14V4zm2 2H3v1h18V6zm2 2H1v12h22V8z\"/></svg></span>\n")
          .append("    <span class=\"sidebar-label\">Subscriptions</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/history\" class=\"sidebar-item ").append(histActive).append("\">\n")
          .append("    <span class=\"sidebar-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M11.9 21.1c-4.3 0-8-3.1-8.7-7.4-.1-.7-.1-1.4 0-2.1.8-4.4 4.6-7.5 9-7.5H13V2l5.3 4.2-5.3 4.2V8.1H12.2c-3.1 0-5.7 2.2-6.2 5.2-.1.5-.1 1 0 1.5.5 3 3.1 5.2 6.2 5.2 3.5 0 6.3-2.8 6.3-6.3h2c0 4.6-3.7 8.2-8.6 8.2zm-.4-12.6h1v4.8l4.2 2.5-.5.9-4.7-2.8z\"/></svg></span>\n")
          .append("    <span class=\"sidebar-label\">History</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/cache\" class=\"sidebar-item ").append(cachedActive).append("\">\n")
          .append("    <span class=\"sidebar-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M17 18H7v-1h10v1zm-5-3.4l3.1-3.1.7.7-4.3 4.3-4.3-4.3.7-.7 3.1 3.1V5h1v9.6z\"/></svg></span>\n")
          .append("    <span class=\"sidebar-label\">Downloads</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/settings\" class=\"sidebar-item ").append(settingsActive).append("\">\n")
          .append("    <span class=\"sidebar-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M19.4 13c0-.3.1-.6.1-.9s0-.6-.1-.9l2.1-1.7c.2-.2.2-.4.1-.6l-2-3.5c-.1-.2-.4-.3-.6-.2l-2.5 1c-.5-.4-1.1-.7-1.7-.9l-.4-2.6c0-.2-.2-.4-.5-.4h-4c-.3 0-.5.2-.5.4l-.4 2.6c-.6.2-1.2.5-1.7.9l-2.5-1c-.2-.1-.5 0-.6.2l-2 3.5c-.1.2-.1.4.1.6l2.1 1.7c-.1.3-.1.6-.1.9s0 .6.1.9l-2.1 1.7c-.2.2-.2.4-.1.6l2 3.5c.1.2.4.3.6.2l2.5-1c.5.4 1.1.7 1.7.9l.4 2.6c0 .2.2.4.5.4h4c.3 0 .5-.2.5-.4l.4-2.6c.6-.2 1.2-.5 1.7-.9l2.5 1c.2.1.5 0 .6-.2l2-3.5c.1-.2.1-.4-.1-.6l-2.1-1zm-7.4 2.5c-1.9 0-3.5-1.6-3.5-3.5s1.6-3.5 3.5-3.5 3.5 1.6 3.5 3.5-1.6 3.5-3.5 3.5z\"/></svg></span>\n")
          .append("    <span class=\"sidebar-label\">Settings</span>\n")
          .append("  </a>\n")
          .append("</div>\n");

        sb.append("<div class=\"bottom-nav\">\n")
          .append("  <a href=\"/\" class=\"bottom-nav-item ").append(ytActive).append("\">\n")
          .append("    <span class=\"bottom-nav-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M4 21V10.08l8-6.92 8 6.92V21h-6v-6h-4v6H4z\"/></svg></span>\n")
          .append("    <span>Home</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/shorts\" class=\"bottom-nav-item ").append(shortsActive).append("\">\n")
          .append("    <span class=\"bottom-nav-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M10 14.65v-5.3L15 12l-5 2.65zM17.77 10.32c-.77-.32-1.2-.5-1.2-.5L18 8.8c1.37-.8 1.83-2.58 1.03-3.95-.8-1.37-2.58-1.83-3.95-1.03l-10.3 6c-1.37.8-1.83 2.58-1.03 3.95.8 1.37 2.58 1.83 3.95 1.03l1.2.5s-.43.18-1.2.5c-1.37.8-1.83 2.58-1.03 3.95.8 1.37 2.58 1.83 3.95 1.03l10.3-6c1.37-.8 1.83-2.58 1.03-3.95-.8-1.37-2.58-1.83-3.95-1.03z\"/></svg></span>\n")
          .append("    <span>Reels</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/subscriptions\" class=\"bottom-nav-item ").append(subsActive).append("\">\n")
          .append("    <span class=\"bottom-nav-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M10 14.65v-5.3L15 12l-5 2.65zM19 4H5v1h14V4zm2 2H3v1h18V6zm2 2H1v12h22V8z\"/></svg></span>\n")
          .append("    <span>Subscriptions</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/cache\" class=\"bottom-nav-item ").append(cachedActive).append("\">\n")
          .append("    <span class=\"bottom-nav-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M17 18H7v-1h10v1zm-5-3.4l3.1-3.1.7.7-4.3 4.3-4.3-4.3.7-.7 3.1 3.1V5h1v9.6z\"/></svg></span>\n")
          .append("    <span>Downloads</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/settings\" class=\"bottom-nav-item ").append(settingsActive).append("\">\n")
          .append("    <span class=\"bottom-nav-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M19.4 13c0-.3.1-.6.1-.9s0-.6-.1-.9l2.1-1.7c.2-.2.2-.4.1-.6l-2-3.5c-.1-.2-.4-.3-.6-.2l-2.5 1c-.5-.4-1.1-.7-1.7-.9l-.4-2.6c0-.2-.2-.4-.5-.4h-4c-.3 0-.5.2-.5.4l-.4 2.6c-.6.2-1.2.5-1.7.9l-2.5-1c-.2-.1-.5 0-.6.2l-2 3.5c-.1.2-.1.4.1.6l2.1 1.7c-.1.3-.1.6-.1.9s0 .6.1.9l-2.1 1.7c-.2.2-.2.4-.1.6l2 3.5c.1.2.4.3.6.2l2.5-1c.5.4 1.1.7 1.7.9l.4 2.6c0 .2.2.4.5.4h4c.3 0 .5-.2.5-.4l.4-2.6c.6-.2 1.2-.5 1.7-.9l2.5 1c.2.1.5 0 .6-.2l2-3.5c.1-.2.1-.4-.1-.6l-2.1-1zm-7.4 2.5c-1.9 0-3.5-1.6-3.5-3.5s1.6-3.5 3.5-3.5 3.5 1.6 3.5 3.5-1.6 3.5-3.5 3.5z\"/></svg></span>\n")
          .append("    <span>Settings</span>\n")
          .append("  </a>\n")
          .append("</div>\n");

        return sb.toString();
    }

    private static String wrapInTemplate(String title, String bodyContent, boolean isTv) {
        String bodyClass = isTv ? "is-tv" : "is-phone";
        // Base64 encoded red play button icon to avoid external asset load issues
        String favicon = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iI0ZGMDAwMCI+PHBhdGggZD0iTTIzLjQ5OCA2LjE2M2EzLjAwMyAzLjAwMyAwIDAgMC0yLjExLTIuMTFDMTkuNTE4IDMuNTQ1IDEyIDMuNTQ1IDEyIDMuNTQ1cy03LjUxOCAwLTkuMzg4LjUwOGEzLjAwMyAzLjAwMyAwIDAgMC0yLjExIDIuMTFDMCA4LjAzMyAwIDEyIDAgMTJzMCAzLjk2Ny41MDIgNS44MzdhMy4wMDMgMy4wMDMgMCAwIDAgMi4xMSAyLjExYzEuODcuNTA4IDkuMzg4LjUwOCA5LjM4OC41MDhzNy41MTggMCA5LjM4OC0uNTA4YTMuMDAzIDMuMDAzIDAgMCAwIDIuMTEtMi4xMUMyNCAxNS45NjcgMjQgMTIgMjQgMTJzMC0zLjk2Ny0uNTAyLTUuODM3ek05LjU0NSAxNS41NjhWOC40MzJMMTUuODE4IDEybC02LjI3MyAzLjU2OHoiLz48L3N2Zz4=";
        
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + title + "</title>\n" +
                "    <link rel=\"icon\" type=\"image/svg+xml\" href=\"" + favicon + "\">\n" +
                "    <link href=\"https://vjs.zencdn.net/8.10.0/video-js.css\" rel=\"stylesheet\" />\n" +
                "    <script src=\"https://vjs.zencdn.net/8.10.0/video.min.js\"></script>\n" +
                "    <script src=\"https://unpkg.com/videojs-contrib-quality-levels@4.1.0/dist/videojs-contrib-quality-levels.min.js\"></script>\n" +
                "    <style>\n" + CSS + "\n" + getCustomThemeCss() + "\n" +
                "        /* Glow border outline style for selected/focused interactive elements */\n" +
                "        a:focus, button:focus, input:focus, select:focus, textarea:focus, [tabindex=\"0\"]:focus {\n" +
                "            outline: none !important;\n" +
                "            box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.35), 0 0 15px rgba(219, 39, 119, 0.6) !important;\n" +
                "            border-color: #7c3aed !important;\n" +
                "            transition: all 0.2s ease-in-out !important;\n" +
                "        }\n" +
                "        #share-modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.65); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px); z-index: 100000; display: none; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.25s ease; }\n" +
                "        #share-modal-overlay.active { display: flex; opacity: 1; }\n" +
                "        .share-modal-card { background: var(--card-bg, #1c1b1f); color: var(--text-color, #fff); border: 1px solid rgba(255,255,255,0.15); border-radius: 20px; padding: 20px; width: 90%; max-width: 400px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); transform: translateY(20px); transition: transform 0.25s ease; font-family: 'Roboto', sans-serif; }\n" +
                "        #share-modal-overlay.active .share-modal-card { transform: translateY(0); }\n" +
                "        .share-modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }\n" +
                "        .share-modal-title { font-size: 18px; font-weight: 600; }\n" +
                "        .share-modal-close { background: none; border: none; color: currentColor; font-size: 24px; cursor: pointer; opacity: 0.7; line-height: 1; }\n" +
                "        .share-link-box { display: flex; gap: 8px; margin-bottom: 20px; background: rgba(255,255,255,0.06); border-radius: 12px; padding: 4px 6px 4px 12px; border: 1px solid rgba(255,255,255,0.1); align-items: center; }\n" +
                "        .share-link-input { flex: 1; background: none; border: none; color: inherit; font-size: 13px; outline: none; text-overflow: ellipsis; white-space: nowrap; overflow: hidden; }\n" +
                "        .share-copy-btn { background: var(--logo-color, #7c3aed); color: #fff; border: none; border-radius: 8px; padding: 8px 16px; font-size: 13px; font-weight: 600; cursor: pointer; transition: background 0.2s; flex-shrink: 0; }\n" +
                "        .share-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; text-align: center; }\n" +
                "        .share-item { display: flex; flex-direction: column; align-items: center; gap: 6px; text-decoration: none; color: inherit; font-size: 12px; opacity: 0.85; transition: opacity 0.2s, transform 0.2s; }\n" +
                "        .share-item:hover { opacity: 1; transform: translateY(-2px); }\n" +
                "        .share-icon-btn { width: 48px; height: 48px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; }\n" +
                "    </style>\n" +
                "    <script>\n" +
                "        (function() {\n" +
                "            const savedTheme = localStorage.getItem('theme');\n" +
                "            const theme = savedTheme ? savedTheme : (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');\n" +
                "            document.documentElement.setAttribute('data-theme', theme);\n" +
                "            \n" +
                "            const accent = localStorage.getItem('theme-color') || 'system';\n" +
                "            document.documentElement.setAttribute('data-theme-color', accent);\n" +
                "            \n" +
                "            const pureBlack = localStorage.getItem('pure-black') === 'true';\n" +
                "            document.documentElement.setAttribute('data-pure-black', pureBlack);\n" +
                "        })();\n" +
                "    </script>\n" +
                "</head>\n" +
                "<body class=\"" + bodyClass + "\">\n" +
                "    <!-- Virtual Mouse Cursor -->\n" +
                "    <div id=\"vptr\" style=\"position:fixed; width:22px; height:22px; border-radius:50%; pointer-events:none; z-index:2147483647; display:none; transform:translate(-50%,-50%); transition:left 0.04s linear, top 0.04s linear;\">\n" +
                "      <svg width=\"22\" height=\"22\" viewBox=\"0 0 22 22\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                "        <defs><radialGradient id=\"cg\" cx=\"40%\" cy=\"30%\" r=\"70%\"><stop offset=\"0%\" stop-color=\"#e879f9\"/><stop offset=\"100%\" stop-color=\"#7c3aed\"/></radialGradient></defs>\n" +
                "        <circle cx=\"11\" cy=\"11\" r=\"9\" fill=\"url(#cg)\" stroke=\"white\" stroke-width=\"2\"/>\n" +
                "        <circle cx=\"11\" cy=\"11\" r=\"3\" fill=\"white\" fill-opacity=\"0.8\"/>\n" +
                "      </svg>\n" +
                "    </div>\n" +
                "    <div id=\"tv-lock-banner\" style=\"display:none; flex-direction:column; background: linear-gradient(135deg, #7c3aed, #db2777); color: white; padding: 0 24px; position: fixed; top: 0; left: 0; right: 0; height: 40px; justify-content: center; z-index: 10001; box-shadow: 0 4px 15px rgba(124, 58, 237, 0.3);\">\n" +
                "      <div style=\"display:flex; align-items:center; justify-content:space-between; width:100%; font-weight:500; font-size:14px;\">\n" +
                "        <span>📺 Currently controlling TV playback</span>\n" +
                "        <div style=\"display:flex; gap:10px;\">\n" +
                "          <button onclick=\"releaseTVLock()\" style=\"background: rgba(0,0,0,0.2); border: 1px solid rgba(255,255,255,0.2); color: white; padding: 6px 16px; border-radius: 20px; font-family: inherit; font-size: 12px; font-weight: 600; cursor: pointer; transition: background 0.2s;\">Disconnect / Stop</button>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                bodyContent + "\n" +
                "    <script>\n" +
                "        (function() {\n" +
                "            const toggleBtn = document.getElementById('theme-toggle');\n" +
                "            if (toggleBtn) {\n" +
                "                toggleBtn.addEventListener('click', function() {\n" +
                "                    const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';\n" +
                "                    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';\n" +
                "                    document.documentElement.setAttribute('data-theme', newTheme);\n" +
                "                    localStorage.setItem('theme', newTheme);\n" +
                "                });\n" +
                "            }\n" +
                "        })();\n" +
                "        \n" +
                "        function updateTVLockBanner() {\n" +
                "            const banner = document.getElementById('tv-lock-banner');\n" +
                "            if (!banner) return;\n" +
                "            const code = localStorage.getItem('server_play_release_code');\n" +
                "            if (code) {\n" +
                "                banner.style.display = 'flex';\n" +
                "                document.body.classList.add('has-banner');\n" +
                "            } else {\n" +
                "                banner.style.display = 'none';\n" +
                "                document.body.classList.remove('has-banner');\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        function playOnTV(videoUrl, title) {\n" +
                "            const code = localStorage.getItem('server_play_release_code') || '';\n" +
                "            const url = '/send-link?id=' + encodeURIComponent(videoUrl) + \n" +
                "                        '&release_code=' + encodeURIComponent(code) +\n" +
                "                        '&title=' + encodeURIComponent(title);\n" +
                "            \n" +
                "            fetch(url)\n" +
                "                .then(res => res.json())\n" +
                "                .then(data => {\n" +
                "                    if (data.status === 'success') {\n" +
                "                        localStorage.setItem('server_play_release_code', data.release_code);\n" +
                "                        updateTVLockBanner();\n" +
                "                        startCommandPolling();\n" +
                "                        alert('Successfully connected remote!');\n" +
                "                    } else if (data.status === 'busy') {\n" +
                "                        alert('Server is busy: ' + data.message);\n" +
                "                    } else {\n" +
                "                        alert('Error casting video: ' + (data.message || 'Unknown error'));\n" +
                "                    }\n" +
                "                })\n" +
                "                .catch(err => {\n" +
                "                    alert('Connection error: ' + err);\n" +
                "                });\n" +
                "        }\n" +
                "        \n" +
                "        function showToast(msg) {\n" +
                "            let t = document.getElementById('app-toast');\n" +
                "            if (!t) {\n" +
                "                t = document.createElement('div');\n" +
                "                t.id = 'app-toast';\n" +
                "                t.style.cssText = 'position:fixed;bottom:80px;left:50%;transform:translateX(-50%);background:rgba(28,27,31,0.92);color:#e6e1e5;padding:10px 20px;border-radius:24px;font-size:14px;font-weight:500;z-index:999999;transition:opacity 0.3s ease, transform 0.3s ease;pointer-events:none;box-shadow:0 4px 16px rgba(0,0,0,0.4);border:1px solid rgba(255,255,255,0.1);font-family:Roboto,sans-serif;';\n" +
                "                document.body.appendChild(t);\n" +
                "            }\n" +
                "            t.innerText = msg;\n" +
                "            t.style.opacity = '1';\n" +
                "            t.style.transform = 'translateX(-50%) translateY(0)';\n" +
                "            clearTimeout(t._timer);\n" +
                "            t._timer = setTimeout(() => {\n" +
                "                t.style.opacity = '0';\n" +
                "                t.style.transform = 'translateX(-50%) translateY(10px)';\n" +
                "            }, 2200);\n" +
                "        }\n" +
                "        \n" +
                "        function openShareModal(url, title) {\n" +
                "            const fullUrl = (url && url.startsWith('http')) ? url : ('https://www.youtube.com/watch?v=' + (url || ''));\n" +
                "            let overlay = document.getElementById('share-modal-overlay');\n" +
                "            if (!overlay) {\n" +
                "                overlay = document.createElement('div');\n" +
                "                overlay.id = 'share-modal-overlay';\n" +
                "                overlay.onclick = function(e) { if (e.target === overlay) closeShareModal(); };\n" +
                "                overlay.innerHTML = \n" +
                "                    '<div class=\"share-modal-card\">' +\n" +
                "                    '  <div class=\"share-modal-header\">' +\n" +
                "                    '    <span class=\"share-modal-title\">Share</span>' +\n" +
                "                    '    <button class=\"share-modal-close\" onclick=\"closeShareModal()\">&times;</button>' +\n" +
                "                    '  </div>' +\n" +
                "                    '  <div class=\"share-link-box\">' +\n" +
                "                    '    <input type=\"text\" id=\"share-input-url\" class=\"share-link-input\" readonly>' +\n" +
                "                    '    <button id=\"share-copy-btn\" class=\"share-copy-btn\" onclick=\"copyShareInputUrl()\">Copy</button>' +\n" +
                "                    '  </div>' +\n" +
                "                    '  <div class=\"share-grid\">' +\n" +
                "                    '    <a id=\"share-wa\" class=\"share-item\" target=\"_blank\" rel=\"noopener\">' +\n" +
                "                    '      <div class=\"share-icon-btn\" style=\"background:#25D366;\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"22\" height=\"22\"><path d=\"M12.04 2c-5.46 0-9.91 4.45-9.91 9.91 0 1.75.46 3.45 1.32 4.95L2.05 22l5.25-1.38c1.45.79 3.08 1.21 4.74 1.21 5.46 0 9.91-4.45 9.91-9.91 0-2.65-1.03-5.14-2.9-7.01A9.84 9.84 0 0012.04 2zm.01 1.67c4.55 0 8.24 3.69 8.24 8.24 0 2.2-.86 4.27-2.42 5.82a8.19 8.19 0 01-5.82 2.42c-1.47 0-2.91-.39-4.17-1.14l-.3-.18-3.1 1.18 1.18-3.04-.19-.31A8.2 8.2 0 013.8 11.91c0-4.55 3.69-8.24 8.24-8.24z\"/></svg></div>' +\n" +
                "                    '      <span>WhatsApp</span>' +\n" +
                "                    '    </a>' +\n" +
                "                    '    <a id=\"share-tg\" class=\"share-item\" target=\"_blank\" rel=\"noopener\">' +\n" +
                "                    '      <div class=\"share-icon-btn\" style=\"background:#0088cc;\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"22\" height=\"22\"><path d=\"M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69.01-.03.01-.14-.07-.2-.08-.06-.19-.04-.27-.02-.12.02-1.96 1.25-5.54 3.67-.52.36-1 .53-1.42.52-.47-.01-1.37-.26-2.03-.48-.82-.27-1.47-.42-1.42-.88.03-.25.38-.51 1.07-.78 4.18-1.82 6.97-3.02 8.37-3.6 3.98-1.65 4.81-1.94 5.35-1.95.12 0 .38.03.55.17.14.12.18.28.2.46-.01.07.01.25 0 .37z\"/></svg></div>' +\n" +
                "                    '      <span>Telegram</span>' +\n" +
                "                    '    </a>' +\n" +
                "                    '    <a id=\"share-tw\" class=\"share-item\" target=\"_blank\" rel=\"noopener\">' +\n" +
                "                    '      <div class=\"share-icon-btn\" style=\"background:#000000;\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"20\" height=\"20\"><path d=\"M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z\"/></svg></div>' +\n" +
                "                    '      <span>X</span>' +\n" +
                "                    '    </a>' +\n" +
                "                    '    <a id=\"share-em\" class=\"share-item\">' +\n" +
                "                    '      <div class=\"share-icon-btn\" style=\"background:#ea4335;\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"22\" height=\"22\"><path d=\"M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z\"/></svg></div>' +\n" +
                "                    '      <span>Email</span>' +\n" +
                "                    '    </a>' +\n" +
                "                    '  </div>' +\n" +
                "                    '</div>';\n" +
                "                document.body.appendChild(overlay);\n" +
                "            }\n" +
                "            const input = document.getElementById('share-input-url');\n" +
                "            if (input) input.value = fullUrl;\n" +
                "            const copyBtn = document.getElementById('share-copy-btn');\n" +
                "            if (copyBtn) {\n" +
                "                copyBtn.innerText = 'Copy';\n" +
                "                copyBtn.style.background = 'var(--logo-color, #7c3aed)';\n" +
                "            }\n" +
                "            const encUrl = encodeURIComponent(fullUrl);\n" +
                "            const encTitle = encodeURIComponent(title || 'LocalTube Video');\n" +
                "            const wa = document.getElementById('share-wa'); if (wa) wa.href = 'https://api.whatsapp.com/send?text=' + encTitle + '%20' + encUrl;\n" +
                "            const tg = document.getElementById('share-tg'); if (tg) tg.href = 'https://t.me/share/url?url=' + encUrl + '&text=' + encTitle;\n" +
                "            const tw = document.getElementById('share-tw'); if (tw) tw.href = 'https://twitter.com/intent/tweet?text=' + encTitle + '&url=' + encUrl;\n" +
                "            const em = document.getElementById('share-em'); if (em) em.href = 'mailto:?subject=' + encTitle + '&body=' + encUrl;\n" +
                "            const bindClick = (id) => {\n" +
                "                const el = document.getElementById(id);\n" +
                "                if (el) {\n" +
                "                    el.onclick = function(e) {\n" +
                "                        if (window.NewPipeApp && window.NewPipeApp.openExternalUrl) {\n" +
                "                            e.preventDefault();\n" +
                "                            window.NewPipeApp.openExternalUrl(this.href);\n" +
                "                        }\n" +
                "                    };\n" +
                "                }\n" +
                "            };\n" +
                "            bindClick('share-wa'); bindClick('share-tg'); bindClick('share-tw'); bindClick('share-em');\n" +
                "            overlay.classList.add('active');\n" +
                "        }\n" +
                "        \n" +
                "        function closeShareModal() {\n" +
                "            const overlay = document.getElementById('share-modal-overlay');\n" +
                "            if (overlay) overlay.classList.remove('active');\n" +
                "        }\n" +
                "        \n" +
                "        function copyShareInputUrl() {\n" +
                "            const input = document.getElementById('share-input-url');\n" +
                "            const copyBtn = document.getElementById('share-copy-btn');\n" +
                "            if (input && input.value) {\n" +
                "                const markSuccess = () => {\n" +
                "                    if (copyBtn) {\n" +
                "                        copyBtn.innerText = 'Copied!';\n" +
                "                        copyBtn.style.background = '#2e7d32';\n" +
                "                    }\n" +
                "                    showToast('Link copied to clipboard');\n" +
                "                };\n" +
                "                if (navigator.clipboard && navigator.clipboard.writeText) {\n" +
                "                    navigator.clipboard.writeText(input.value).then(markSuccess).catch(() => {\n" +
                "                        input.select();\n" +
                "                        document.execCommand('copy');\n" +
                "                        markSuccess();\n" +
                "                    });\n" +
                "                } else {\n" +
                "                    input.select();\n" +
                "                    document.execCommand('copy');\n" +
                "                    markSuccess();\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        function shareLink(url, title) {\n" +
                "            openShareModal(url, title);\n" +
                "        }\n" +
                "        \n" +
                "        function startCommandPolling() {\n" +
                "            if (window.wsConnection) return;\n" +
                "            \n" +
                "            // Virtual cursor state\n" +
                "            let vptrX = window.innerWidth / 2, vptrY = window.innerHeight / 2;\n" +
                "            const vptr = document.getElementById('vptr');\n" +
                "            function showVptr() {\n" +
                "                if (vptr) { vptr.style.display = 'block'; vptr.style.left = vptrX + 'px'; vptr.style.top = vptrY + 'px'; }\n" +
                "            }\n" +
                "            function moveVptr(dx, dy) {\n" +
                "                vptrX = Math.max(0, Math.min(window.innerWidth, vptrX + dx));\n" +
                "                vptrY = Math.max(0, Math.min(window.innerHeight, vptrY + dy));\n" +
                "                if (vptr) { vptr.style.left = vptrX + 'px'; vptr.style.top = vptrY + 'px'; }\n" +
                "                const el = document.elementFromPoint(vptrX, vptrY);\n" +
                "                if (el) {\n" +
                "                    const isOverPlayer = el.closest('#video-container') !== null;\n" +
                "                    const controls = document.getElementById('video-controls');\n" +
                "                    if (controls) {\n" +
                "                        controls.style.opacity = isOverPlayer ? '1' : '0';\n" +
                "                        controls.style.pointerEvents = isOverPlayer ? 'auto' : 'none';\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "            function clickVptr() {\n" +
                "                if (vptr) vptr.style.transform = 'translate(-50%,-50%) scale(0.7)';\n" +
                "                setTimeout(() => { if (vptr) vptr.style.transform = 'translate(-50%,-50%) scale(1)'; }, 150);\n" +
                "                const el = document.elementFromPoint(vptrX, vptrY);\n" +
                "                if (!el || el === vptr) return;\n" +
                "                el.click();\n" +
                "                const anchor = el.tagName === 'A' ? el : el.closest('a');\n" +
                "                if (anchor && anchor.href && !anchor.href.startsWith('javascript')) {\n" +
                "                    window.location.href = anchor.href;\n" +
                "                }\n" +
                "            }\n" +
                "            \n" +
                "            window.playVideoSPA = function(url) {\n" +
                "                history.pushState(null, '', '/watch?serviceId=0&id=' + encodeURIComponent(url));\n" +
                "                let container = document.querySelector('.container');\n" +
                "                if (container) {\n" +
                "                    container.outerHTML = '<div id=\"watch-container-loader\" style=\"text-align: center; padding: 100px 0; font-family: inherit;\">' +\n" +
                "                      '  <div style=\"display: inline-block; width: 60px; height: 60px; border: 4px solid rgba(124, 58, 237, 0.1); border-top: 4px solid #7c3aed; border-radius: 50%; animation: spin 1.5s linear infinite;\"></div>' +\n" +
                "                      '  <div style=\"margin-top: 24px; font-size: 16px; font-weight: 500; color: var(--text-color);\">Loading video streams...</div>' +\n" +
                "                      '</div>' +\n" +
                "                      '<div id=\"watch-content\" style=\"display: none;\"></div>';\n" +
                "                } else {\n" +
                "                    window.location.href = '/watch?serviceId=0&id=' + encodeURIComponent(url);\n" +
                "                    return;\n" +
                "                }\n" +
                "                if (!document.getElementById('spa-spin-style')) {\n" +
                "                    const style = document.createElement('style');\n" +
                "                    style.id = 'spa-spin-style';\n" +
                "                    style.innerHTML = '@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }';\n" +
                "                    document.head.appendChild(style);\n" +
                "                }\n" +
                "                const loader = document.getElementById('watch-container-loader');\n" +
                "                const content = document.getElementById('watch-content');\n" +
                "                fetch('/watch-content?id=' + encodeURIComponent(url))\n" +
                "                    .then(res => {\n" +
                "                        if (!res.ok) throw new Error('HTTP ' + res.status);\n" +
                "                        return res.text();\n" +
                "                    })\n" +
                "                    .then(html => {\n" +
                "                        if (loader) loader.remove();\n" +
                "                        if (content) {\n" +
                "                            content.style.display = 'block';\n" +
                "                            content.outerHTML = html;\n" +
                "                            const newContainer = document.querySelector('.container');\n" +
                "                            if (newContainer) {\n" +
                "                                newContainer.querySelectorAll('script').forEach(oldScript => {\n" +
                "                                    const newScript = document.createElement('script');\n" +
                "                                    Array.from(oldScript.attributes).forEach(attr => newScript.setAttribute(attr.name, attr.value));\n" +
                "                                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));\n" +
                "                                    oldScript.parentNode.replaceChild(newScript, oldScript);\n" +
                "                                });\n" +
                "                            }\n" +
                "                        }\n" +
                "                    })\n" +
                "                    .catch(err => {\n" +
                "                        if (loader) loader.remove();\n" +
                "                        const errDiv = document.createElement('div');\n" +
                "                        errDiv.className = 'loading-placeholder';\n" +
                "                        errDiv.style.color = '#ff4b5c';\n" +
                "                        errDiv.style.borderColor = 'rgba(255, 75, 92, 0.2)';\n" +
                "                        errDiv.innerText = 'Failed to load video: ' + err.message;\n" +
                "                        document.body.appendChild(errDiv);\n" +
                "                    });\n" +
                "            };\n" +
                "            \n" +
                "            window.wsConnection = new WebSocket('ws://' + location.hostname + ':8081');\n" +
                "            window.wsConnection.onmessage = function(event) {\n" +
                "                const cmd = event.data;\n" +
                "                if (cmd.startsWith('play_video:')) {\n" +
                "                    const url = cmd.substring('play_video:'.length);\n" +
                "                    window.playVideoSPA(url);\n" +
                "                } else if (cmd.startsWith('pointer_move:')) {\n" +
                "                    const parts = cmd.substring('pointer_move:'.length).split(',');\n" +
                "                    const dx = parseFloat(parts[0]) || 0;\n" +
                "                    const dy = parseFloat(parts[1]) || 0;\n" +
                "                    showVptr();\n" +
                "                    moveVptr(dx, dy);\n" +
                "                } else if (cmd === 'pointer_click') {\n" +
                "                    showVptr();\n" +
                "                    clickVptr();\n" +
                "                } else if (cmd.startsWith('pointer_scroll:')) {\n" +
                "                    const dy = parseFloat(cmd.substring('pointer_scroll:'.length)) || 0;\n" +
                "                    window.scrollBy({ top: dy, behavior: 'smooth' });\n" +
                "                } else if (cmd === 'back') {\n" +
                "                    if (window.history.length > 1) window.history.back();\n" +
                "                } else if (cmd === 'play_pause') {\n" +
                "                    if (window.videoPlayer) {\n" +
                "                        if (window.videoPlayer.paused()) {\n" +
                "                            window.videoPlayer.play().catch(e => {});\n" +
                "                        } else {\n" +
                "                            window.videoPlayer.pause();\n" +
                "                        }\n" +
                "                    } else {\n" +
                "                        const media = document.getElementById('player') || document.getElementById('audio-player');\n" +
                "                        if (media) {\n" +
                "                            if (media.readyState < 2) {\n" +
                "                                const loader = document.getElementById('video-loader');\n" +
                "                                if (loader) loader.style.display = 'block';\n" +
                "                                media.play().catch(e => {});\n" +
                "                            } else {\n" +
                "                                if (media.paused) media.play().catch(e => {}); else media.pause();\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                } else if (cmd === 'forward') {\n" +
                "                    if (typeof window.seekVideo === 'function') window.seekVideo(10);\n" +
                "                    else {\n" +
                "                        const media = document.getElementById('player') || document.getElementById('audio-player');\n" +
                "                        if (media) media.currentTime += 10;\n" +
                "                    }\n" +
                "                } else if (cmd === 'rewind') {\n" +
                "                    if (typeof window.seekVideo === 'function') window.seekVideo(-10);\n" +
                "                    else {\n" +
                "                        const media = document.getElementById('player') || document.getElementById('audio-player');\n" +
                "                        if (media) media.currentTime -= 10;\n" +
                "                    }\n" +
                "                }\n" +
                "            };\n" +
                "            window.wsConnection.onclose = function() {\n" +
                "                window.wsConnection = null;\n" +
                "                setTimeout(() => {\n" +
                "                    if (localStorage.getItem('server_play_release_code')) {\n" +
                "                        startCommandPolling();\n" +
                "                    }\n" +
                "                }, 1000);\n" +
                "            };\n" +
                "        }\n" +
                "        \n" +
                "        function releaseTVLock() {\n" +
                "            const code = localStorage.getItem('server_play_release_code');\n" +
                "            if (!code) return;\n" +
                "            \n" +
                "            fetch('/release-lock?release_code=' + encodeURIComponent(code))\n" +
                "                .then(res => res.json())\n" +
                "                .then(data => {\n" +
                "                    localStorage.removeItem('server_play_release_code');\n" +
                "                    updateTVLockBanner();\n" +
                "                    if (window.wsConnection) {\n" +
                "                        window.wsConnection.close();\n" +
                "                        window.wsConnection = null;\n" +
                "                    }\n" +
                "                })\n" +
                "                .catch(err => {\n" +
                "                    localStorage.removeItem('server_play_release_code');\n" +
                "                    updateTVLockBanner();\n" +
                "                    if (window.wsConnection) {\n" +
                "                        window.wsConnection.close();\n" +
                "                        window.wsConnection = null;\n" +
                "                    }\n" +
                "                    alert('Connection error/released locally: ' + err);\n" +
                "                });\n" +
                "        }\n" +
                "        \n" +
                "        document.addEventListener('DOMContentLoaded', () => {\n" +
                "            updateTVLockBanner();\n" +
                "            \n" +
                "            window.getFocusableElements = () => {\n" +
                "                const selector = 'a, button, input, select, textarea, [tabindex=\"0\"]';\n" +
                "                return Array.from(document.querySelectorAll(selector)).filter(el => {\n" +
                "                    const rect = el.getBoundingClientRect();\n" +
                "                    return rect.width > 0 && rect.height > 0 && \n" +
                "                           window.getComputedStyle(el).display !== 'none' &&\n" +
                "                           window.getComputedStyle(el).visibility !== 'hidden';\n" +
                "                });\n" +
                "            };\n" +
                "            \n" +
                "            window.focusNext = (reverse = false) => {\n" +
                "                const els = window.getFocusableElements();\n" +
                "                if (els.length === 0) return;\n" +
                "                const active = document.activeElement;\n" +
                "                let idx = els.indexOf(active);\n" +
                "                if (idx === -1) {\n" +
                "                    els[0].focus();\n" +
                "                    return;\n" +
                "                }\n" +
                "                if (reverse) {\n" +
                "                    idx = (idx - 1 + els.length) % els.length;\n" +
                "                } else {\n" +
                "                    idx = (idx + 1) % els.length;\n" +
                "                }\n" +
                "                els[idx].focus();\n" +
                "                els[idx].scrollIntoView({ behavior: 'smooth', block: 'center' });\n" +
                "            };\n" +
                "            \n" +
                "            window.focusVertical = (down = true) => {\n" +
                "                const els = window.getFocusableElements();\n" +
                "                if (els.length === 0) return;\n" +
                "                const active = document.activeElement;\n" +
                "                let idx = els.indexOf(active);\n" +
                "                if (idx === -1) {\n" +
                "                    els[0].focus();\n" +
                "                    return;\n" +
                "                }\n" +
                "                \n" +
                "                let cols = 1;\n" +
                "                const firstRect = els[0].getBoundingClientRect();\n" +
                "                for (let i = 1; i < els.length; i++) {\n" +
                "                    const r = els[i].getBoundingClientRect();\n" +
                "                    if (Math.abs(r.top - firstRect.top) < 15) {\n" +
                "                        cols++;\n" +
                "                    } else {\n" +
                "                        break;\n" +
                "                    }\n" +
                "                }\n" +
                "                \n" +
                "                const step = down ? cols : -cols;\n" +
                "                let newIdx = idx + step;\n" +
                "                if (newIdx < 0) newIdx = 0;\n" +
                "                if (newIdx >= els.length) newIdx = els.length - 1;\n" +
                "                \n" +
                "                els[newIdx].focus();\n" +
                "                els[newIdx].scrollIntoView({ behavior: 'smooth', block: 'center' });\n" +
                "            };\n" +
                "            \n" +
                "            const initFocusable = () => {\n" +
                "                document.querySelectorAll('.card').forEach(card => {\n" +
                "                    if (!card.hasAttribute('tabindex')) {\n" +
                "                        card.setAttribute('tabindex', '0');\n" +
                "                    }\n" +
                "                });\n" +
                "            };\n" +
                "            initFocusable();\n" +
                "            \n" +
                "            const defaultFocus = () => {\n" +
                "                const els = window.getFocusableElements ? window.getFocusableElements() : [];\n" +
                "                if (els.length > 0) {\n" +
                "                    els[0].focus();\n" +
                "                }\n" +
                "            };\n" +
                "            setTimeout(defaultFocus, 200);\n" +
                "            \n" +
                "            // Global keyboard keydown handler to replace Arrow keys with Tab / Shift-Tab linear focus cycle\n" +
                "            document.addEventListener('keydown', (e) => {\n" +
                "                const active = document.activeElement;\n" +
                "                if (active && (active.tagName === 'INPUT' || active.tagName === 'SELECT' || active.tagName === 'TEXTAREA' || active.isContentEditable)) {\n" +
                "                    return; // Let native typing handle arrows inside inputs\n" +
                "                }\n" +
                "                if (e.key === 'ArrowLeft') {\n" +
                "                    e.preventDefault();\n" +
                "                    const player = document.getElementById('player') || document.getElementById('audio-player');\n" +
                "                    if (player) {\n" +
                "                        if (typeof window.seekVideo === 'function') window.seekVideo(-10);\n" +
                "                        else player.currentTime = Math.max(0, player.currentTime - 10);\n" +
                "                    } else {\n" +
                "                        if (window.focusNext) window.focusNext(true);\n" +
                "                    }\n" +
                "                } else if (e.key === 'ArrowRight') {\n" +
                "                    e.preventDefault();\n" +
                "                    const player = document.getElementById('player') || document.getElementById('audio-player');\n" +
                "                    if (player) {\n" +
                "                        if (typeof window.seekVideo === 'function') window.seekVideo(10);\n" +
                "                        else player.currentTime = Math.min(player.duration || 0, player.currentTime + 10);\n" +
                "                    } else {\n" +
                "                        if (window.focusNext) window.focusNext(false);\n" +
                "                    }\n" +
                "                } else if (e.key === 'ArrowUp') {\n" +
                "                    e.preventDefault();\n" +
                "                    if (window.focusVertical) window.focusVertical(false);\n" +
                "                } else if (e.key === 'ArrowDown') {\n" +
                "                    e.preventDefault();\n" +
                "                    if (window.focusVertical) window.focusVertical(true);\n" +
                "                } else if (e.key === 'Enter') {\n" +
                "                    if (active) {\n" +
                "                        active.click();\n" +
                "                        const anchor = active.tagName === 'A' ? active : active.querySelector('a');\n" +
                "                        if (anchor && anchor.href) {\n" +
                "                            window.location.href = anchor.href;\n" +
                "                        }\n" +
                "                    }\n" +
                "                } else if (e.key === 'Backspace' || e.key === 'Escape') {\n" +
                "                    if (window.history.length > 1) {\n" +
                "                        e.preventDefault();\n" +
                "                        window.history.back();\n" +
                "                    }\n" +
                "                }\n" +
                "            });\n" +
                "            \n" +
                "            // Resume command polling if we are currently connected/locked\n" +
                "            if (localStorage.getItem('server_play_release_code')) {\n" +
                "                startCommandPolling();\n" +
                "            }\n" +
                "            \n" +
                "            const sidebarToggle = document.getElementById('sidebar-toggle-btn');\n" +
                "            if (sidebarToggle) {\n" +
                "                sidebarToggle.addEventListener('click', () => {\n" +
                "                    document.body.classList.toggle('sidebar-collapsed');\n" +
                "                    localStorage.setItem('sidebar-collapsed', document.body.classList.contains('sidebar-collapsed'));\n" +
                "                });\n" +
                "            }\n" +
                "            if (localStorage.getItem('sidebar-collapsed') === 'true') {\n" +
                "                document.body.classList.add('sidebar-collapsed');\n" +
                "            }\n" +
                "            \n" +
                "            const cacheBtn = document.getElementById('cache-btn');\n" +
                "            if (cacheBtn && cacheBtn.textContent.toLowerCase().includes('downloading')) {\n" +
                "                const videoUrl = cacheBtn.getAttribute('data-url');\n" +
                "                if (videoUrl) {\n" +
                "                    pollDownloadProgress(videoUrl, cacheBtn);\n" +
                "                }\n" +
                "            }\n" +
                "            \n" +
                "            const activeStatusLabels = document.querySelectorAll('.cache-status-label');\n" +
                "            activeStatusLabels.forEach(label => {\n" +
                "                const status = label.getAttribute('data-status');\n" +
                "                if (status === 'DOWNLOADING' || status === 'PENDING') {\n" +
                "                    const videoUrl = label.getAttribute('data-url');\n" +
                "                    pollListDownloadProgress(videoUrl, label);\n" +
                "                }\n" +
                "            });\n" +
                "            \n" +
                "            const settingsThemeToggle = document.getElementById('settings-theme-toggle');\n" +
                "            if (settingsThemeToggle) {\n" +
                "                const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';\n" +
                "                settingsThemeToggle.checked = (currentTheme === 'dark');\n" +
                "                settingsThemeToggle.addEventListener('change', function() {\n" +
                "                    const newTheme = settingsThemeToggle.checked ? 'dark' : 'light';\n" +
                "                    document.documentElement.setAttribute('data-theme', newTheme);\n" +
                "                    localStorage.setItem('theme', newTheme);\n" +
                "                });\n" +
                "            }\n" +
                "            \n" +
                "            const settingsAccentSelect = document.getElementById('settings-accent-select');\n" +
                "            if (settingsAccentSelect) {\n" +
                "                const currentAccent = localStorage.getItem('theme-color') || 'system';\n" +
                "                settingsAccentSelect.value = currentAccent;\n" +
                "                settingsAccentSelect.addEventListener('change', function() {\n" +
                "                    const newAccent = settingsAccentSelect.value;\n" +
                "                    document.documentElement.setAttribute('data-theme-color', newAccent);\n" +
                "                    localStorage.setItem('theme-color', newAccent);\n" +
                "                });\n" +
                "            }\n" +
                "            \n" +
                "            const settingsPureBlackToggle = document.getElementById('settings-pureblack-toggle');\n" +
                "            if (settingsPureBlackToggle) {\n" +
                "                settingsPureBlackToggle.checked = (localStorage.getItem('pure-black') === 'true');\n" +
                "                settingsPureBlackToggle.addEventListener('change', function() {\n" +
                "                    const isPureBlack = settingsPureBlackToggle.checked;\n" +
                "                    document.documentElement.setAttribute('data-pure-black', isPureBlack);\n" +
                "                    localStorage.setItem('pure-black', isPureBlack ? 'true' : 'false');\n" +
                "                });\n" +
                "            }\n" +
                "            \n" +
                "            const settingsAudioOnlyToggle = document.getElementById('settings-audio-only-toggle');\n" +
                "            if (settingsAudioOnlyToggle) {\n" +
                "                settingsAudioOnlyToggle.checked = (localStorage.getItem('audio_only_default') === 'true');\n" +
                "                settingsAudioOnlyToggle.addEventListener('change', function() {\n" +
                "                    localStorage.setItem('audio_only_default', settingsAudioOnlyToggle.checked ? 'true' : 'false');\n" +
                "                });\n" +
                "            }\n" +
                "            \n" +
                "            document.addEventListener('click', function(e) {\n" +
                "                const target = e.target.closest('a');\n" +
                "                if (target && target.href) {\n" +
                "                    const urlStr = target.href;\n" +
                "                    if (urlStr.indexOf('/watch?') !== -1 && urlStr.indexOf('force_video=true') === -1) {\n" +
                "                        if (localStorage.getItem('audio_only_default') === 'true') {\n" +
                "                            e.preventDefault();\n" +
                "                            try {\n" +
                "                                const url = new URL(urlStr);\n" +
                "                                url.pathname = '/audio';\n" +
                "                                window.location.href = url.toString();\n" +
                "                            } catch (err) {\n" +
                "                                window.location.href = urlStr.replace('/watch?', '/audio?');\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            });\n" +
                "            \n" +
                "            const searchForm = document.querySelector('.search-form');\n" +
                "            const searchInput = document.querySelector('.search-input');\n" +
                "            const searchBtn = document.querySelector('.search-btn');\n" +
                "            const topBar = document.querySelector('.top-bar');\n" +
                "            const suggestionsBox = document.getElementById('search-suggestions-box');\n" +
                "            \n" +
                "            if (searchForm && searchInput && searchBtn) {\n" +
                "                if (window.innerWidth <= 768) {\n" +
                "                    searchBtn.addEventListener('click', function(e) {\n" +
                "                        if (!searchForm.classList.contains('search-active')) {\n" +
                "                            e.preventDefault();\n" +
                "                            searchForm.classList.add('search-active');\n" +
                "                            if (topBar) topBar.classList.add('search-active');\n" +
                "                            searchInput.focus();\n" +
                "                            window.history.pushState({ searchActive: true }, '');\n" +
                "                        }\n" +
                "                    });\n" +
                "                    \n" +
                "                    window.addEventListener('popstate', function(e) {\n" +
                "                        if (searchForm.classList.contains('search-active')) {\n" +
                "                            searchForm.classList.remove('search-active');\n" +
                "                            if (topBar) topBar.classList.remove('search-active');\n" +
                "                            if (suggestionsBox) suggestionsBox.style.display = 'none';\n" +
                "                        }\n" +
                "                    });\n" +
                "                    \n" +
                "                    searchInput.addEventListener('blur', function() {\n" +
                "                        setTimeout(() => {\n" +
                "                            if (document.activeElement !== searchInput && searchInput.value.trim() === '') {\n" +
                "                                searchForm.classList.remove('search-active');\n" +
                "                                if (topBar) topBar.classList.remove('search-active');\n" +
                "                                if (window.history.state && window.history.state.searchActive) {\n" +
                "                                    window.history.back();\n" +
                "                                }\n" +
                "                            }\n" +
                "                        }, 250);\n" +
                "                    });\n" +
                "                    \n" +
                "                    document.addEventListener('click', function(e) {\n" +
                "                        if (!searchForm.contains(e.target) && searchForm.classList.contains('search-active')) {\n" +
                "                            if (searchInput.value.trim() === '') {\n" +
                "                                searchForm.classList.remove('search-active');\n" +
                "                                if (topBar) topBar.classList.remove('search-active');\n" +
                "                                if (window.history.state && window.history.state.searchActive) {\n" +
                "                                    window.history.back();\n" +
                "                                }\n" +
                "                            }\n" +
                "                        }\n" +
                "                    });\n" +
                "                }\n" +
                "                \n" +
                "                if (suggestionsBox) {\n" +
                "                    searchInput.addEventListener('focus', showSuggestions);\n" +
                "                    searchInput.addEventListener('input', showSuggestions);\n" +
                "                    searchInput.addEventListener('click', showSuggestions);\n" +
                "                    \n" +
                "                    document.addEventListener('click', function(e) {\n" +
                "                        if (!searchForm.contains(e.target)) {\n" +
                "                            suggestionsBox.style.display = 'none';\n" +
                "                        }\n" +
                "                    });\n" +
                "                    \n" +
                "                    function showSuggestions() {\n" +
                "                        fetch('/search-history')\n" +
                "                            .then(res => res.json())\n" +
                "                            .then(data => {\n" +
                "                                if (data && data.length > 0) {\n" +
                "                                    const filterVal = searchInput.value.toLowerCase().trim();\n" +
                "                                    const filtered = filterVal ? data.filter(q => q.toLowerCase().includes(filterVal)) : data;\n" +
                "                                    if (filtered.length === 0) {\n" +
                "                                        suggestionsBox.style.display = 'none';\n" +
                "                                        return;\n" +
                "                                    }\n" +
                "                                    suggestionsBox.innerHTML = '';\n" +
                "                                    filtered.forEach(q => {\n" +
                "                                        const item = document.createElement('div');\n" +
                "                                        item.className = 'search-suggestion-item';\n" +
                "                                        \n" +
                "                                        const textDiv = document.createElement('div');\n" +
                "                                        textDiv.className = 'search-suggestion-text';\n" +
                "                                        textDiv.innerHTML = `<svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"18\" height=\"18\" style=\"color:var(--card-meta-color);\"><path d=\"M11.9 21.1c-4.3 0-8-3.1-8.7-7.4-.1-.7-.1-1.4 0-2.1.8-4.4 4.6-7.5 9-7.5H13V2l5.3 4.2-5.3 4.2V8.1H12.2c-3.1 0-5.7 2.2-6.2 5.2-.1.5-.1 1 0 1.5.5 3 3.1 5.2 6.2 5.2 3.5 0 6.3-2.8 6.3-6.3h2c0 4.6-3.7 8.2-8.6 8.2zm-.4-12.6h1v4.8l4.2 2.5-.5.9-4.7-2.8z\"/></svg><span>${q}</span>`;\n" +
                "                                        \n" +
                "                                        textDiv.addEventListener('mousedown', (e) => {\n" +
                "                                            e.preventDefault();\n" +
                "                                            searchInput.value = q;\n" +
                "                                            searchForm.submit();\n" +
                "                                        });\n" +
                "                                        \n" +
                "                                        const delBtn = document.createElement('span');\n" +
                "                                        delBtn.className = 'search-suggestion-delete';\n" +
                "                                        delBtn.textContent = 'Remove';\n" +
                "                                        delBtn.addEventListener('mousedown', (e) => {\n" +
                "                                            e.preventDefault();\n" +
                "                                            e.stopPropagation();\n" +
                "                                            fetch('/search-history?delete=' + encodeURIComponent(q))\n" +
                "                                                .then(() => showSuggestions());\n" +
                "                                        });\n" +
                "                                        \n" +
                "                                        item.appendChild(textDiv);\n" +
                "                                        item.appendChild(delBtn);\n" +
                "                                        suggestionsBox.appendChild(item);\n" +
                "                                    });\n" +
                "                                    suggestionsBox.style.display = 'flex';\n" +
                "                                } else {\n" +
                "                                    suggestionsBox.style.display = 'none';\n" +
                "                                }\n" +
                "                            });\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        });\n" +
                "        \n" +
                "        function toggleSubscribe(event, btn, uploaderUrl, name, avatar) {\n" +
                "            event.preventDefault();\n" +
                "            const isSub = btn.classList.contains('subscribed');\n" +
                "            const action = isSub ? 'unsubscribe' : 'subscribe';\n" +
                "            const url = '/subscribe?action=' + action + '&id=' + encodeURIComponent(uploaderUrl) + '&name=' + encodeURIComponent(name) + '&avatar=' + encodeURIComponent(avatar) + '&back=ajax';\n" +
                "            \n" +
                "            if (isSub) {\n" +
                "                btn.classList.remove('subscribed');\n" +
                "                btn.textContent = 'Subscribe';\n" +
                "            } else {\n" +
                "                btn.classList.add('subscribed');\n" +
                "                btn.textContent = 'Subscribed';\n" +
                "            }\n" +
                "            \n" +
                "            fetch(url).catch(() => {\n" +
                "                if (isSub) {\n" +
                "                    btn.classList.add('subscribed');\n" +
                "                    btn.textContent = 'Subscribed';\n" +
                "                } else {\n" +
                "                    btn.classList.remove('subscribed');\n" +
                "                    btn.textContent = 'Subscribe';\n" +
                "                }\n" +
                "            });\n" +
                "        }\n" +
                "        \n" +
                "        function toggleCache(event, btn, videoUrl, serviceId) {\n" +
                "            event.preventDefault();\n" +
                "            const text = btn.textContent.toLowerCase();\n" +
                "            let action = 'add';\n" +
                "            if (text.includes('download') && !text.includes('delete') && !text.includes('downloading')) {\n" +
                "                action = 'add';\n" +
                "                btn.style.backgroundColor = 'var(--service-tab-bg)';\n" +
                "                btn.style.color = 'var(--text-color)';\n" +
                "                btn.textContent = 'Downloading (0%)';\n" +
                "                btn.style.pointerEvents = 'none';\n" +
                "            } else if (text.includes('delete')) {\n" +
                "                action = 'delete';\n" +
                "                btn.textContent = 'Deleting...';\n" +
                "                btn.style.pointerEvents = 'none';\n" +
                "            }\n" +
                "            \n" +
                "            const url = '/cache?action=' + action + '&id=' + encodeURIComponent(videoUrl) + '&serviceId=' + serviceId + '&back=ajax';\n" +
                "            fetch(url).then(res => {\n" +
                "                if (action === 'add') {\n" +
                "                    pollDownloadProgress(videoUrl, btn);\n" +
                "                } else if (action === 'delete') {\n" +
                "                    const card = btn.closest('.card');\n" +
                "                    if (card) {\n" +
                "                        card.style.transition = 'opacity 0.3s ease, transform 0.3s ease';\n" +
                "                        card.style.opacity = '0';\n" +
                "                        card.style.transform = 'scale(0.9)';\n" +
                "                        setTimeout(() => card.remove(), 300);\n" +
                "                    }\n" +
                "                }\n" +
                "            }).catch(() => {\n" +
                "                btn.style.pointerEvents = '';\n" +
                "                if (action === 'delete') {\n" +
                "                    btn.textContent = 'Delete';\n" +
                "                }\n" +
                "            });\n" +
                "        }\n" +
                "        \n" +
                "        function pollDownloadProgress(videoUrl, btn) {\n" +
                "            const interval = setInterval(() => {\n" +
                "                fetch('/cache-status?id=' + encodeURIComponent(videoUrl))\n" +
                "                    .then(res => res.json())\n" +
                "                    .then(data => {\n" +
                "                        if (data.status === 'COMPLETED') {\n" +
                "                            clearInterval(interval);\n" +
                "                            btn.textContent = 'Delete Download';\n" +
                "                            btn.style.backgroundColor = '#c00c0c';\n" +
                "                            btn.style.color = '#ffffff';\n" +
                "                            btn.style.pointerEvents = '';\n" +
                "                        } else if (data.status === 'DOWNLOADING' || data.status === 'PENDING') {\n" +
                "                            btn.textContent = 'Downloading (' + data.progress + '%)';\n" +
                "                        } else if (data.status === 'FAILED') {\n" +
                "                            clearInterval(interval);\n" +
                "                            btn.textContent = 'Retry Download';\n" +
                "                            btn.style.backgroundColor = '#c00c0c';\n" +
                "                            btn.style.color = '#ffffff';\n" +
                "                            btn.style.pointerEvents = '';\n" +
                "                        }\n" +
                "                    })\n" +
                "                    .catch(() => clearInterval(interval));\n" +
                "            }, 2000);\n" +
                "        }\n" +
                "        \n" +
                "        function pollListDownloadProgress(videoUrl, label) {\n" +
                "            const card = label.closest('.card');\n" +
                "            const deleteBtn = card ? card.querySelector('.cache-delete-btn') : null;\n" +
                "            const interval = setInterval(() => {\n" +
                "                fetch('/cache-status?id=' + encodeURIComponent(videoUrl))\n" +
                "                    .then(res => res.json())\n" +
                "                    .then(data => {\n" +
                "                        if (data.status === 'COMPLETED') {\n" +
                "                            clearInterval(interval);\n" +
                "                            label.textContent = '✅ Saved Offline';\n" +
                "                            label.style.color = '#2b8a3e';\n" +
                "                            label.setAttribute('data-status', 'COMPLETED');\n" +
                "                        } else if (data.status === 'DOWNLOADING' || data.status === 'PENDING') {\n" +
                "                            label.textContent = '⏳ Caching (' + data.progress + '%)';\n" +
                "                            label.style.color = '#e67e22';\n" +
                "                            label.setAttribute('data-status', data.status);\n" +
                "                        } else if (data.status === 'FAILED') {\n" +
                "                            clearInterval(interval);\n" +
                "                            label.textContent = '❌ Failed';\n" +
                "                            label.style.color = '#c0392b';\n" +
                "                            label.setAttribute('data-status', 'FAILED');\n" +
                "                        }\n" +
                "                    })\n" +
                "                    .catch(() => clearInterval(interval));\n" +
                "            }, 2000);\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    public static String renderHomeSkeleton(int serviceId, boolean isTv) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, ""));
        sb.append("<div class=\"container\">\n")
          .append("  <div id=\"home-feed-loader\" style=\"text-align: center; padding: 100px 0;\">\n")
          .append("    <div style=\"display: inline-block; width: 50px; height: 50px; border: 4px solid var(--search-input-bg); border-top: 4px solid var(--logo-color); border-radius: 50%; animation: spin 0.8s linear infinite;\"></div>\n")
          .append("    <div style=\"margin-top: 24px; font-size: 16px; font-weight: 500; color: var(--text-color);\">Loading Home Feed...</div>\n")
          .append("  </div>\n")
          .append("  <div id=\"home-feed-content\" style=\"display: none;\"></div>\n")
          .append("</div>\n")
          .append("<style>\n")
          .append("  @keyframes spin {\n")
          .append("    0% { transform: rotate(0deg); }\n")
          .append("    100% { transform: rotate(360deg); }\n")
          .append("  }\n")
          .append("</style>\n")
          .append("<script>\n")
          .append("  document.addEventListener('DOMContentLoaded', () => {\n")
          .append("      const urlParams = new URLSearchParams(window.location.search);\n")
          .append("      const nextPage = urlParams.get('nextPage') || '';\n")
          .append("      let url = '/?feed=ajax&serviceId=' + ").append(serviceId).append(";\n")
          .append("      if (nextPage) url += '&nextPage=' + encodeURIComponent(nextPage);\n")
          .append("      \n")
          .append("      fetch(url)\n")
          .append("          .then(res => res.text())\n")
          .append("          .then(html => {\n")
          .append("              const loader = document.getElementById('home-feed-loader');\n")
          .append("              const content = document.getElementById('home-feed-content');\n")
          .append("              if (content) {\n")
          .append("                  content.innerHTML = html;\n")
          .append("                  content.style.display = 'block';\n")
          .append("              }\n")
          .append("              if (loader) loader.style.display = 'none';\n")
          .append("          })\n")
          .append("          .catch(err => {\n")
          .append("              const loader = document.getElementById('home-feed-loader');\n")
          .append("              if (loader) loader.innerHTML = '<div class=\"loading-placeholder\" style=\"color: #ff4b5c; border-color: rgba(255, 75, 92, 0.2);\">Failed to load home feed: ' + err.message + '</div>';\n")
          .append("          });\n")
          .append("  });\n")
          .append("</script>\n");
        return wrapInTemplate(SERVICE_NAMES[serviceId] + " - LocalTube", sb.toString(), isTv);
    }

    public static String renderHomeFeed(int serviceId, List<InfoItem> items, Page nextPage) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <h2 style=\"margin-bottom: 20px; font-weight: 700;\">🔥 Trending</h2>\n");
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
        return sb.toString();
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

    public static String renderWatchLater(int serviceId, List<InfoItem> items, boolean isTv) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, "", "watch-later"));
        sb.append("<div class=\"container\">\n")
          .append("  <h2 style=\"margin-bottom: 20px; font-weight: 700;\">⭐ Watch Later</h2>\n");

        if (items == null || items.isEmpty()) {
            sb.append("<div class=\"loading-placeholder\">No videos in Watch Later list. Browse videos and click \"Watch Later\" to add them!</div>\n");
        } else {
            renderGrid(sb, serviceId, items);
        }

        sb.append("</div>\n");
        return wrapInTemplate("Watch Later - LocalTube", sb.toString(), isTv);
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

    public static String renderWatchSkeleton(int serviceId, String mediaUrl, boolean isTv) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, ""));
        sb.append("  <div id=\"watch-container-loader\" style=\"text-align: center; padding: 100px 0; font-family: inherit;\">\n")
          .append("    <div style=\"display: inline-block; width: 50px; height: 50px; border: 4px solid var(--search-input-bg); border-top: 4px solid var(--logo-color); border-radius: 50%; animation: spin 0.8s linear infinite;\"></div>\n")
          .append("    <div style=\"margin-top: 24px; font-size: 16px; font-weight: 500; color: var(--text-color);\">Loading video streams...</div>\n")
          .append("  </div>\n")
          .append("  <div id=\"watch-content\" style=\"display: none;\"></div>\n")
          .append("<style>\n")
          .append("  @keyframes spin {\n")
          .append("    0% { transform: rotate(0deg); }\n")
          .append("    100% { transform: rotate(360deg); }\n")
          .append("  }\n")
          .append("</style>\n")
          .append("<script>\n")
          .append("  function loadWatchContent(url) {\n")
          .append("      const loader = document.getElementById('watch-container-loader');\n")
          .append("      const content = document.getElementById('watch-content');\n")
          .append("      if (loader) loader.style.display = 'block';\n")
          .append("      if (content) content.style.display = 'none';\n")
          .append("      \n")
          .append("      fetch('/watch-content?id=' + encodeURIComponent(url))\n")
          .append("          .then(res => {\n")
          .append("              if (!res.ok) throw new Error('HTTP ' + res.status);\n")
          .append("              return res.text();\n")
          .append("          })\n")
          .append("          .then(html => {\n")
          .append("              if (loader) loader.style.display = 'none';\n")
          .append("              if (content) {\n")
          .append("                  content.style.display = 'block';\n")
          .append("                  content.innerHTML = html;\n")
          .append("                  \n")
          .append("                  // Execute scripts inside the loaded content\n")
          .append("                  content.querySelectorAll('script').forEach(oldScript => {\n")
          .append("                      const newScript = document.createElement('script');\n")
          .append("                      Array.from(oldScript.attributes).forEach(attr => newScript.setAttribute(attr.name, attr.value));\n")
          .append("                      newScript.appendChild(document.createTextNode(oldScript.innerHTML));\n")
          .append("                      oldScript.parentNode.replaceChild(newScript, oldScript);\n")
          .append("                  });\n")
          .append("              }\n")
          .append("          })\n")
          .append("          .catch(err => {\n")
          .append("              if (loader) loader.style.display = 'none';\n")
          .append("              if (content) {\n")
          .append("                  content.style.display = 'block';\n")
          .append("                  content.innerHTML = '<div class=\"loading-placeholder\" style=\"color: #ff4b5c; border-color: rgba(255, 75, 92, 0.2);\">Failed to load video: ' + err.message + '</div>';\n")
          .append("              }\n")
          .append("          });\n")
          .append("  }\n")
          .append("  \n")
          .append("  window.loadNewVideo = function(url) {\n")
          .append("      history.pushState(null, '', '/watch?serviceId=0&id=' + encodeURIComponent(url));\n")
          .append("      loadWatchContent(url);\n")
          .append("  };\n")
          .append("  \n")
          .append("  document.addEventListener('DOMContentLoaded', () => {\n")
          .append("      const urlParams = new URLSearchParams(window.location.search);\n")
          .append("      const id = urlParams.get('id');\n")
          .append("      if (id) loadWatchContent(id);\n")
          .append("  });\n")
          .append("</script>\n");

        return wrapInTemplate("Loading video...", sb.toString(), isTv);
    }

    public static String renderWatchContent(int serviceId, StreamInfo info, CachedVideo cachedVideo, boolean isSubscribed, boolean isTv, String targetQuality, long duration) {
        StringBuilder sb = new StringBuilder();
        String nextVideoUrl = "";
        String nextVideoTitle = "";
        String nextVideoThumb = "";
        if (info.getRelatedItems() != null && !info.getRelatedItems().isEmpty()) {
            InfoItem nextItem = info.getRelatedItems().get(0);
            nextVideoUrl = "/watch?serviceId=" + serviceId + "&id=" + nextItem.getUrl();
            nextVideoTitle = nextItem.getName();
            nextVideoThumb = getThumbnailUrl(nextItem.getThumbnails());
        }
        
        String advancedJs = 
            "                (function() {\n" +
            "                    const nextUrl = \"" + escapeJs(nextVideoUrl) + "\";\n" +
            "                    const nextTitle = \"" + escapeJs(nextVideoTitle) + "\";\n" +
            "                    const nextThumb = \"" + escapeJs(nextVideoThumb) + "\";\n" +
            "                    \n" +
            "                    const wrapper = player.el();\n" +
            "                    player.ready(() => {\n" +
            "                        const el = player.el();\n" +
            "                        const leftTap = document.getElementById(\"double-tap-left\");\n" +
            "                        const rightTap = document.getElementById(\"double-tap-right\");\n" +
            "                        const volumeHud = document.getElementById(\"volume-hud-indicator\");\n" +
            "                        const autoplayOverlay = document.getElementById(\"autoplay-overlay\");\n" +
            "                        if (leftTap) el.appendChild(leftTap);\n" +
            "                        if (rightTap) el.appendChild(rightTap);\n" +
            "                        if (volumeHud) el.appendChild(volumeHud);\n" +
            "                        if (autoplayOverlay) el.appendChild(autoplayOverlay);\n" +
            "                    });\n" +
            "                    \n" +
            "                    // Double tap & Double click to seek\n" +
            "                    if (wrapper) {\n" +
            "                        let lastTap = 0;\n" +
            "                        wrapper.addEventListener(\"touchstart\", function(e) {\n" +
            "                            const now = Date.now();\n" +
            "                            const DOUBLE_PRESS_DELAY = 300;\n" +
            "                            if (now - lastTap < DOUBLE_PRESS_DELAY) {\n" +
            "                                e.preventDefault();\n" +
            "                                const rect = wrapper.getBoundingClientRect();\n" +
            "                                const touchX = e.touches[0].clientX - rect.left;\n" +
            "                                const isLeft = touchX < rect.width * 0.4;\n" +
            "                                const isRight = touchX > rect.width * 0.6;\n" +
            "                                if (isLeft) {\n" +
            "                                    window.seekVideo(-10);\n" +
            "                                    showDoubleTapRipple(\"left\");\n" +
            "                                } else if (isRight) {\n" +
            "                                    window.seekVideo(10);\n" +
            "                                    showDoubleTapRipple(\"right\");\n" +
            "                                }\n" +
            "                            }\n" +
            "                            lastTap = now;\n" +
            "                        }, { passive: false });\n" +
            "                        \n" +
            "                        wrapper.addEventListener(\"dblclick\", function(e) {\n" +
            "                            e.preventDefault();\n" +
            "                            const rect = wrapper.getBoundingClientRect();\n" +
            "                            const clickX = e.clientX - rect.left;\n" +
            "                            const isLeft = clickX < rect.width * 0.4;\n" +
            "                            const isRight = clickX > rect.width * 0.6;\n" +
            "                            if (isLeft) {\n" +
            "                                window.seekVideo(-10);\n" +
            "                                showDoubleTapRipple(\"left\");\n" +
            "                            } else if (isRight) {\n" +
            "                                window.seekVideo(10);\n" +
            "                                showDoubleTapRipple(\"right\");\n" +
            "                            }\n" +
            "                        });\n" +
            "                    }\n" +
            "                    \n" +
            "                    function showDoubleTapRipple(side) {\n" +
            "                        const ind = document.getElementById(\"double-tap-\" + side);\n" +
            "                        if (ind) {\n" +
            "                            ind.classList.add(\"show\");\n" +
            "                            setTimeout(() => ind.classList.remove(\"show\"), 650);\n" +
            "                        }\n" +
            "                    }\n" +
            "                    \n" +
            "                    // Swipe vertically on right side to adjust volume\n" +
            "                    if (wrapper) {\n" +
            "                        let touchStartY = 0;\n" +
            "                        let initialVolume = 1;\n" +
            "                        let isSwipeActive = false;\n" +
            "                        \n" +
            "                        wrapper.addEventListener(\"touchstart\", function(e) {\n" +
            "                            if (e.touches.length === 1) {\n" +
            "                                const rect = wrapper.getBoundingClientRect();\n" +
            "                                const touchX = e.touches[0].clientX - rect.left;\n" +
            "                                if (touchX > rect.width * 0.5) {\n" +
            "                                    touchStartY = e.touches[0].clientY;\n" +
            "                                    initialVolume = player.volume();\n" +
            "                                    isSwipeActive = true;\n" +
            "                                }\n" +
            "                            }\n" +
            "                        }, { passive: true });\n" +
            "                        \n" +
            "                        wrapper.addEventListener(\"touchmove\", function(e) {\n" +
            "                            if (isSwipeActive && e.touches.length === 1) {\n" +
            "                                e.preventDefault();\n" +
            "                                const deltaY = touchStartY - e.touches[0].clientY;\n" +
            "                                const rect = wrapper.getBoundingClientRect();\n" +
            "                                const volumeChange = deltaY / (rect.height * 0.8);\n" +
            "                                const newVolume = Math.max(0, Math.min(1, initialVolume + volumeChange));\n" +
            "                                player.volume(newVolume);\n" +
            "                                showVolumeHUD(Math.round(newVolume * 100));\n" +
            "                            }\n" +
            "                        }, { passive: false });\n" +
            "                        \n" +
            "                        wrapper.addEventListener(\"touchend\", function() {\n" +
            "                            isSwipeActive = false;\n" +
            "                        });\n" +
            "                    }\n" +
            "                    \n" +
            "                    let volumeHudTimeout = null;\n" +
            "                    function showVolumeHUD(volumePercent) {\n" +
            "                        const hud = document.getElementById(\"volume-hud-indicator\");\n" +
            "                        const text = document.getElementById(\"volume-hud-text\");\n" +
            "                        const icon = document.getElementById(\"volume-hud-icon\");\n" +
            "                        if (hud && text && icon) {\n" +
            "                            text.innerText = volumePercent + \"%\";\n" +
            "                            if (volumePercent === 0) icon.innerText = \"🔇\";\n" +
            "                            else if (volumePercent < 30) icon.innerText = \"🔈\";\n" +
            "                            else if (volumePercent < 70) icon.innerText = \"🔉\";\n" +
            "                            else icon.innerText = \"🔊\";\n" +
            "                            hud.classList.add(\"show\");\n" +
            "                            clearTimeout(volumeHudTimeout);\n" +
            "                            volumeHudTimeout = setTimeout(() => hud.classList.remove(\"show\"), 1000);\n" +
            "                        }\n" +
            "                    }\n" +
            "                    \n" +
            "                    // Autoplay Queue\n" +
            "                    let autoplayTimer = null;\n" +
            "                    let autoplayInterval = null;\n" +
            "                    player.on(\"ended\", function() {\n" +
            "                        if (!nextUrl) return;\n" +
            "                        const overlay = document.getElementById(\"autoplay-overlay\");\n" +
            "                        const titleEl = document.getElementById(\"autoplay-next-title\");\n" +
            "                        const thumbEl = document.getElementById(\"autoplay-next-thumb\");\n" +
            "                        const progressCircle = document.getElementById(\"autoplay-progress-circle\");\n" +
            "                        \n" +
            "                        if (overlay && titleEl && thumbEl && progressCircle) {\n" +
            "                            titleEl.innerText = nextTitle;\n" +
            "                            thumbEl.src = nextThumb;\n" +
            "                            overlay.classList.add(\"show\");\n" +
            "                            \n" +
            "                            const totalDash = 138;\n" +
            "                            progressCircle.style.strokeDashoffset = 0;\n" +
            "                            \n" +
            "                            autoplayTimer = setTimeout(() => {\n" +
            "                                window.location.href = nextUrl;\n" +
            "                            }, 5000);\n" +
            "                            \n" +
            "                            let elapsed = 0;\n" +
            "                            autoplayInterval = setInterval(() => {\n" +
            "                                elapsed += 100;\n" +
            "                                const progress = elapsed / 5000;\n" +
            "                                progressCircle.style.strokeDashoffset = totalDash * progress;\n" +
            "                            }, 100);\n" +
            "                        }\n" +
            "                    });\n" +
            "                    \n" +
            "                    function clearAutoplay() {\n" +
            "                        clearTimeout(autoplayTimer);\n" +
            "                        clearInterval(autoplayInterval);\n" +
            "                        const overlay = document.getElementById(\"autoplay-overlay\");\n" +
            "                        if (overlay) overlay.classList.remove(\"show\");\n" +
            "                    }\n" +
            "                    \n" +
            "                    const cancelBtn = document.getElementById(\"autoplay-cancel\");\n" +
            "                    if (cancelBtn) cancelBtn.addEventListener(\"click\", clearAutoplay);\n" +
            "                    \n" +
            "                    const playNowBtn = document.getElementById(\"autoplay-play-now\");\n" +
            "                    if (playNowBtn) {\n" +
            "                        playNowBtn.addEventListener(\"click\", () => {\n" +
            "                            if (nextUrl) window.location.href = nextUrl;\n" +
            "                        });\n" +
            "                    }\n" +
            "                    \n" +
            "                    // Keyboard Shortcuts\n" +
            "                    document.addEventListener(\"keydown\", (e) => {\n" +
            "                        const active = document.activeElement;\n" +
            "                        if (active && (active.tagName === \"INPUT\" || active.tagName === \"SELECT\" || active.tagName === \"TEXTAREA\" || active.isContentEditable)) {\n" +
            "                            return;\n" +
            "                        }\n" +
            "                        if (e.key === \" \" || e.key === \"k\" || e.key === \"K\") {\n" +
            "                            e.preventDefault();\n" +
            "                            if (player.paused()) player.play().catch(e => {}); else player.pause();\n" +
            "                        } else if (e.key === \"j\" || e.key === \"J\") {\n" +
            "                            e.preventDefault();\n" +
            "                            window.seekVideo(-10);\n" +
            "                            showDoubleTapRipple(\"left\");\n" +
            "                        } else if (e.key === \"l\" || e.key === \"L\") {\n" +
            "                            e.preventDefault();\n" +
            "                            window.seekVideo(10);\n" +
            "                            showDoubleTapRipple(\"right\");\n" +
            "                        } else if (e.key === \"m\" || e.key === \"M\") {\n" +
            "                            e.preventDefault();\n" +
            "                            player.muted(!player.muted());\n" +
            "                            showVolumeHUD(player.muted() ? 0 : Math.round(player.volume() * 100));\n" +
            "                        }\n" +
            "                    });\n" +
            "                    \n" +
            "                    // Picture-in-Picture Control\n" +
            "                    try {\n" +
            "                        const Button = videojs.getComponent(\"Button\");\n" +
            "                        const PipButton = videojs.extend(Button, {\n" +
            "                            constructor: function() {\n" +
            "                                Button.apply(this, arguments);\n" +
            "                                this.controlText(\"Picture-in-Picture\");\n" +
            "                            },\n" +
            "                            createEl: function() {\n" +
            "                                return videojs.dom.createEl(\"button\", {\n" +
            "                                    className: \"vjs-pip-control vjs-control vjs-button\",\n" +
            "                                    innerHTML: '<span aria-hidden=\"true\" class=\"vjs-icon-placeholder\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" style=\"width:18px;height:18px;vertical-align:middle;margin-top:6px;\"><path d=\"M19 11h-8v6h8v-6zm4 8V4.98C23 3.88 22.1 3 21 3H3c-1.1 0-2 .88-2 1.98V19c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2zm-2 .02H3V4.97h18v14.05z\"/></svg></span>',\n" +
            "                                    type: \"button\"\n" +
            "                                });\n" +
            "                            },\n" +
            "                            handleClick: function() {\n" +
            "                                const video = document.querySelector(\"#player_html5_api\") || document.querySelector(\"video\");\n" +
            "                                if (video) {\n" +
            "                                    if (document.pictureInPictureElement) {\n" +
            "                                        document.exitPictureInPicture().catch(e => {});\n" +
            "                                    } else {\n" +
            "                                        video.requestPictureInPicture().catch(e => {});\n" +
            "                                    }\n" +
            "                                }\n" +
            "                            }\n" +
            "                        });\n" +
            "                        videojs.registerComponent(\"PipButton\", PipButton);\n" +
            "                        player.ready(() => {\n" +
            "                            player.getChild(\"controlBar\").addChild(\"PipButton\", {}, player.getChild(\"controlBar\").children().length - 1);\n" +
            "                        });\n" +
            "                    } catch(e) { console.error(e); }\n" +
            "                })();\n";

        sb.append("<script>document.title = \"").append(escapeJs(info.getName())).append(" - LocalTube\";</script>\n");
        sb.append("<div class=\"container\">\n")
          .append("  <div class=\"player-container\">\n")
          .append("    <div class=\"player-layout\">\n")
          .append("      <div class=\"main-content\">\n");

        boolean hasVideo = !info.getVideoStreams().isEmpty() || !info.getVideoOnlyStreams().isEmpty() || (info.getHlsUrl() != null && !info.getHlsUrl().isEmpty());
        if (hasVideo) {
            boolean isCached = cachedVideo != null && "COMPLETED".equals(cachedVideo.getStatus());
            String defaultQuality = targetQuality != null ? targetQuality : "720p";
            
            List<SubtitlesStream> subtitles = info.getSubtitles();
            StringBuilder trackTags = new StringBuilder();
            if (subtitles != null) {
                for (SubtitlesStream sub : subtitles) {
                    String lang = sub.getLanguageTag();
                    String label = sub.getDisplayLanguageName();
                    boolean isAuto = sub.isAutoGenerated();
                    trackTags.append("            <track kind=\"captions\" src=\"/subtitles?serviceId=").append(serviceId)
                             .append("&id=").append(encodeUrl(info.getUrl()))
                             .append("&lang=").append(lang)
                             .append("&auto=").append(isAuto)
                             .append("\" srclang=\"").append(lang)
                             .append("\" label=\"").append(escapeJs(label)).append("\">\n");
                }
            }

            if (isCached) {
                sb.append("        <div class=\"vjs-player-wrapper\">\n")
                  .append("          <video id=\"player\" class=\"video-js vjs-default-skin vjs-big-play-centered\" controls autoplay preload=\"auto\" style=\"width:100%; height:auto; aspect-ratio:16/9; display:block;\" poster=\"/thumbnail?id=").append(encodeUrl(info.getUrl())).append("\">\n")
                  .append("            <source src=\"/stream?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUrl())).append("\" type=\"video/mp4\">\n")
                  .append(trackTags.toString())
                  .append("          </video>\n")
                  .append("          <div class=\"double-tap-indicator left\" id=\"double-tap-left\">\n")
                  .append("            <svg viewBox=\"0 0 24 24\"><path d=\"M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z\"/></svg>\n")
                  .append("            <div class=\"double-tap-text\">-10s</div>\n")
                  .append("          </div>\n")
                  .append("          <div class=\"double-tap-indicator right\" id=\"double-tap-right\">\n")
                  .append("            <svg viewBox=\"0 0 24 24\"><path d=\"M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z\"/></svg>\n")
                  .append("            <div class=\"double-tap-text\">+10s</div>\n")
                  .append("          </div>\n")
                  .append("          <div class=\"volume-hud\" id=\"volume-hud-indicator\">\n")
                  .append("            <span id=\"volume-hud-icon\">🔊</span>\n")
                  .append("            <span id=\"volume-hud-text\">100%</span>\n")
                  .append("          </div>\n")
                  .append("          <div class=\"up-next-overlay\" id=\"autoplay-overlay\">\n")
                  .append("            <div class=\"up-next-title\">Up Next</div>\n")
                  .append("            <div class=\"up-next-name\" id=\"autoplay-next-title\"></div>\n")
                  .append("            <img class=\"up-next-thumb\" id=\"autoplay-next-thumb\" src=\"\" alt=\"\">\n")
                  .append("            <div class=\"up-next-circle\">\n")
                  .append("              <svg width=\"56\" height=\"56\">\n")
                  .append("                <circle cx=\"28\" cy=\"28\" r=\"22\" class=\"up-next-circle-bg\" />\n" )
                  .append("                <circle cx=\"28\" cy=\"28\" r=\"22\" class=\"up-next-circle-val\" id=\"autoplay-progress-circle\" />\n")
                  .append("              </svg>\n")
                  .append("            </div>\n")
                  .append("            <div class=\"up-next-btn-row\">\n")
                  .append("              <button class=\"up-next-btn up-next-btn-play\" id=\"autoplay-play-now\">Play Now</button>\n")
                  .append("              <button class=\"up-next-btn up-next-btn-cancel\" id=\"autoplay-cancel\">Cancel</button>\n")
                  .append("            </div>\n")
                  .append("          </div>\n")
                  .append("        </div>\n");

                sb.append("        <script>\n")
                  .append("            (function() {\n")
                  .append("                const player = videojs('player', {\n")
                  .append("                    playbackRates: [0.5, 1, 1.25, 1.5, 2]\n")
                  .append("                });\n")
                  .append("                window.videoPlayer = player;\n")
                  .append("                player.on('fullscreenchange', () => {\n")
                  .append("                    if (player.isFullscreen()) {\n")
                  .append("                        if (screen.orientation && screen.orientation.lock) {\n")
                  .append("                            screen.orientation.lock('landscape').catch(e => {});\n")
                  .append("                        }\n")
                  .append("                    } else {\n")
                  .append("                        if (screen.orientation && screen.orientation.unlock) {\n")
                  .append("                            screen.orientation.unlock();\n")
                  .append("                        }\n")
                  .append("                    }\n")
                  .append("                });\n")
                  .append("                player.ready(() => {\n")
                  .append("                    player.play().catch(err => console.error(err));\n")
                  .append("                });\n")
                  .append("                window.seekVideo = (delta) => {\n")
                  .append("                    player.currentTime(Math.max(0, Math.min(player.duration() || 0, player.currentTime() + delta)));\n")
                  .append("                };\n")
                  .append(advancedJs)
                  .append("            })();\n")
                  .append("        </script>\n");
            } else {
                sb.append("        <div class=\"vjs-player-wrapper\">\n")
                  .append("          <video id=\"player\" class=\"video-js vjs-default-skin vjs-big-play-centered\" controls autoplay preload=\"auto\" style=\"width:100%; height:auto; aspect-ratio:16/9; display:block;\">\n")
                  .append("            <source src=\"/manifest?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUrl())).append("\" type=\"application/dash+xml\">\n")
                  .append(trackTags.toString())
                  .append("            Your browser does not support HTML5 video.\n")
                  .append("          </video>\n")
                  .append("          <div class=\"double-tap-indicator left\" id=\"double-tap-left\">\n")
                  .append("            <svg viewBox=\"0 0 24 24\"><path d=\"M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z\"/></svg>\n")
                  .append("            <div class=\"double-tap-text\">-10s</div>\n")
                  .append("          </div>\n")
                  .append("          <div class=\"double-tap-indicator right\" id=\"double-tap-right\">\n")
                  .append("            <svg viewBox=\"0 0 24 24\"><path d=\"M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z\"/></svg>\n")
                  .append("            <div class=\"double-tap-text\">+10s</div>\n")
                  .append("          </div>\n")
                  .append("          <div class=\"volume-hud\" id=\"volume-hud-indicator\">\n")
                  .append("            <span id=\"volume-hud-icon\">🔊</span>\n")
                  .append("            <span id=\"volume-hud-text\">100%</span>\n")
                  .append("          </div>\n")
                  .append("          <div class=\"up-next-overlay\" id=\"autoplay-overlay\">\n")
                  .append("            <div class=\"up-next-title\">Up Next</div>\n")
                  .append("            <div class=\"up-next-name\" id=\"autoplay-next-title\"></div>\n")
                  .append("            <img class=\"up-next-thumb\" id=\"autoplay-next-thumb\" src=\"\" alt=\"\">\n")
                  .append("            <div class=\"up-next-circle\">\n")
                  .append("              <svg width=\"56\" height=\"56\">\n")
                  .append("                <circle cx=\"28\" cy=\"28\" r=\"22\" class=\"up-next-circle-bg\" />\n" )
                  .append("                <circle cx=\"28\" cy=\"28\" r=\"22\" class=\"up-next-circle-val\" id=\"autoplay-progress-circle\" />\n")
                  .append("              </svg>\n")
                  .append("            </div>\n")
                  .append("            <div class=\"up-next-btn-row\">\n")
                  .append("              <button class=\"up-next-btn up-next-btn-play\" id=\"autoplay-play-now\">Play Now</button>\n")
                  .append("              <button class=\"up-next-btn up-next-btn-cancel\" id=\"autoplay-cancel\">Cancel</button>\n")
                  .append("            </div>\n")
                  .append("          </div>\n")
                  .append("        </div>\n");

                // Serialize available audio tracks for JavaScript dropdown rendering
                org.schabi.newpipe.extractor.stream.AudioStream defaultAudio = null;
                List<org.schabi.newpipe.extractor.stream.AudioStream> audioStreams = info.getAudioStreams();
                if (audioStreams != null && !audioStreams.isEmpty()) {
                    List<org.schabi.newpipe.extractor.stream.AudioStream> m4aStreams = audioStreams.stream()
                            .filter(as -> as.getFormat() == org.schabi.newpipe.extractor.MediaFormat.M4A)
                            .collect(java.util.stream.Collectors.toList());
                    if (m4aStreams.isEmpty()) {
                        m4aStreams = new java.util.ArrayList<>(audioStreams);
                    }
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
                        return 0;
                    });
                    defaultAudio = m4aStreams.get(0);
                }

                String defaultTrackId = defaultAudio != null ? defaultAudio.getAudioTrackId() : "";
                if (defaultTrackId == null) {
                    defaultTrackId = "";
                }

                StringBuilder tracksJson = new StringBuilder("[");
                if (audioStreams != null) {
                    boolean first = true;
                    java.util.Set<String> processedTrackIds = new java.util.HashSet<>();
                    for (org.schabi.newpipe.extractor.stream.AudioStream stream : audioStreams) {
                        String trackId = stream.getAudioTrackId();
                        if (trackId == null) trackId = "";
                        if (processedTrackIds.contains(trackId)) {
                            continue;
                        }
                        processedTrackIds.add(trackId);

                        String label = stream.getAudioTrackName();
                        if (label == null || label.isEmpty()) {
                            java.util.Locale locale = stream.getAudioLocale();
                            label = locale != null ? locale.getDisplayName() : "Audio Track";
                        }
                        org.schabi.newpipe.extractor.stream.AudioTrackType type = stream.getAudioTrackType();
                        if (type != null) {
                            label += " (" + type.name() + ")";
                        }

                        if (!first) tracksJson.append(",");
                        first = false;
                        tracksJson.append("{\"id\":\"").append(escapeJs(trackId))
                                  .append("\",\"label\":\"").append(escapeJs(label)).append("\"}");
                    }
                }
                tracksJson.append("]");

                // Generate quality selector and audio track selector HTML options
                sb.append("        <div class=\"player-controls-row\" style=\"display: flex; gap: 15px; margin-top: 10px; margin-bottom: 15px; align-items: center; justify-content: flex-start; flex-wrap: wrap;\">\n")
                  .append("          <div style=\"display: flex; align-items: center; gap: 8px;\">\n")
                  .append("            <label for=\"quality-select\" style=\"font-size: 13px; font-weight: 500; color: var(--text-color); opacity: 0.8;\">Quality:</label>\n")
                  .append("            <select id=\"quality-select\" style=\"padding: 6px 12px; border-radius: 6px; border: 1px solid var(--search-input-border); background-color: var(--card-bg); color: var(--text-color); font-family: inherit; font-size: 13px; outline: none; cursor: pointer;\">\n")
                  .append("              <option value=\"auto\" selected>Auto</option>\n");

                // List available qualities
                java.util.Set<String> addedQualities = new java.util.HashSet<>();
                // Check video-only streams (HD)
                if (info.getVideoOnlyStreams() != null) {
                    for (VideoStream vs : info.getVideoOnlyStreams()) {
                        String res = vs.getResolution();
                        if (res != null && !addedQualities.contains(res)) {
                            addedQualities.add(res);
                            sb.append("              <option value=\"").append(res).append("\">").append(res).append("</option>\n");
                        }
                    }
                }
                // Check progressive streams (SD)
                if (info.getVideoStreams() != null) {
                    for (VideoStream vs : info.getVideoStreams()) {
                        String res = vs.getResolution();
                        if (res != null && !addedQualities.contains(res)) {
                            addedQualities.add(res);
                            sb.append("              <option value=\"").append(res).append("\">").append(res).append("</option>\n");
                        }
                    }
                }
                sb.append("            </select>\n")
                  .append("          </div>\n")
                  .append("          <div id=\"audio-track-container\" style=\"display: flex; align-items: center; gap: 8px;\">\n")
                  .append("            <label for=\"audio-track-select\" style=\"font-size: 13px; font-weight: 500; color: var(--text-color); opacity: 0.8;\">Audio Language:</label>\n")
                  .append("            <select id=\"audio-track-select\" style=\"padding: 6px 12px; border-radius: 6px; border: 1px solid var(--search-input-border); background-color: var(--card-bg); color: var(--text-color); font-family: inherit; font-size: 13px; outline: none; cursor: pointer;\">\n")
                  .append("            </select>\n")
                  .append("          </div>\n")
                  .append("        </div>\n");

                // Script for player quality switching and remote commands
                sb.append("        <script>\n")
                  .append("            window.availableAudioTracks = ").append(tracksJson.toString()).append(";\n")
                  .append("            window.defaultAudioTrackId = '").append(escapeJs(defaultTrackId)).append("';\n")
                  .append("            (function() {\n")
                  .append("                const player = videojs('player', {\n")
                  .append("                    playbackRates: [0.5, 1, 1.25, 1.5, 2],\n")
                  .append("                    controlBar: { audioTrackButton: false }\n")
                  .append("                });\n")
                  .append("                window.videoPlayer = player;\n")
                  .append("                player.on('fullscreenchange', () => {\n")
                  .append("                    if (player.isFullscreen()) {\n")
                  .append("                        if (screen.orientation && screen.orientation.lock) {\n")
                  .append("                            screen.orientation.lock('landscape').catch(e => {});\n")
                  .append("                        }\n")
                  .append("                    } else {\n")
                  .append("                        if (screen.orientation && screen.orientation.unlock) {\n")
                  .append("                            screen.orientation.unlock();\n")
                  .append("                        }\n")
                  .append("                    }\n")
                  .append("                });\n")
                  .append("                player.ready(() => {\n")
                  .append("                    player.play().catch(err => console.error(err));\n")
                  .append("                });\n")
                  .append("                \n")
                  .append("                const selector = document.getElementById('quality-select');\n")
                  .append("                const streamDuration = ").append(duration).append(";\n")
                  .append("                \n")
                  .append("                // Extract initial start time if available\n")
                  .append("                const urlParams = new URLSearchParams(window.location.search);\n")
                  .append("                let initialStartTime = parseFloat(urlParams.get('start_time')) || 0;\n")
                  .append("                if (initialStartTime > 0) {\n")
                  .append("                    player.ready(() => {\n")
                  .append("                        player.currentTime(initialStartTime);\n")
                  .append("                    });\n")
                  .append("                }\n")
                  .append("                \n")
                  .append("                window.seekVideo = (delta) => {\n")
                  .append("                    const targetTime = Math.max(0, Math.min(streamDuration || player.duration() || 0, player.currentTime() + delta));\n")
                  .append("                    player.currentTime(targetTime);\n")
                  .append("                };\n")
                  .append("                \n")
                  .append("                if (selector) {\n")
                  .append("                    selector.addEventListener('change', () => {\n")
                  .append("                        const targetQuality = selector.value;\n")
                  .append("                        const qualityLevels = player.qualityLevels();\n")
                  .append("                        if (!qualityLevels) return;\n")
                  .append("                        \n")
                  .append("                        if (targetQuality === 'auto') {\n")
                  .append("                            for (let i = 0; i < qualityLevels.length; i++) {\n")
                  .append("                                qualityLevels[i].enabled = true;\n")
                  .append("                            }\n")
                  .append("                        } else {\n")
                  .append("                            const targetHeight = parseInt(targetQuality);\n")
                  .append("                            for (let i = 0; i < qualityLevels.length; i++) {\n")
                  .append("                                const level = qualityLevels[i];\n")
                  .append("                                if (level.height === targetHeight) {\n")
                  .append("                                    level.enabled = true;\n")
                  .append("                                } else {\n")
                  .append("                                    level.enabled = false;\n")
                  .append("                                }\n")
                  .append("                            }\n")
                  .append("                        }\n")
                  .append("                    });\n")
                  .append("                }\n")
                  .append("                \n")
                  .append("                const audioSelect = document.getElementById('audio-track-select');\n")
                  .append("                if (audioSelect && window.availableAudioTracks) {\n")
                  .append("                    audioSelect.innerHTML = '';\n")
                  .append("                    const urlParams = new URLSearchParams(window.location.search);\n")
                  .append("                    let currentAudioTrack = urlParams.get('audio_track');\n")
                  .append("                    if (currentAudioTrack === null) {\n")
                  .append("                        currentAudioTrack = window.defaultAudioTrackId || '';\n")
                  .append("                    }\n")
                  .append("                    window.availableAudioTracks.forEach(track => {\n")
                  .append("                        const option = document.createElement('option');\n")
                  .append("                        option.value = track.id;\n")
                  .append("                        option.text = track.label;\n")
                  .append("                        option.selected = (track.id === currentAudioTrack);\n")
                  .append("                        audioSelect.appendChild(option);\n")
                  .append("                    });\n")
                  .append("                    audioSelect.addEventListener('change', () => {\n")
                  .append("                        const selectedTrackId = audioSelect.value;\n")
                  .append("                        const currUrl = new URL(window.location.href);\n")
                  .append("                        currUrl.searchParams.set('audio_track', selectedTrackId);\n")
                  .append("                        window.history.replaceState({}, '', currUrl);\n")
                  .append("                        const currentTime = player.currentTime();\n")
                  .append("                        const isPaused = player.paused();\n")
                  .append("                        const manifestUrl = '/manifest?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUrl())).append("&audio_track=' + encodeURIComponent(selectedTrackId);\n")
                  .append("                        player.src({ src: manifestUrl, type: 'application/dash+xml' });\n")
                  .append("                        player.ready(() => {\n")
                  .append("                            setTimeout(() => {\n")
                  .append("                                player.currentTime(currentTime);\n")
                  .append("                                if (!isPaused) {\n")
                  .append("                                    player.play().catch(e => {});\n")
                  .append("                                }\n")
                  .append("                            }, 150);\n")
                  .append("                        });\n")
                  .append("                    });\n")
                  .append("                }\n")
                  .append("                \n")
                  .append("                const cacheBtn = document.getElementById('cache-offline-btn');\n")
                  .append("                if (cacheBtn) {\n")
                  .append("                    cacheBtn.addEventListener('click', (e) => {\n")
                  .append("                        e.preventDefault();\n")
                  .append("                        const qualitySelect = document.getElementById('quality-select');\n")
                  .append("                        const audioSelectEl = document.getElementById('audio-track-select');\n")
                  .append("                        const quality = qualitySelect ? qualitySelect.value : 'auto';\n")
                  .append("                        const audioTrack = audioSelectEl ? audioSelectEl.value : '';\n")
                  .append("                        const url = new URL(cacheBtn.href, window.location.origin);\n")
                  .append("                        url.searchParams.set('quality', quality);\n")
                  .append("                        url.searchParams.set('audio_track', audioTrack);\n")
                  .append("                        window.location.href = url.toString();\n")
                  .append("                    });\n")
                  .append("                }\n")
                  .append(advancedJs)
                  .append("                if ('mediaSession' in navigator) {\n")
                  .append("                    navigator.mediaSession.metadata = new MediaMetadata({\n")
                  .append("                        title: '").append(escapeJs(info.getName())).append("',\n")
                  .append("                        artist: '").append(escapeJs(info.getUploaderName())).append("'\n")
                  .append("                    });\n")
                  .append("                }\n")
                  .append("            })();\n")
                  .append("        </script>\n");
            }
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
              .append("        <audio id=\"audio-player\" controls autoplay class=\"native-audio\">\n")
              .append("          <source src=\"/stream?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUrl())).append("\" type=\"").append(audioMime).append("\">\n")
              .append("          Your browser does not support the HTML5 audio tag.\n")
              .append("        </audio>\n")
              .append("        <script>\n")
              .append("            (function() {\n")
              .append("                const audio = document.getElementById('audio-player');\n")
              .append("                const streamDuration = ").append(duration).append(";\n")
              .append("                if (audio && streamDuration > 0) {\n")
              .append("                    const setDuration = () => {\n")
              .append("                        if (Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'duration')) {\n")
              .append("                           try {\n")
              .append("                               Object.defineProperty(audio, 'duration', { value: streamDuration, configurable: true });\n")
              .append("                               audio.dispatchEvent(new Event('durationchange'));\n")
              .append("                           } catch(e) { console.error('Failed to override audio duration:', e); }\n")
              .append("                        }\n")
              .append("                    };\n")
              .append("                    audio.addEventListener('loadedmetadata', setDuration);\n")
              .append("                    if (audio.readyState >= 1) setDuration();\n")
              .append("                }\n")
              .append("            })();\n")
              .append("        </script>\n");
        }

        String formattedViews = info.getViewCount() >= 0 ? formatCount(info.getViewCount()) + " views" : "Unknown views";
        String uploadDate = info.getTextualUploadDate() != null ? info.getTextualUploadDate() : "Unknown date";
        String likesText = info.getLikeCount() >= 0 ? formatCount(info.getLikeCount()) : "Like";
        String subsText = info.getUploaderSubscriberCount() >= 0 ? formatCount(info.getUploaderSubscriberCount()) + " subscribers" : "";

        sb.append("        <div class=\"media-info\">\n")
          .append("          <h1 class=\"media-title\">").append(info.getName()).append("</h1>\n");

        sb.append("          <div class=\"uploader-profile\">\n")
          .append("            <div class=\"uploader-main\">\n")
          .append("              <img class=\"uploader-avatar\" src=\"").append(getThumbnailUrl(info.getUploaderAvatars())).append("\">\n")
          .append("              <div class=\"uploader-info\">\n")
          .append("                <a href=\"/channel?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUploaderUrl())).append("\" class=\"uploader-name\">")
          .append(info.getUploaderName()).append("</a>\n")
          .append("                <span class=\"uploader-subs\">").append(subsText).append("</span>\n")
          .append("              </div>\n");

        String uploaderAvatar = getThumbnailUrl(info.getUploaderAvatars());
        if (isSubscribed) {
            sb.append("              <a href=\"/subscribe?action=unsubscribe&id=").append(encodeUrl(info.getUploaderUrl())).append("&back=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleSubscribe(event, this, '").append(escapeJs(info.getUploaderUrl())).append("', '").append(escapeJs(info.getUploaderName())).append("', '").append(escapeJs(uploaderAvatar)).append("')\" class=\"subscribe-btn subscribed\">Subscribed</a>\n");
        } else {
            sb.append("              <a href=\"/subscribe?action=subscribe&id=").append(encodeUrl(info.getUploaderUrl())).append("&name=").append(encodeUrl(info.getUploaderName())).append("&avatar=").append(encodeUrl(uploaderAvatar)).append("&back=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleSubscribe(event, this, '").append(escapeJs(info.getUploaderUrl())).append("', '").append(escapeJs(info.getUploaderName())).append("', '").append(escapeJs(uploaderAvatar)).append("')\" class=\"subscribe-btn\">Subscribe</a>\n");
        }
        sb.append("            </div>\n");

        sb.append("            <div class=\"action-buttons-group\">\n")
          .append("              <div class=\"like-dislike-pill\">\n")
          .append("                <button class=\"pill-btn like-btn\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"16\" height=\"16\"><path d=\"M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z\"/></svg> ").append(likesText).append("</button>\n")
          .append("                <div class=\"pill-divider\"></div>\n")
          .append("                <button class=\"pill-btn dislike-btn\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"16\" height=\"16\" style=\"transform:scaleY(-1);\"><path d=\"M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z\"/></svg></button>\n")
          .append("              </div>\n")
          .append("              <button type=\"button\" onclick=\"if (window.NewPipeApp &amp;&amp; window.NewPipeApp.enterPip) { window.NewPipeApp.enterPip(); } else if (document.pictureInPictureEnabled &amp;&amp; document.querySelector('video')) { document.querySelector('video').requestPictureInPicture(); }\" class=\"action-pill-btn\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"16\" height=\"16\" style=\"margin-right:6px;\"><path d=\"M19 11h-8v6h8v-6zm4-8H1c-.55 0-1 .45-1 1v16c0 .55.45 1 1 1h22c.55 0 1-.45 1-1V4c0-.55-.45-1-1-1zm-2 16H3V5h18v14z\"/></svg>Pop-up</button>\n")
          .append("              <a href=\"/audio?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUrl())).append("\" class=\"action-pill-btn\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"16\" height=\"16\" style=\"margin-right:6px;\"><path d=\"M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z\"/></svg>Audio Only</a>\n")
          .append("              <button type=\"button\" onclick=\"shareLink('").append(escapeJs(info.getUrl())).append("', '").append(escapeJs(info.getName())).append("')\" class=\"action-pill-btn\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"16\" height=\"16\" style=\"margin-right:6px;\"><path d=\"M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z\"/></svg>Share</button>\n");

        if (cachedVideo == null) {
            sb.append("              <a id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" href=\"/cache?action=add&id=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleCache(event, this, '").append(escapeJs(info.getUrl())).append("', ").append(serviceId).append(")\" class=\"action-pill-btn\">Download</a>\n");
        } else if ("COMPLETED".equals(cachedVideo.getStatus())) {
            sb.append("              <a id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" href=\"/cache?action=delete&id=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleCache(event, this, '").append(escapeJs(info.getUrl())).append("', ").append(serviceId).append(")\" class=\"action-pill-btn danger\">Delete Download</a>\n");
        } else if ("DOWNLOADING".equals(cachedVideo.getStatus()) || "PENDING".equals(cachedVideo.getStatus())) {
            sb.append("              <span id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" class=\"action-pill-btn disabled\">Downloading (").append(cachedVideo.getProgress()).append("%)</span>\n");
        } else if ("FAILED".equals(cachedVideo.getStatus())) {
            sb.append("              <a id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" href=\"/cache?action=add&id=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleCache(event, this, '").append(escapeJs(info.getUrl())).append("', ").append(serviceId).append(")\" class=\"action-pill-btn danger\">Retry Download</a>\n");
        }
        sb.append("            </div>\n");
        sb.append("          </div>\n");

        sb.append("          <div class=\"media-description\">\n")
          .append("            <div style=\"font-weight:700; font-size:13.5px; margin-bottom:8px; color:var(--text-color);\">").append(formattedViews).append(" &nbsp;•&nbsp; ").append(uploadDate).append("</div>\n")
          .append(info.getDescription() != null ? info.getDescription().getContent() : "No description provided.")
          .append("          </div>\n")
          .append("        </div>\n");

        sb.append("        <div class=\"comments-section\">\n")
          .append("          <h3 class=\"comment-count\">💬 Comments</h3>\n");
        sb.append("          <div class=\"loading-placeholder\">Access comments by opening the related section below or scrolling.</div>\n")
          .append("        </div>\n")
          .append("      </div>\n");

        sb.append("      <div class=\"sidebar\">\n")
          .append("        <h3 style=\"font-size: 16px; font-weight: 700; margin-bottom: 16px;\">Related Content</h3>\n");
        for (InfoItem related : info.getRelatedItems()) {
            String uploader = "";
            String metaText = "";
            if (related instanceof StreamInfoItem) {
                StreamInfoItem stream = (StreamInfoItem) related;
                uploader = stream.getUploaderName();
                metaText = (stream.getViewCount() >= 0 ? formatCount(stream.getViewCount()) + " views" : "Live") + " • " + (stream.getTextualUploadDate() != null ? stream.getTextualUploadDate() : "");
            } else {
                uploader = related.getName();
            }
            if (uploader == null) uploader = "";

            sb.append("        <div class=\"card\" style=\"margin-bottom:8px; flex-direction:row; gap:8px; height:94px; background:transparent; border:none; box-shadow:none; min-width:0; overflow:hidden;\">\n")
              .append("          <a href=\"/watch?serviceId=").append(serviceId).append("&id=").append(related.getUrl()).append("\" style=\"flex-shrink:0; width:168px; height:94px; border-radius:8px; overflow:hidden; background:var(--card-thumbnail-bg);\">\n")
              .append("            <img src=\"").append(getThumbnailUrl(related.getThumbnails())).append("\" style=\"width:100%; height:100%; object-fit:cover; flex-shrink:0;\">\n")
              .append("          </a>\n")
              .append("          <div class=\"card-details\" style=\"padding:0; display:flex; flex-direction:column; justify-content:flex-start; min-width:0; flex-grow:1; overflow:hidden;\">\n")
              .append("            <a href=\"/watch?serviceId=").append(serviceId).append("&id=").append(related.getUrl()).append("\" class=\"card-title\" style=\"font-size:14px; font-weight:500; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; line-height:1.2; margin-bottom:4px; word-break:break-word; overflow-wrap:break-word;\">")
              .append(related.getName()).append("</a>\n")
              .append("            <span class=\"card-meta\" style=\"font-size:12px; line-height:1.4;\">\n")
              .append("              <span class=\"card-uploader\">").append(uploader).append("</span>\n");
            if (!metaText.isEmpty()) {
                sb.append("              <span>").append(metaText).append("</span>\n");
            }
            sb.append("            </span>\n")
              .append("          </div>\n")
              .append("        </div>\n");
        }
        sb.append("      </div>\n");
        sb.append("    </div>\n")
          .append("  </div>\n")
          .append("</div>\n");

        return sb.toString();
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

    public static void renderGrid(StringBuilder sb, int serviceId, List<InfoItem> items) {
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
                    if (item instanceof StreamInfoItem) {
                        StreamInfoItem stream = (StreamInfoItem) item;
                        if (stream.getDuration() > 0 && stream.getDuration() <= 120) {
                            String vidId = org.schabi.newpipe.localserver.LocalHttpServer.getVideoId(stream.getUrl());
                            clickUrl = "/shorts?serviceId=" + serviceId + "&id=" + vidId;
                        } else {
                            clickUrl = "/watch?serviceId=" + serviceId + "&id=" + item.getUrl();
                        }
                    } else {
                        clickUrl = "/watch?serviceId=" + serviceId + "&id=" + item.getUrl();
                    }
                    break;
            }

            if (item.getInfoType() == org.schabi.newpipe.extractor.InfoItem.InfoType.CHANNEL) {
                sb.append("    <div class=\"card\" style=\"flex-direction:row; align-items:center; gap:16px; padding:12px 0; min-width:0; max-width:100%; overflow:hidden;\">\n")
                  .append("      <a href=\"").append(clickUrl).append("\" style=\"flex-shrink:0;\">\n")
                  .append("        <img class=\"channel-card-avatar\" src=\"").append(getThumbnailUrl(item.getThumbnails())).append("\">\n")
                  .append("      </a>\n")
                  .append("      <div class=\"card-info\" style=\"min-width:0; flex-grow:1; flex-shrink:1; overflow:hidden;\">\n")
                  .append("        <a href=\"").append(clickUrl).append("\" class=\"card-title\" style=\"font-size:16px; font-weight:600; margin-bottom:4px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;\">").append(item.getName()).append("</a>\n")
                  .append("        <div class=\"card-meta\">\n")
                  .append("          <span class=\"card-uploader\" style=\"white-space:nowrap; overflow:hidden; text-overflow:ellipsis;\">").append(item.getName()).append("</span>\n")
                  .append("          <span style=\"color:#ff0000; font-weight:bold; font-size:11px;\">👤 Channel</span>\n")
                  .append("        </div>\n")
                  .append("      </div>\n")
                  .append("    </div>\n");
                continue;
            }

            String uploaderName = "";
            if (item instanceof StreamInfoItem) {
                uploaderName = ((StreamInfoItem) item).getUploaderName();
            } else {
                uploaderName = item.getName();
            }
            if (uploaderName == null) uploaderName = "";
            String firstChar = (!uploaderName.isEmpty()) ? uploaderName.substring(0, 1).toUpperCase() : "?";
            int hash = Math.abs(uploaderName.hashCode());
            String[] colors = {"#ff5722", "#e91e63", "#9c27b0", "#673ab7", "#3f51b5", "#2196f3", "#03a9f4", "#00bcd4", "#009688", "#4caf50", "#8bc34a", "#cddc39", "#ffc107", "#ff9800"};
            String avatarBg = colors[hash % colors.length];

            sb.append("    <div class=\"card\">\n")
              .append("      <a href=\"").append(clickUrl).append("\">\n")
              .append("        <img class=\"card-thumbnail\" src=\"").append(getThumbnailUrl(item.getThumbnails())).append("\">\n")
              .append("      </a>\n")
              .append("      <div class=\"card-details\">\n")
              .append("        <div class=\"card-avatar\" style=\"background-color:").append(avatarBg).append(";\">").append(firstChar).append("</div>\n")
              .append("        <div class=\"card-info\">\n")
              .append("          <a href=\"").append(clickUrl).append("\" class=\"card-title\">").append(item.getName()).append("</a>\n")
              .append("          <div class=\"card-meta\">\n");

            if (!typeBadge.isEmpty()) {
                sb.append("            <span style=\"color:#ff0000; font-weight:bold; font-size:11px;\">").append(typeBadge).append("</span>\n");
            } else if (item instanceof StreamInfoItem) {
                StreamInfoItem stream = (StreamInfoItem) item;
                sb.append("            <a href=\"/channel?serviceId=").append(serviceId).append("&id=").append(stream.getUploaderUrl()).append("\" class=\"card-uploader\">")
                  .append(stream.getUploaderName()).append("</a>\n")
                  .append("            <span>👁️ ").append(stream.getViewCount() >= 0 ? formatCount(stream.getViewCount()) + " views" : "Live / Dynamic").append(" • ")
                  .append(stream.getTextualUploadDate() != null ? stream.getTextualUploadDate() : "").append("</span>\n");
            } else {
                sb.append("            <span class=\"card-uploader\">").append(item.getName()).append("</span>\n");
            }

            sb.append("          </div>\n")
              .append("        </div>\n")
              .append("      </div>\n")
              .append("    </div>\n");
        }
        sb.append("  </div>\n");
    }

    private static String getThumbnailUrl(List<Image> thumbnails) {
        if (thumbnails != null && !thumbnails.isEmpty()) {
            for (int i = thumbnails.size() - 1; i >= 0; i--) {
                Image img = thumbnails.get(i);
                if (img != null && img.getUrl() != null && !img.getUrl().trim().isEmpty()) {
                    String url = img.getUrl().trim();
                    if (url.startsWith("//")) {
                        url = "https:" + url;
                    }
                    return url;
                }
            }
        }
        return "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?q=80&w=300&auto=format&fit=crop";
    }

    public static String formatCount(long count) {
        if (count < 0) return "";
        if (count < 1000) {
            return String.valueOf(count);
        } else if (count < 1000000) {
            double val = count / 1000.0;
            if (val >= 100) {
                return String.format(java.util.Locale.US, "%.0fK", val);
            } else {
                return String.format(java.util.Locale.US, "%.1fK", val).replace(".0K", "K");
            }
        } else if (count < 1000000000) {
            double val = count / 1000000.0;
            if (val >= 100) {
                return String.format(java.util.Locale.US, "%.0fM", val);
            } else {
                return String.format(java.util.Locale.US, "%.1fM", val).replace(".0M", "M");
            }
        } else {
            double val = count / 1000000000.0;
            return String.format(java.util.Locale.US, "%.1fB", val).replace(".0B", "B");
        }
    }

    private static String encodeUrl(String url) {
        try {
            return java.net.URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return url;
        }
    }

    public static String renderAudioWatch(int serviceId, StreamInfo info, CachedVideo cachedVideo, boolean isSubscribed, boolean isTv) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, "", "audio"));

        String formattedViews = info.getViewCount() >= 0 ? formatCount(info.getViewCount()) + " views" : "Unknown views";
        String uploadDate = info.getTextualUploadDate() != null ? info.getTextualUploadDate() : "Unknown date";
        String likesText = info.getLikeCount() >= 0 ? formatCount(info.getLikeCount()) : "Like";

        String posterUrl = getThumbnailUrl(info.getThumbnails());
        String audioMime = "audio/mpeg";
        if (info.getAudioStreams() != null && !info.getAudioStreams().isEmpty()) {
            AudioStream stream = info.getAudioStreams().get(0);
            if (stream.getFormat() != null) {
                audioMime = stream.getFormat().mimeType;
            }
        }

        sb.append("<div class=\"container\">\n")
          .append("  <div class=\"player-container\">\n")
          .append("    <div class=\"player-layout\">\n")
          .append("      <div class=\"main-content\">\n")
          .append("        <div class=\"audio-player-card\" style=\"display:flex; flex-direction:column; align-items:center; background:var(--card-bg); border-radius:24px; padding:32px 24px; border:1px solid var(--card-border); box-shadow:0 8px 24px rgba(0,0,0,0.12); text-align:center;\">\n")
          .append("          <div style=\"position:relative; width:240px; height:240px; margin-bottom:24px;\">\n")
          .append("            <img id=\"audio-cover\" src=\"").append(posterUrl).append("\" style=\"width:100%; height:100%; border-radius:20px; object-fit:cover; box-shadow:0 8px 20px rgba(0,0,0,0.3); transition:transform 0.5s ease;\">\n")
          .append("            <div style=\"position:absolute; bottom:12px; right:12px; background:rgba(0,0,0,0.7); color:#fff; padding:4px 10px; border-radius:20px; font-size:12px; font-weight:600;\">🎵 Audio Only</div>\n")
          .append("          </div>\n")
          .append("          <h1 class=\"media-title\" style=\"font-size:22px; font-weight:700; margin-bottom:8px;\">").append(info.getName()).append("</h1>\n")
          .append("          <a href=\"/channel?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUploaderUrl())).append("\" style=\"font-size:15px; color:var(--logo-color, #6750A4); font-weight:600; margin-bottom:20px;\">").append(info.getUploaderName()).append("</a>\n")
          .append("          <audio id=\"audio-player\" controls autoplay style=\"width:100%; max-width:540px; height:48px; border-radius:24px; margin-bottom:20px;\">\n")
          .append("            <source src=\"/stream?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUrl())).append("\" type=\"").append(audioMime).append("\">\n")
          .append("            Your browser does not support the HTML5 audio element.\n")
          .append("          </audio>\n")
          .append("          <script>\n")
          .append("            (function() {\n")
          .append("              const audio = document.getElementById('audio-player');\n")
          .append("              const cover = document.getElementById('audio-cover');\n")
          .append("              const audioStreamUrl = window.location.origin + '/stream?serviceId=").append(serviceId).append("&id=' + encodeURIComponent('").append(escapeJs(info.getUrl())).append("');\n")
          .append("              const titleText = '").append(escapeJs(info.getName())).append("';\n")
          .append("              const artistText = '").append(escapeJs(info.getUploaderName())).append("';\n")
          .append("              if (audio) {\n")
          .append("                audio.addEventListener('play', () => {\n")
          .append("                  if(cover) cover.style.transform = 'scale(1.04)';\n")
          .append("                  if (window.NewPipeApp && window.NewPipeApp.resumeNativeAudio) {\n")
          .append("                    window.NewPipeApp.resumeNativeAudio();\n")
          .append("                  }\n")
          .append("                });\n")
          .append("                audio.addEventListener('pause', () => {\n")
          .append("                  if(cover) cover.style.transform = 'scale(1)';\n")
          .append("                  if (window.NewPipeApp && window.NewPipeApp.pauseNativeAudio) {\n")
          .append("                    window.NewPipeApp.pauseNativeAudio();\n")
          .append("                  }\n")
          .append("                });\n")
          .append("                audio.addEventListener('seeked', () => {\n")
          .append("                  if (window.NewPipeApp && window.NewPipeApp.seekNativeAudio) {\n")
          .append("                    window.NewPipeApp.seekNativeAudio(Math.floor(audio.currentTime * 1000));\n")
          .append("                  }\n")
          .append("                });\n")
          .append("              }\n")
          .append("              if (window.NewPipeApp && window.NewPipeApp.playNativeAudio) {\n")
          .append("                if (audio) {\n")
          .append("                  audio.muted = true;\n")
          .append("                  setInterval(() => {\n")
          .append("                    if (window.NewPipeApp.getNativeAudioPosition) {\n")
          .append("                      const nativePosSec = window.NewPipeApp.getNativeAudioPosition() / 1000.0;\n")
          .append("                      if (nativePosSec > 0 && Math.abs(audio.currentTime - nativePosSec) > 1.5) {\n")
          .append("                        audio.currentTime = nativePosSec;\n")
          .append("                      }\n")
          .append("                    }\n")
          .append("                  }, 1000);\n")
          .append("                }\n")
          .append("                window.NewPipeApp.playNativeAudio(audioStreamUrl, titleText, artistText);\n")
          .append("              }\n")
          .append("              if ('mediaSession' in navigator) {\n")
          .append("                navigator.mediaSession.metadata = new MediaMetadata({\n")
          .append("                  title: titleText,\n")
          .append("                  artist: artistText,\n")
          .append("                  artwork: [{ src: '").append(escapeJs(posterUrl)).append("', sizes: '512x512', type: 'image/png' }]\n")
          .append("                });\n")
          .append("              }\n")
          .append("            })();\n")
          .append("          </script>\n")
          .append("          <div class=\"action-buttons-group\" style=\"justify-content:center; flex-wrap:wrap; gap:10px;\">\n")
          .append("            <a href=\"/watch?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUrl())).append("&amp;force_video=true\" class=\"action-pill-btn\" style=\"background-color:var(--logo-color, #6750A4); color:#fff;\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"16\" height=\"16\" style=\"margin-right:6px;\"><path d=\"M21 3H3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H3V5h18v14zM9 8l7 4-7 4V8z\"/></svg>📺 Video Mode</a>\n")
          .append("            <div class=\"like-dislike-pill\">\n")
          .append("              <button class=\"pill-btn like-btn\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"16\" height=\"16\"><path d=\"M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z\"/></svg> ").append(likesText).append("</button>\n")
          .append("            </div>\n");

        String uploaderAvatar = getThumbnailUrl(info.getUploaderAvatars());
        if (isSubscribed) {
            sb.append("            <a href=\"/subscribe?action=unsubscribe&id=").append(encodeUrl(info.getUploaderUrl())).append("&back=").append(encodeUrl("/audio?serviceId=" + serviceId + "&id=" + info.getUrl())).append("\" onclick=\"toggleSubscribe(event, this, '").append(escapeJs(info.getUploaderUrl())).append("', '").append(escapeJs(info.getUploaderName())).append("', '").append(escapeJs(uploaderAvatar)).append("')\" class=\"subscribe-btn subscribed\">Subscribed</a>\n");
        } else {
            sb.append("            <a href=\"/subscribe?action=subscribe&id=").append(encodeUrl(info.getUploaderUrl())).append("&name=").append(encodeUrl(info.getUploaderName())).append("&avatar=").append(encodeUrl(uploaderAvatar)).append("&back=").append(encodeUrl("/audio?serviceId=" + serviceId + "&id=" + info.getUrl())).append("\" onclick=\"toggleSubscribe(event, this, '").append(escapeJs(info.getUploaderUrl())).append("', '").append(escapeJs(info.getUploaderName())).append("', '").append(escapeJs(uploaderAvatar)).append("')\" class=\"subscribe-btn\">Subscribe</a>\n");
        }

        if (cachedVideo == null) {
            sb.append("            <a id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" href=\"/cache?action=add&id=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleCache(event, this, '").append(escapeJs(info.getUrl())).append("', ").append(serviceId).append(")\" class=\"action-pill-btn\">Download</a>\n");
        } else if ("COMPLETED".equals(cachedVideo.getStatus())) {
            sb.append("            <a id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" href=\"/cache?action=delete&id=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleCache(event, this, '").append(escapeJs(info.getUrl())).append("', ").append(serviceId).append(")\" class=\"action-pill-btn danger\">Delete Download</a>\n");
        } else if ("DOWNLOADING".equals(cachedVideo.getStatus()) || "PENDING".equals(cachedVideo.getStatus())) {
            sb.append("            <span id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:var(--service-tab-bg); color:var(--text-color); cursor:default; pointer-events:none;\">Downloading (").append(cachedVideo.getProgress()).append("%)</span>\n");
        } else if ("FAILED".equals(cachedVideo.getStatus())) {
            sb.append("            <a id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" href=\"/cache?action=add&id=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleCache(event, this, '").append(escapeJs(info.getUrl())).append("', ").append(serviceId).append(")\" class=\"action-pill-btn danger\">Retry Download</a>\n");
        }
        sb.append("          </div>\n");
        sb.append("        </div>\n");

        sb.append("        <div class=\"media-description\" style=\"margin-top:20px;\">\n")
          .append("          <div style=\"font-weight:700; font-size:13.5px; margin-bottom:8px; color:var(--text-color);\">").append(formattedViews).append(" &nbsp;•&nbsp; ").append(uploadDate).append("</div>\n")
          .append(info.getDescription() != null ? info.getDescription().getContent() : "No description provided.")
          .append("        </div>\n")
          .append("      </div>\n");

        sb.append("      <div class=\"sidebar\">\n")
          .append("        <h3 style=\"font-size:16px; font-weight:700; margin-bottom:16px;\">Up Next</h3>\n");
        for (InfoItem related : info.getRelatedItems()) {
            String uploader = "";
            String metaText = "";
            if (related instanceof StreamInfoItem) {
                StreamInfoItem stream = (StreamInfoItem) related;
                uploader = stream.getUploaderName();
                metaText = (stream.getViewCount() >= 0 ? formatCount(stream.getViewCount()) + " views" : "Live") + " • " + (stream.getTextualUploadDate() != null ? stream.getTextualUploadDate() : "");
            } else {
                uploader = related.getName();
            }
            if (uploader == null) uploader = "";

            sb.append("        <div class=\"card\" style=\"margin-bottom:8px; flex-direction:row; gap:8px; height:94px; background:transparent; border:none; box-shadow:none;\">\n")
              .append("          <a href=\"/audio?serviceId=").append(serviceId).append("&id=").append(related.getUrl()).append("\" style=\"flex-shrink:0; width:120px; height:80px; border-radius:12px; overflow:hidden; background:var(--card-thumbnail-bg);\">\n")
              .append("            <img src=\"").append(getThumbnailUrl(related.getThumbnails())).append("\" style=\"width:100%; height:100%; object-fit:cover;\">\n")
              .append("          </a>\n")
              .append("          <div class=\"card-details\" style=\"padding:0; display:flex; flex-direction:column; justify-content:flex-start; min-width:0; flex-grow:1;\">\n")
              .append("            <a href=\"/audio?serviceId=").append(serviceId).append("&id=").append(related.getUrl()).append("\" class=\"card-title\" style=\"font-size:14px; font-weight:500; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; line-height:1.2; margin-bottom:4px;\">")
              .append(related.getName()).append("</a>\n")
              .append("            <span class=\"card-meta\" style=\"font-size:12px; line-height:1.4;\">\n")
              .append("              <span class=\"card-uploader\">").append(uploader).append("</span>\n");
            if (!metaText.isEmpty()) {
                sb.append("              <span>").append(metaText).append("</span>\n");
            }
            sb.append("            </span>\n")
              .append("          </div>\n")
              .append("        </div>\n");
        }
        sb.append("      </div>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");
        sb.append("</div>\n");

        return wrapInTemplate("Audio: " + info.getName(), sb.toString(), isTv);
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
          .append("            <a href=\"/cache?action=delete&id=").append(encodeUrl(video.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:#c00c0c; color:#ffffff;\">Delete Download</a>\n")
          .append("          </div>\n");

        sb.append("          <div class=\"media-description\">")
          .append(video.getDescription() != null && !video.getDescription().isEmpty() ? video.getDescription() : "No description cached.")
          .append("          </div>\n")
          .append("        </div>\n");

        sb.append("      </div>\n");

        sb.append("      <div class=\"sidebar\">\n")
          .append("        <h3 style=\"font-size: 16px; font-weight: 700; margin-bottom: 16px;\">Other Cached Videos</h3>\n");
        int count = 0;
        for (CachedVideo other : otherCached) {
            if (other.getUrl().equals(video.getUrl())) continue;
            count++;
            sb.append("        <div class=\"card\" style=\"margin-bottom:8px; flex-direction:row; gap:8px; height:94px; background:transparent; border:none; box-shadow:none;\">\n")
              .append("          <a href=\"/watch?serviceId=").append(serviceId).append("&id=").append(encodeUrl(other.getUrl())).append("\" style=\"flex-shrink:0; width:168px; height:94px; border-radius:8px; overflow:hidden; background:var(--card-thumbnail-bg);\">\n")
              .append("            <img src=\"/thumbnail?id=").append(encodeUrl(other.getUrl())).append("\" style=\"width:100%; height:100%; object-fit:cover;\">\n")
              .append("          </a>\n")
              .append("          <div class=\"card-details\" style=\"padding:0; display:flex; flex-direction:column; justify-content:flex-start; min-width:0; flex-grow:1;\">\n")
              .append("            <a href=\"/watch?serviceId=").append(serviceId).append("&id=").append(encodeUrl(other.getUrl())).append("\" class=\"card-title\" style=\"font-size:14px; font-weight:500; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; line-height:1.2; margin-bottom:4px;\">")
              .append(other.getTitle()).append("</a>\n")
              .append("            <span class=\"card-meta\" style=\"font-size:12px; line-height:1.4;\">\n")
              .append("              <span class=\"card-uploader\">").append(other.getUploader()).append("</span>\n")
              .append("              <span>💾 Cached</span>\n")
              .append("            </span>\n")
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

                String uploaderName = item.getUploader();
                if (uploaderName == null) uploaderName = "";
                String firstChar = (!uploaderName.isEmpty()) ? uploaderName.substring(0, 1).toUpperCase() : "?";
                int hash = Math.abs(uploaderName.hashCode());
                String[] colors = {"#ff5722", "#e91e63", "#9c27b0", "#673ab7", "#3f51b5", "#2196f3", "#03a9f4", "#00bcd4", "#009688", "#4caf50", "#8bc34a", "#cddc39", "#ffc107", "#ff9800"};
                String avatarBg = colors[hash % colors.length];

                sb.append("    <div class=\"card\">\n")
                  .append("      <a href=\"").append(clickUrl).append("\">\n")
                  .append("        <img class=\"card-thumbnail\" src=\"/thumbnail?id=").append(encodeUrl(item.getUrl())).append("\">\n")
                  .append("      </a>\n")
                  .append("      <div class=\"card-details\">\n")
                  .append("        <div class=\"card-avatar\" style=\"background-color:").append(avatarBg).append(";\">").append(firstChar).append("</div>\n")
                  .append("        <div class=\"card-info\">\n")
                  .append("          <a href=\"").append(clickUrl).append("\" class=\"card-title\">").append(item.getTitle()).append("</a>\n")
                  .append("          <div class=\"card-meta\">\n")
                  .append("            <span class=\"card-uploader\">").append(item.getUploader()).append("</span>\n")
                  .append("            <div style=\"display:flex; justify-content:space-between; align-items:center; margin-top:8px;\">\n")
                  .append("              <span class=\"cache-status-label\" data-url=\"").append(escapeJs(item.getUrl())).append("\" data-status=\"").append(item.getStatus()).append("\" style=\"color:").append(statusColor).append("; font-weight:bold; font-size:12px;\">").append(statusLabel).append("</span>\n")
                  .append("              <a href=\"/cache?action=delete&id=").append(encodeUrl(item.getUrl())).append("\" onclick=\"toggleCache(event, this, '").append(escapeJs(item.getUrl())).append("', ").append(serviceId).append(")\" class=\"subscribe-btn\" style=\"background-color:#c00c0c; color:#ffffff; padding:4px 10px; font-size:11px; border-radius:12px;\">Delete</a>\n")
                  .append("            </div>\n")
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

    public static String renderSubscriptions(int serviceId, List<InfoItem> channels, List<InfoItem> playlists, List<InfoItem> watchLater, String activeTab, boolean isTv) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, "", "subscriptions"));
        sb.append("<div class=\"container\">\n");

        boolean isPlaylists = "playlists".equals(activeTab);
        boolean isWatchLater = "watch_later".equals(activeTab);
        boolean isChannels = !isPlaylists && !isWatchLater;

        String channelsClass = isChannels ? "active" : "";
        String playlistsClass = isPlaylists ? "active" : "";
        String watchLaterClass = isWatchLater ? "active" : "";

        sb.append("  <div class=\"channel-header\" style=\"margin-bottom: 28px;\">\n")
          .append("    <div class=\"channel-tabs-selector\">\n")
          .append("      <a href=\"/subscriptions?serviceId=").append(serviceId).append("&tab=channels\" class=\"channel-tab-btn ").append(channelsClass).append("\">👤 Subscribed Channels (").append(channels.size()).append(")</a>\n")
          .append("      <a href=\"/subscriptions?serviceId=").append(serviceId).append("&tab=playlists\" class=\"channel-tab-btn ").append(playlistsClass).append("\">⭐ Saved Playlists (").append(playlists.size()).append(")</a>\n")
          .append("      <a href=\"/subscriptions?serviceId=").append(serviceId).append("&tab=watch_later\" class=\"channel-tab-btn ").append(watchLaterClass).append("\">⏳ Watch Later (").append(watchLater != null ? watchLater.size() : 0).append(")</a>\n")
          .append("    </div>\n")
          .append("  </div>\n");

        if (isPlaylists) {
            if (playlists == null || playlists.isEmpty()) {
                sb.append("<div class=\"loading-placeholder\">You haven't saved any playlists yet.</div>\n");
            } else {
                renderGrid(sb, serviceId, playlists);
            }
        } else if (isWatchLater) {
            if (watchLater == null || watchLater.isEmpty()) {
                sb.append("<div class=\"loading-placeholder\">Your Watch Later list is empty.</div>\n");
            } else {
                renderGrid(sb, serviceId, watchLater);
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

    public static String renderSettings(int serviceId, String currentQuality, boolean hideWatched, boolean hideShorts, String homeFeedMode, boolean saved, boolean isTv) {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeaderHtml(serviceId, "", "settings"));
        sb.append("<div class=\"container\">\n")
          .append("  <div class=\"settings-card\">\n")
          .append("    <h1 class=\"settings-title\">⚙️ Preferences &amp; Backup</h1>\n");

        if (saved) {
            sb.append("    <div class=\"alert-banner\">✓ Settings saved successfully!</div>\n");
        }

        sb.append("    <form action=\"/settings\" method=\"GET\">\n")
          .append("      <input type=\"hidden\" name=\"action\" value=\"save\">\n")
          .append("      <div class=\"settings-section\">\n")
          .append("        <h3 class=\"settings-section-title\">Filter Settings</h3>\n")
          .append("        <div class=\"setting-row\">\n")
          .append("          <div class=\"setting-label-group\">\n")
          .append("            <span class=\"setting-label\">Preferred Video Quality</span>\n")
          .append("            <span class=\"setting-desc\">Default playback resolution for streams.</span>\n")
          .append("          </div>\n")
          .append("          <select name=\"video_quality\" style=\"padding: 8px 16px; border-radius: 8px; border: 1px solid var(--search-input-border); background-color: var(--bg-color); color: var(--text-color); font-family: inherit; font-size: 14px; outline: none; cursor: pointer; width: 100%; max-width: 280px;\">\n");

        String[] qualities = {"144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p"};
        for (String q : qualities) {
            String selected = q.equals(currentQuality) ? "selected" : "";
            sb.append("            <option value=\"").append(q).append("\" ").append(selected).append(">").append(q).append("</option>\n");
        }

        sb.append("          </select>\n")
          .append("        </div>\n")
          .append("        <div class=\"setting-row\">\n")
          .append("          <div class=\"setting-label-group\">\n")
          .append("            <span class=\"setting-label\">Home Feed Content</span>\n")
          .append("            <span class=\"setting-desc\">Choose what content appears on your Home feed.</span>\n")
          .append("          </div>\n")
          .append("          <select name=\"home_feed_mode\" style=\"padding: 8px 16px; border-radius: 8px; border: 1px solid var(--search-input-border); background-color: var(--bg-color); color: var(--text-color); font-family: inherit; font-size: 14px; outline: none; cursor: pointer; width: 100%; max-width: 280px;\">\n");

        String[][] modes = {
            {"mix", "Mix (Recommendations & Subscriptions)"},
            {"subs", "Subscriptions Only"},
            {"recs", "Recommendations Only"}
        };
        for (String[] m : modes) {
            String selected = m[0].equals(homeFeedMode) ? "selected" : "";
            sb.append("            <option value=\"").append(m[0]).append("\" ").append(selected).append(">").append(m[1]).append("</option>\n");
        }

        sb.append("          </select>\n")
          .append("        </div>\n")
          .append("        <div class=\"setting-row\">\n")
          .append("          <div class=\"setting-label-group\">\n")
          .append("            <span class=\"setting-label\">Hide Watched Videos</span>\n")
          .append("            <span class=\"setting-desc\">Hide videos you have already watched from lists.</span>\n")
          .append("          </div>\n")
          .append("          <label class=\"switch\">\n")
          .append("            <input type=\"checkbox\" name=\"hide_watched\" value=\"on\" ").append(hideWatched ? "checked" : "").append(">\n")
          .append("            <span class=\"slider\"></span>\n")
          .append("          </label>\n")
          .append("        </div>\n")
          .append("        <div class=\"setting-row\">\n")
          .append("          <div class=\"setting-label-group\">\n")
          .append("            <span class=\"setting-label\">Hide Reels</span>\n")
          .append("            <span class=\"setting-desc\">Hide vertical videos shorter than 2 minutes.</span>\n")
          .append("          </div>\n")
          .append("          <label class=\"switch\">\n")
          .append("            <input type=\"checkbox\" name=\"hide_shorts\" value=\"on\" ").append(hideShorts ? "checked" : "").append(">\n")
          .append("            <span class=\"slider\"></span>\n")
          .append("          </label>\n")
          .append("        </div>\n")
          .append("        <div class=\"setting-row\" id=\"mobile-theme-row\">\n")
          .append("          <div class=\"setting-label-group\">\n")
          .append("            <span class=\"setting-label\">Dark Theme</span>\n")
          .append("            <span class=\"setting-desc\">Toggle between dark and light appearance.</span>\n")
          .append("          </div>\n")
          .append("          <label class=\"switch\">\n")
          .append("            <input type=\"checkbox\" id=\"settings-theme-toggle\">\n")
          .append("            <span class=\"slider\"></span>\n")
          .append("          </label>\n")
          .append("        </div>\n")
          .append("        <div class=\"setting-row\" id=\"settings-accent-row\">\n")
          .append("          <div class=\"setting-label-group\">\n")
          .append("            <span class=\"setting-label\">Theme Accent Color</span>\n")
          .append("            <span class=\"setting-desc\">Select the primary accent color of the interface.</span>\n")
          .append("          </div>\n")
          .append("          <select id=\"settings-accent-select\" style=\"padding: 8px 16px; border-radius: 8px; border: 1px solid var(--search-input-border); background-color: var(--bg-color); color: var(--text-color); font-family: inherit; font-size: 14px; outline: none; cursor: pointer;\">\n")
          .append("            <option value=\"system\">System (Material You)</option>\n")
          .append("            <option value=\"purple\">Classic Purple</option>\n")
          .append("            <option value=\"green\">Forest Green</option>\n")
          .append("            <option value=\"blue\">Ocean Blue</option>\n")
          .append("            <option value=\"orange\">Sunset Orange</option>\n")
          .append("            <option value=\"red\">Crimson Red</option>\n")
          .append("          </select>\n")
          .append("        </div>\n")
          .append("        <div class=\"setting-row\" id=\"settings-pureblack-row\">\n")
          .append("          <div class=\"setting-label-group\">\n")
          .append("            <span class=\"setting-label\">AMOLED Black</span>\n")
          .append("            <span class=\"setting-desc\">Use pure black background in dark theme.</span>\n")
          .append("          </div>\n")
          .append("          <label class=\"switch\">\n")
          .append("            <input type=\"checkbox\" id=\"settings-pureblack-toggle\">\n")
          .append("            <span class=\"slider\"></span>\n")
          .append("          </label>\n")
          .append("        </div>\n")
          .append("        <div class=\"setting-row\" id=\"settings-audio-only-row\">\n")
          .append("          <div class=\"setting-label-group\">\n")
          .append("            <span class=\"setting-label\">Default to Audio Only</span>\n")
          .append("            <span class=\"setting-desc\">Always play the audio-only version of videos (Reels are excluded).</span>\n")
          .append("          </div>\n")
          .append("          <label class=\"switch\">\n")
          .append("            <input type=\"checkbox\" id=\"settings-audio-only-toggle\">\n")
          .append("            <span class=\"slider\"></span>\n")
          .append("          </label>\n")
          .append("        </div>\n")
          .append("        <div class=\"setting-row\" id=\"mobile-history-row\">\n")
          .append("          <div class=\"setting-label-group\">\n")
          .append("            <span class=\"setting-label\">Watch History</span>\n")
          .append("            <span class=\"setting-desc\">View your local watch history.</span>\n")
          .append("          </div>\n")
          .append("          <a href=\"/history\" class=\"subscribe-btn\" style=\"background-color: var(--logo-color); padding: 8px 20px; font-size: 14px; text-decoration: none; border-radius: 100px; display: inline-flex; align-items: center; justify-content: center; height: 36px;\">View</a>\n")
          .append("        </div>\n")
          .append("      </div>\n")
          .append("      <button type=\"submit\" class=\"btn-save btn-save-primary\" style=\"margin-bottom: 24px;\">Save Settings</button>\n")
          .append("    </form>\n")
          .append("    <div class=\"settings-section\">\n")
          .append("      <h3 class=\"settings-section-title\">Backup &amp; Restore Database</h3>\n")
          .append("      <p class=\"setting-desc\" style=\"margin-bottom: 16px;\">Export your local history, subscriptions, and bookmarks to a JSON file, or restore them from a previous backup.</p>\n")
          .append("      <div style=\"display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 16px;\">\n")
          .append("        <a href=\"/db/export\" class=\"subscribe-btn\" style=\"background-color: #007acc; text-decoration: none; display: inline-flex; align-items: center; justify-content: center; height: 38px; padding: 0 16px;\">💾 Export JSON</a>\n")
          .append("        <button type=\"button\" id=\"btn-import-web\" class=\"subscribe-btn\" style=\"background-color: #2e7d32; border: none; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; height: 38px; padding: 0 16px; color: white;\">📤 Import JSON</button>\n")
          .append("        <input type=\"file\" id=\"file-import-web\" accept=\".json\" style=\"display: none;\">\n")
          .append("      </div>\n")
          .append("    </div>\n")
          .append("  </div>\n")
          .append("</div>\n")
          .append("<script>\n")
          .append("    document.addEventListener('DOMContentLoaded', () => {\n")
          .append("        const btnImport = document.getElementById('btn-import-web');\n")
          .append("        const fileInput = document.getElementById('file-import-web');\n")
          .append("        if (btnImport && fileInput) {\n")
          .append("            btnImport.addEventListener('click', () => fileInput.click());\n")
          .append("            fileInput.addEventListener('change', () => {\n")
          .append("                const file = fileInput.files[0];\n")
          .append("                if (!file) return;\n")
          .append("                const reader = new FileReader();\n")
          .append("                reader.onload = (e) => {\n")
          .append("                    const content = e.target.result;\n")
          .append("                    fetch('/db/import', {\n")
          .append("                        method: 'POST',\n")
          .append("                        body: content,\n")
          .append("                        headers: { 'Content-Type': 'application/json' }\n")
          .append("                    })\n")
          .append("                    .then(res => res.text())\n")
          .append("                    .then(text => {\n")
          .append("                        if (text === 'SUCCESS') {\n")
          .append("                            alert('Database successfully imported! Page will reload.');\n")
          .append("                            window.location.reload();\n")
          .append("                        } else {\n")
          .append("                            alert('Import failed: Check file format');\n")
          .append("                        }\n")
          .append("                    })\n")
          .append("                    .catch(err => alert('Import error: ' + err));\n")
          .append("                };\n")
          .append("                reader.readAsText(file);\n")
          .append("            });\n")
          .append("        }\n")
          .append("    });\n")
          .append("</script>\n");

        return wrapInTemplate("Settings - LocalTube", sb.toString(), isTv);
    }

    private static String escapeJs(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String getCustomThemeCss() {
        StringBuilder sb = new StringBuilder();
        if (lightColors != null && !lightColors.isEmpty()) {
            sb.append(":root {\n")
              .append("  --bg-color: ").append(lightColors.getOrDefault("surface", "#fbfafe")).append(";\n")
              .append("  --text-color: ").append(lightColors.getOrDefault("onSurface", "#1d1b20")).append(";\n")
              .append("  --header-bg: ").append(lightColors.getOrDefault("surfaceContainer", "#f3f4f9")).append(";\n")
              .append("  --logo-color: ").append(lightColors.getOrDefault("primary", "#6750A4")).append(";\n")
              .append("  --search-input-bg: ").append(lightColors.getOrDefault("surfaceContainerHigh", "#ece6f0")).append(";\n")
              .append("  --search-btn-bg: ").append(lightColors.getOrDefault("surfaceContainerHigh", "#ece6f0")).append(";\n")
              .append("  --search-btn-hover: ").append(lightColors.getOrDefault("secondaryContainer", "#e8def8")).append(";\n")
              .append("  --service-tab-bg: ").append(lightColors.getOrDefault("surfaceContainerHigh", "#ece6f0")).append(";\n")
              .append("  --service-tab-hover-bg: ").append(lightColors.getOrDefault("secondaryContainer", "#e8def8")).append(";\n")
              .append("  --card-bg: ").append(lightColors.getOrDefault("surfaceContainerLow", "#ffffff")).append(";\n")
              .append("  --bottom-nav-bg: ").append(lightColors.getOrDefault("surfaceContainer", "#f3f4f9")).append(";\n")
              .append("  --bottom-nav-active-pill-bg: ").append(lightColors.getOrDefault("secondaryContainer", "#e8def8")).append(";\n")
              .append("  --bottom-nav-item-active-color: ").append(lightColors.getOrDefault("primary", "#21005d")).append(";\n")
              .append("  --settings-card-bg: ").append(lightColors.getOrDefault("surfaceContainerLow", "#ffffff")).append(";\n")
              .append("  --settings-section-title-color: ").append(lightColors.getOrDefault("primary", "#6750A4")).append(";\n")
              .append("  --textarea-border: ").append(lightColors.getOrDefault("outline", "#79747e")).append(";\n")
              .append("  --textarea-bg: ").append(lightColors.getOrDefault("surfaceContainerLow", "#ffffff")).append(";\n")
              .append("  --slider-bg: ").append(lightColors.getOrDefault("secondaryContainer", "#e8def8")).append(";\n")
              .append("}\n");
        }
        if (darkColors != null && !darkColors.isEmpty()) {
            sb.append("[data-theme=\"dark\"] {\n")
              .append("  --bg-color: ").append(darkColors.getOrDefault("surface", "#141218")).append(";\n")
              .append("  --text-color: ").append(darkColors.getOrDefault("onSurface", "#e6e1e5")).append(";\n")
              .append("  --header-bg: ").append(darkColors.getOrDefault("surfaceContainer", "#1d1b20")).append(";\n")
              .append("  --logo-color: ").append(darkColors.getOrDefault("primary", "#d0bcff")).append(";\n")
              .append("  --search-input-bg: ").append(darkColors.getOrDefault("surfaceContainerHigh", "#2b2930")).append(";\n")
              .append("  --search-btn-bg: ").append(darkColors.getOrDefault("surfaceContainerHigh", "#2b2930")).append(";\n")
              .append("  --search-btn-hover: ").append(darkColors.getOrDefault("secondaryContainer", "#4a4458")).append(";\n")
              .append("  --service-tab-bg: ").append(darkColors.getOrDefault("surfaceContainerHigh", "#2b2930")).append(";\n")
              .append("  --service-tab-hover-bg: ").append(darkColors.getOrDefault("secondaryContainer", "#4a4458")).append(";\n")
              .append("  --card-bg: ").append(darkColors.getOrDefault("surfaceContainerLow", "#1d1b20")).append(";\n")
              .append("  --bottom-nav-bg: ").append(darkColors.getOrDefault("surfaceContainer", "#1d1b20")).append(";\n")
              .append("  --bottom-nav-active-pill-bg: ").append(darkColors.getOrDefault("secondaryContainer", "#4a4458")).append(";\n")
              .append("  --bottom-nav-item-active-color: ").append(darkColors.getOrDefault("primary", "#e8def8")).append(";\n")
              .append("  --settings-card-bg: ").append(darkColors.getOrDefault("surfaceContainerLow", "#1d1b20")).append(";\n")
              .append("  --settings-section-title-color: ").append(darkColors.getOrDefault("primary", "#d0bcff")).append(";\n")
              .append("  --textarea-border: ").append(darkColors.getOrDefault("outline", "#938f99")).append(";\n")
              .append("  --textarea-bg: ").append(darkColors.getOrDefault("surfaceContainerLow", "#1d1b20")).append(";\n")
              .append("  --slider-bg: ").append(darkColors.getOrDefault("secondaryContainer", "#4a4458")).append(";\n")
              .append("}\n");
        }
        
        sb.append("\n/* Custom color theme overrides */\n")
          .append("[data-theme-color=\"purple\"] {\n")
          .append("  --logo-color: #6750A4;\n")
          .append("  --bottom-nav-item-active-color: #21005d;\n")
          .append("  --bottom-nav-active-pill-bg: #e8def8;\n")
          .append("  --settings-section-title-color: #6750A4;\n")
          .append("  --slider-bg: #e8def8;\n")
          .append("  --search-btn-hover: #e8def8;\n")
          .append("  --service-tab-hover-bg: #e8def8;\n")
          .append("}\n")
          .append("[data-theme=\"dark\"][data-theme-color=\"purple\"] {\n")
          .append("  --bg-color: #000000;\n")
          .append("  --header-bg: #121212;\n")
          .append("  --card-bg: #121212;\n")
          .append("  --bottom-nav-bg: #121212;\n")
          .append("  --settings-card-bg: #121212;\n")
          .append("  --textarea-bg: #121212;\n")
          .append("  --logo-color: #d0bcff;\n")
          .append("  --bottom-nav-item-active-color: #e8def8;\n")
          .append("  --bottom-nav-active-pill-bg: #4a4458;\n")
          .append("  --settings-section-title-color: #d0bcff;\n")
          .append("  --slider-bg: #4a4458;\n")
          .append("  --search-btn-hover: #4a4458;\n")
          .append("  --service-tab-hover-bg: #4a4458;\n")
          .append("}\n")
          .append("[data-theme-color=\"green\"] {\n")
          .append("  --logo-color: #2E7D32;\n")
          .append("  --bottom-nav-item-active-color: #1B5E20;\n")
          .append("  --bottom-nav-active-pill-bg: #C8E6C9;\n")
          .append("  --settings-section-title-color: #2E7D32;\n")
          .append("  --slider-bg: #C8E6C9;\n")
          .append("  --search-btn-hover: #C8E6C9;\n")
          .append("  --service-tab-hover-bg: #C8E6C9;\n")
          .append("}\n")
          .append("[data-theme=\"dark\"][data-theme-color=\"green\"] {\n")
          .append("  --bg-color: #000000;\n")
          .append("  --header-bg: #121212;\n")
          .append("  --card-bg: #121212;\n")
          .append("  --bottom-nav-bg: #121212;\n")
          .append("  --settings-card-bg: #121212;\n")
          .append("  --textarea-bg: #121212;\n")
          .append("  --logo-color: #81C784;\n")
          .append("  --bottom-nav-item-active-color: #E8F5E9;\n")
          .append("  --bottom-nav-active-pill-bg: #1B5E20;\n")
          .append("  --settings-section-title-color: #81C784;\n")
          .append("  --slider-bg: #1B5E20;\n")
          .append("  --search-btn-hover: #1B5E20;\n")
          .append("  --service-tab-hover-bg: #1B5E20;\n")
          .append("}\n")
          .append("[data-theme-color=\"blue\"] {\n")
          .append("  --logo-color: #1565C0;\n")
          .append("  --bottom-nav-item-active-color: #0D47A1;\n")
          .append("  --bottom-nav-active-pill-bg: #BBDEFB;\n")
          .append("  --settings-section-title-color: #1565C0;\n")
          .append("  --slider-bg: #BBDEFB;\n")
          .append("  --search-btn-hover: #BBDEFB;\n")
          .append("  --service-tab-hover-bg: #BBDEFB;\n")
          .append("}\n")
          .append("[data-theme=\"dark\"][data-theme-color=\"blue\"] {\n")
          .append("  --bg-color: #000000;\n")
          .append("  --header-bg: #121212;\n")
          .append("  --card-bg: #121212;\n")
          .append("  --bottom-nav-bg: #121212;\n")
          .append("  --settings-card-bg: #121212;\n")
          .append("  --textarea-bg: #121212;\n")
          .append("  --logo-color: #90CAF9;\n")
          .append("  --bottom-nav-item-active-color: #E3F2FD;\n")
          .append("  --bottom-nav-active-pill-bg: #1565C0;\n")
          .append("  --settings-section-title-color: #90CAF9;\n")
          .append("  --slider-bg: #1565C0;\n")
          .append("  --search-btn-hover: #1565C0;\n")
          .append("  --service-tab-hover-bg: #1565C0;\n")
          .append("}\n")
          .append("[data-theme-color=\"orange\"] {\n")
          .append("  --logo-color: #E65100;\n")
          .append("  --bottom-nav-item-active-color: #BF360C;\n")
          .append("  --bottom-nav-active-pill-bg: #FFE0B2;\n")
          .append("  --settings-section-title-color: #E65100;\n")
          .append("  --slider-bg: #FFE0B2;\n")
          .append("  --search-btn-hover: #FFE0B2;\n")
          .append("  --service-tab-hover-bg: #FFE0B2;\n")
          .append("}\n")
          .append("[data-theme=\"dark\"][data-theme-color=\"orange\"] {\n")
          .append("  --bg-color: #000000;\n")
          .append("  --header-bg: #121212;\n")
          .append("  --card-bg: #121212;\n")
          .append("  --bottom-nav-bg: #121212;\n")
          .append("  --settings-card-bg: #121212;\n")
          .append("  --textarea-bg: #121212;\n")
          .append("  --logo-color: #FFB74D;\n")
          .append("  --bottom-nav-item-active-color: #FFF3E0;\n")
          .append("  --bottom-nav-active-pill-bg: #E65100;\n")
          .append("  --settings-section-title-color: #FFB74D;\n")
          .append("  --slider-bg: #E65100;\n")
          .append("  --search-btn-hover: #E65100;\n")
          .append("  --service-tab-hover-bg: #E65100;\n")
          .append("}\n")
          .append("[data-theme-color=\"red\"] {\n")
          .append("  --logo-color: #C62828;\n")
          .append("  --bottom-nav-item-active-color: #B71C1C;\n")
          .append("  --bottom-nav-active-pill-bg: #FFCDD2;\n")
          .append("  --settings-section-title-color: #C62828;\n")
          .append("  --slider-bg: #FFCDD2;\n")
          .append("  --search-btn-hover: #FFCDD2;\n")
          .append("  --service-tab-hover-bg: #FFCDD2;\n")
          .append("}\n")
          .append("[data-theme=\"dark\"][data-theme-color=\"red\"] {\n")
          .append("  --bg-color: #000000;\n")
          .append("  --header-bg: #121212;\n")
          .append("  --card-bg: #121212;\n")
          .append("  --bottom-nav-bg: #121212;\n")
          .append("  --settings-card-bg: #121212;\n")
          .append("  --textarea-bg: #121212;\n")
          .append("  --logo-color: #E57373;\n")
          .append("  --bottom-nav-item-active-color: #FFEBEE;\n")
          .append("  --bottom-nav-active-pill-bg: #C62828;\n")
          .append("  --settings-section-title-color: #E57373;\n")
          .append("  --slider-bg: #C62828;\n")
          .append("  --search-btn-hover: #C62828;\n")
          .append("  --service-tab-hover-bg: #C62828;\n")
          .append("}\n");

        sb.append("\n/* AMOLED Pure Black Override */\n")
          .append("[data-theme=\"dark\"][data-pure-black=\"true\"] {\n")
          .append("  --bg-color: #000000 !important;\n")
          .append("  --header-bg: #121212 !important;\n")
          .append("  --card-bg: #121212 !important;\n")
          .append("  --bottom-nav-bg: #121212 !important;\n")
          .append("  --settings-card-bg: #121212 !important;\n")
          .append("  --textarea-bg: #121212 !important;\n")
          .append("}\n");

        return sb.toString();
    }

    public static String renderShortsPage(int serviceId, boolean isTv) {
        StringBuilder sb = new StringBuilder();
        sb.append("<style>\n")
          .append("  .sidebar-nav { display: none !important; }\n")
          .append("  .bottom-nav { display: none !important; }\n")
          .append("  header { display: none !important; }\n")
          .append("  body { background: var(--bg-color, #000) !important; color: var(--text-color, #fff); }\n")
          .append("  .shorts-viewport { position: fixed; top: 0; left: 0; right: 0; bottom: 0; overflow-y: scroll; scroll-snap-type: y mandatory; scrollbar-width: none; background: var(--bg-color, #0f0d13); z-index: 10; }\n")
          .append("  .shorts-viewport::-webkit-scrollbar { display: none; }\n")
          .append("  .shorts-card { position: relative; width: 100%; height: 100vh; scroll-snap-align: start; scroll-snap-stop: always; display: flex; justify-content: center; align-items: center; background: var(--bg-color, #000); overflow: hidden; cursor: pointer; }\n")
          .append("  .shorts-video { max-width: 100vw; max-height: 100vh; width: auto; height: auto; object-fit: contain; margin: auto; }\n")
          .append("  .shorts-overlay { position: absolute; inset: 0; pointer-events: none; display: flex; flex-direction: column; justify-content: space-between; padding: 16px; background: linear-gradient(180deg, rgba(0,0,0,0.4) 0%, transparent 20%, transparent 60%, rgba(0,0,0,0.85) 100%); z-index: 10; }\n")
          .append("  .shorts-overlay * { pointer-events: auto; }\n")
          .append("  .shorts-top-bar { display: flex; justify-content: space-between; align-items: center; width: 100%; z-index: 2; }\n")
          .append("  .shorts-filter-pill { background: rgba(232, 222, 248, 0.25); backdrop-filter: blur(12px); color: #e6e1e5; padding: 6px 16px; border-radius: 100px; font-size: 13px; font-weight: 500; border: 1px solid rgba(255,255,255,0.2); font-family: 'Roboto', sans-serif; cursor: pointer; transition: background 0.2s; }\n")
          .append("  .shorts-filter-pill:hover { background: rgba(232, 222, 248, 0.4); }\n")
          .append("  .shorts-bottom-info { display: flex; flex-direction: column; gap: 10px; color: #fff; text-shadow: 0 1px 3px rgba(0,0,0,0.8); z-index: 2; margin-right: 64px; }\n")
          .append("  .shorts-author-row { display: flex; align-items: center; gap: 10px; }\n")
          .append("  .shorts-avatar { width: 40px; height: 40px; border-radius: 50%; border: 2px solid var(--logo-color, #6750A4); object-fit: cover; }\n")
          .append("  .shorts-author-name { font-size: 15px; font-weight: 600; color: #fff; text-decoration: none; }\n")
          .append("  .shorts-sub-btn { background: var(--logo-color, #6750A4); color: #fff; padding: 6px 16px; border-radius: 100px; font-size: 13px; font-weight: 600; border: none; cursor: pointer; transition: transform 0.2s; }\n")
          .append("  .shorts-sub-btn:hover { transform: scale(1.05); }\n")
          .append("  .shorts-title { font-size: 14px; font-weight: 400; line-height: 1.4; max-height: 2.8em; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }\n")
          .append("  .shorts-actions { position: absolute; right: 16px; bottom: 32px; display: flex; flex-direction: column; gap: 20px; align-items: center; z-index: 3; }\n")
          .append("  .shorts-action-btn { background: rgba(29, 27, 32, 0.6); backdrop-filter: blur(8px); border-radius: 50%; width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; color: #fff; border: 1px solid rgba(255,255,255,0.15); cursor: pointer; transition: background 0.2s, transform 0.2s; }\n")
          .append("  .shorts-action-btn:hover { background: var(--logo-color, #6750A4); transform: scale(1.1); }\n")
          .append("  .shorts-action-btn.active { color: var(--logo-color, #6750A4) !important; }\n")
          .append("  .shorts-action-label { font-size: 11px; color: #e6e1e5; margin-top: 4px; text-shadow: 0 1px 2px rgba(0,0,0,0.8); font-weight: 500; }\n")
          .append("  .shorts-back-btn { position: fixed; top: 16px; left: 16px; z-index: 100; background: var(--search-input-bg, rgba(29,27,32,0.7)); backdrop-filter: blur(8px); border-radius: 50%; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; color: var(--text-color, #fff); border: 1px solid rgba(255,255,255,0.15); cursor: pointer; text-decoration: none; transition: background 0.2s, color 0.2s; }\n")
          .append("  .shorts-back-btn:hover { background: var(--logo-color, #6750A4); color: #fff; }\n")
          .append("  .shorts-refresh-btn { position: fixed; top: 16px; right: 16px; z-index: 100; background: var(--search-input-bg, rgba(29,27,32,0.7)); backdrop-filter: blur(8px); border-radius: 50%; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; color: var(--text-color, #fff); border: 1px solid rgba(255,255,255,0.15); cursor: pointer; transition: background 0.2s, color 0.2s, transform 0.4s; }\n")
          .append("  .shorts-refresh-btn:hover { background: var(--logo-color, #6750A4); color: #fff; }\n")
          .append("  .shorts-refresh-btn.spinning { animation: spin 0.6s linear; }\n")
          .append("  .shorts-progress-bar-container { position: absolute; bottom: 0; left: 0; right: 0; height: 4px; background: rgba(255,255,255,0.25); z-index: 10; pointer-events: none; }\n")
          .append("  .shorts-progress-bar { height: 100%; width: 0%; background: var(--logo-color, #6750A4); transition: width 0.15s linear; }\n")
          .append("  @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }\n")
          .append("  @keyframes rotate { 100% { transform: rotate(360deg); } }\n")
          .append("</style>\n")
          .append("<a href=\"/\" class=\"shorts-back-btn\" title=\"Back to Home\">\n")
          .append("  <svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"22\" height=\"22\"><path d=\"M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z\"/></svg>\n")
          .append("</a>\n")
          .append("<button class=\"shorts-refresh-btn\" id=\"shorts-refresh-btn\" onclick=\"refreshFeed()\" title=\"Refresh feed\">\n")
          .append("  <svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"22\" height=\"22\"><path d=\"M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z\"/></svg>\n")
          .append("</button>\n")
          .append("<div class=\"shorts-viewport\" id=\"shorts-container\">\n")
          .append("  <div style=\"display:flex; justify-content:center; align-items:center; height:100%; color:var(--text-color, #fff); font-family:'Roboto',sans-serif; font-size:16px;\">\n")
          .append("    <svg class=\"spinner\" viewBox=\"0 0 50 50\" width=\"40\" height=\"40\" style=\"animation:rotate 1.5s linear infinite; margin-right:12px;\"><circle cx=\"25\" cy=\"25\" r=\"20\" fill=\"none\" stroke=\"var(--logo-color, #6750A4)\" stroke-width=\"4\"></circle></svg>\n")
          .append("    Loading your personalised feed...\n")
          .append("  </div>\n")
          .append("</div>\n")
          .append("<script>\n")
          .append("  let shortsQueue = [];\n")
          .append("  let currentPage = 0;\n")
          .append("  let isLoading = false;\n")
          .append("  let noMore = false;\n")
          .append("  let isMuted = true;\n")
          .append("  let activeIndex = 0;\n")
          .append("  async function refreshFeed() {\n")
          .append("    const btn = document.getElementById('shorts-refresh-btn');\n")
          .append("    if (btn) { btn.classList.add('spinning'); }\n")
          .append("    try {\n")
          .append("      await fetch('/api/shorts/refresh?serviceId=").append(serviceId).append("');\n")
          .append("    } catch(e) {}\n")
          .append("    shortsQueue = [];\n")
          .append("    currentPage = 0;\n")
          .append("    isLoading = false;\n")
          .append("    activeIndex = 0;\n")
          .append("    shortObserver = null;\n")
          .append("    const container = document.getElementById('shorts-container');\n")
          .append("    container.innerHTML = '<div style=\"display:flex;justify-content:center;align-items:center;height:100%;color:var(--text-color, #fff);font-family:\\\'Roboto\\',sans-serif;font-size:16px;\"><svg viewBox=\"0 0 50 50\" width=\"40\" height=\"40\" style=\"animation:rotate 1.5s linear infinite;margin-right:12px;\"><circle cx=\"25\" cy=\"25\" r=\"20\" fill=\"none\" stroke=\"var(--logo-color, #6750A4)\" stroke-width=\"4\"></circle></svg>Finding new videos...</div>';\n")
          .append("    setTimeout(() => {\n")
          .append("      if (btn) btn.classList.remove('spinning');\n")
          .append("      fetchShortsFeed();\n")
          .append("    }, 1500);\n")
          .append("  }\n")
          .append("  function showLoadingCard() {\n")
          .append("    const container = document.getElementById('shorts-container');\n")
          .append("    if (!container) return;\n")
          .append("    if (document.getElementById('shorts-loading-card')) return;\n")
          .append("    const card = document.createElement('div');\n")
          .append("    card.className = 'shorts-card';\n")
          .append("    card.id = 'shorts-loading-card';\n")
          .append("    card.innerHTML = `<div style=\"display:flex; flex-direction:column; justify-content:center; align-items:center; height:100%; color:var(--text-color, #fff); font-family:\\\'Roboto\\\',sans-serif; gap:16px;\"><svg class=\"spinner\" viewBox=\"0 0 50 50\" width=\"40\" height=\"40\" style=\"animation:rotate 1.5s linear infinite; margin-bottom:12px;\"><circle cx=\"25\" cy=\"25\" r=\"20\" fill=\"none\" stroke=\"var(--logo-color, #6750A4)\" stroke-width=\"4\"></circle></svg><div style=\"font-size:16px; font-weight:500;\">Loading next shorts...</div></div>`;\n")
          .append("    container.appendChild(card);\n")
          .append("  }\n")
          .append("  function removeLoadingCard() {\n")
          .append("    const card = document.getElementById('shorts-loading-card');\n")
          .append("    if (card) card.remove();\n")
          .append("  }\n")
          .append("  async function fetchShortsFeed() {\n")
          .append("    if (isLoading || noMore) return;\n")
          .append("    isLoading = true;\n")
          .append("    showLoadingCard();\n")
          .append("    try {\n")
          .append("      const res = await fetch('/api/shorts/feed?serviceId=").append(serviceId).append("&page=' + currentPage);\n")
          .append("      const data = await res.json();\n")
          .append("      removeLoadingCard();\n")
          .append("      if (data && data.items && data.items.length > 0) {\n")
          .append("        const container = document.getElementById('shorts-container');\n")
          .append("        const isFirstLoad = (shortsQueue.length === 0);\n")
          .append("        if (isFirstLoad) container.innerHTML = '';\n")
          .append("        data.items.forEach(item => {\n")
          .append("          const idx = shortsQueue.length;\n")
          .append("          shortsQueue.push(item);\n")
          .append("          renderShortCard(container, item, idx);\n")
          .append("        });\n")
          .append("        currentPage++;\n")
          .append("        if (isFirstLoad) { container.scrollTop = 0; }\n")
          .append("        observeNewCards();\n")
          .append("      } else if (data && data.loading) {\n")
          .append("        removeLoadingCard();\n")
          .append("        isLoading = false;\n")
          .append("        setTimeout(fetchShortsFeed, 2000);\n")
          .append("        const container = document.getElementById('shorts-container');\n")
          .append("        if (shortsQueue.length === 0) {\n")
          .append("          container.innerHTML = '<div style=\"display:flex;flex-direction:column;justify-content:center;align-items:center;height:100%;color:var(--text-color, #fff);font-family:\\\'Roboto\\\',sans-serif;gap:16px;\"><svg viewBox=\"0 0 50 50\" width=\"48\" height=\"48\" style=\"animation:rotate 1.5s linear infinite;\"><circle cx=\"25\" cy=\"25\" r=\"20\" fill=\"none\" stroke=\"var(--logo-color, #6750A4)\" stroke-width=\"4\"></circle></svg><div style=\"font-size:16px;font-weight:500;\">Personalising your feed...</div></div>';\n")
          .append("        }\n")
          .append("        return;\n")
          .append("      } else {\n")
          .append("        noMore = true;\n")
          .append("        if (shortsQueue.length === 0) {\n")
          .append("          const container = document.getElementById('shorts-container');\n")
          .append("          container.innerHTML = '<div style=\"display:flex;flex-direction:column;justify-content:center;align-items:center;height:100%;color:var(--text-color, #fff);font-family:\\\'Roboto\\\',sans-serif;gap:16px;\"><div style=\"font-size:18px;font-weight:500;\">No reels found</div><button onclick=\"currentPage=0;noMore=false;fetchShortsFeed();\" style=\"background:var(--logo-color, #6750A4);color:#fff;border:none;padding:10px 24px;border-radius:100px;cursor:pointer;font-size:14px;\">Retry</button></div>';\n")
          .append("        }\n")
          .append("      }\n")
          .append("    } catch(e) {\n")
          .append("      removeLoadingCard();\n")
          .append("      const container = document.getElementById('shorts-container');\n")
          .append("      if (container && shortsQueue.length === 0) {\n")
          .append("        container.innerHTML = '<div style=\"display:flex;flex-direction:column;justify-content:center;align-items:center;height:100%;color:var(--text-color, #fff);font-family:\\\'Roboto\\\',sans-serif;gap:16px;\"><div style=\"font-size:18px;font-weight:500;\">Error loading reels</div><button onclick=\"fetchShortsFeed();\" style=\"background:var(--logo-color, #6750A4);color:#fff;border:none;padding:10px 24px;border-radius:100px;cursor:pointer;font-size:14px;\">Retry</button></div>';\n")
          .append("      }\n")
          .append("    }\n")
          .append("    isLoading = false;\n")
          .append("  }\n")
          .append("  function getAvatarColor(name) {\n")
          .append("    const colors = ['#ff5722','#e91e63','#9c27b0','#3f51b5','#2196f3','#009688','#4caf50','#ff9800'];\n")
          .append("    let h = 0; for (let c of (name||'')) h = (h * 31 + c.charCodeAt(0)) & 0xffffffff;\n")
          .append("    return colors[Math.abs(h) % colors.length];\n")
          .append("  }\n")
          .append("  function toggleMuteGlobal(event) {\n")
          .append("    if (event) event.stopPropagation();\n")
          .append("    isMuted = !isMuted;\n")
          .append("    document.querySelectorAll('.shorts-video').forEach(video => {\n")
          .append("      video.muted = isMuted;\n")
          .append("    });\n")
          .append("    updateMuteUI();\n")
          .append("  }\n")
          .append("  function updateMuteUI() {\n")
          .append("    const muteSvg = isMuted ? \n")
          .append("      '<path d=\"M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.21.05-.42.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z\"/>' :\n")
          .append("      '<path d=\"M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z\"/>';\n")
          .append("    document.querySelectorAll('.mute-btn svg').forEach(svg => { svg.innerHTML = muteSvg; });\n")
          .append("    document.querySelectorAll('.mute-label').forEach(lbl => { lbl.innerText = isMuted ? 'Muted' : 'Unmuted'; });\n")
          .append("  }\n")
          .append("  function togglePlayCard(card) {\n")
          .append("    const video = card.querySelector('video');\n")
          .append("    if (video) {\n")
          .append("      if (video.paused) {\n")
          .append("        video.play().catch(e => console.log('Play failed', e));\n")
          .append("      } else {\n")
          .append("        video.pause();\n")
          .append("      }\n")
          .append("    }\n")
          .append("  }\n")
          .append("  async function toggleSave(btn, url, title, uploader, thumbnail) {\n")
          .append("    const isSaved = btn.classList.contains('active');\n")
          .append("    const action = isSaved ? 'remove' : 'add';\n")
          .append("    btn.classList.toggle('active');\n")
          .append("    let apiUrl = `/watch_later_action?action=${action}&url=${encodeURIComponent(url)}`;\n")
          .append("    if (action === 'add') {\n")
          .append("      apiUrl += `&title=${encodeURIComponent(title)}&uploader=${encodeURIComponent(uploader)}&thumbnail=${encodeURIComponent(thumbnail)}&type=video`;\n")
          .append("    }\n")
          .append("    apiUrl += '&back=/api/shorts/feed';\n")
          .append("    try { await fetch(apiUrl); } catch (e) {}\n")
          .append("  }\n")
          .append("  function renderShortCard(container, item, idx) {\n")
          .append("    const card = document.createElement('div');\n")
          .append("    card.className = 'shorts-card';\n")
          .append("    card.dataset.index = idx;\n")
          .append("    const firstChar = (item.uploaderName || '?').charAt(0).toUpperCase();\n")
          .append("    const avatarColor = getAvatarColor(item.uploaderName);\n")
          .append("    const muteSvg = isMuted ? \n")
          .append("      '<path d=\"M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.21.05-.42.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z\"/>' :\n")
          .append("      '<path d=\"M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z\"/>';\n")
          .append("    card.onclick = function() { togglePlayCard(card); };\n")
          .append("    card.innerHTML = `<video class=\"shorts-video\" id=\"short-video-${idx}\" ontimeupdate=\"const pb = document.getElementById('shorts-progress-bar-${idx}'); if (pb && this.duration) pb.style.width = (this.currentTime / this.duration * 100) + '%';\" loop playsinline preload=\"none\" poster=\"${item.thumbnailUrl || ''}\" muted></video><div class=\"shorts-overlay\"><div class=\"shorts-top-bar\"></div><div class=\"shorts-bottom-info\" onclick=\"event.stopPropagation()\"><div class=\"shorts-author-row\"><div style=\"width:40px;height:40px;border-radius:50%;border:2px solid var(--logo-color, #6750A4);background:${avatarColor};display:flex;align-items:center;justify-content:center;font-weight:700;font-size:16px;color:#fff;flex-shrink:0;\">${firstChar}</div><a href=\"/channel?id=${encodeURIComponent(item.uploaderUrl || '')}\" class=\"shorts-author-name\">${item.uploaderName || 'Creator'}</a><button class=\"shorts-sub-btn\">Subscribe</button></div><div class=\"shorts-title\">${item.name || ''}</div></div><div class=\"shorts-actions\"><div style=\"text-align:center;\" onclick=\"event.stopPropagation()\"><button class=\"shorts-action-btn\" onclick=\"toggleSave(this, '${item.url}', '${item.name || ''}', '${item.uploaderName || ''}', '${item.thumbnailUrl || ''}')\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15l-5-2.18L7 18V5h10v13z\"/></svg></button><div class=\"shorts-action-label\">Save</div></div><div style=\"text-align:center;\" onclick=\"event.stopPropagation()\"><button class=\"shorts-action-btn\" onclick=\"location.href='/watch?id=' + encodeURIComponent('${item.url}')\" title=\"Open in full player\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M19 19H5V5h7V3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z\"/></svg></button><div class=\"shorts-action-label\">Open</div></div><div style=\"text-align:center;\" onclick=\"event.stopPropagation()\"><button class=\"shorts-action-btn mute-btn\" onclick=\"toggleMuteGlobal(event)\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\">${muteSvg}</svg></button><div class=\"shorts-action-label mute-label\">${isMuted ? 'Muted' : 'Unmuted'}</div></div><div style=\"text-align:center;\" onclick=\"event.stopPropagation()\"><button class=\"shorts-action-btn\" onclick=\"shareLink('${item.url}', '${item.name || ''}')\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z\"/></svg></button><div class=\"shorts-action-label\">Share</div></div></div><div class=\"shorts-progress-bar-container\"><div class=\"shorts-progress-bar\" id=\"shorts-progress-bar-${idx}\"></div></div></div>`;\n")
          .append("    container.appendChild(card);\n")
          .append("  }\n")
          .append("  let shortObserver = null;\n")
          .append("  function observeNewCards() {\n")
          .append("    if (!shortObserver) {\n")
          .append("      const options = { root: document.getElementById('shorts-container'), threshold: [0.0, 0.2, 0.4, 0.6, 0.8, 1.0] };\n")
          .append("      shortObserver = new IntersectionObserver((entries) => {\n")
          .append("        let bestEntry = null;\n")
          .append("        let maxRatio = 0.1;\n")
          .append("        entries.forEach(entry => {\n")
          .append("          if (entry.isIntersecting && entry.intersectionRatio > maxRatio) {\n")
          .append("            maxRatio = entry.intersectionRatio;\n")
          .append("            bestEntry = entry;\n")
          .append("          }\n")
          .append("        });\n")
          .append("        if (bestEntry) {\n")
          .append("          const idx = parseInt(bestEntry.target.dataset.index);\n")
          .append("          activeIndex = idx;\n")
          .append("          document.querySelectorAll('.shorts-video').forEach((video, vIdx) => {\n")
          .append("            if (vIdx === idx) {\n")
          .append("              if (!video.src) {\n")
          .append("                video.src = '/stream?id=' + encodeURIComponent(shortsQueue[idx].url);\n")
          .append("              }\n")
          .append("              video.muted = isMuted;\n")
          .append("              video.play().catch(e => console.log('Autoplay blocked', e));\n")
          .append("            } else {\n")
          .append("              video.pause();\n")
          .append("            }\n")
          .append("          });\n")
          .append("          if (idx >= shortsQueue.length - 2) fetchShortsFeed();\n")
          .append("        }\n")
          .append("      }, options);\n")
          .append("    }\n")
          .append("    document.querySelectorAll('.shorts-card:not([data-observed])').forEach(card => {\n")
          .append("      card.setAttribute('data-observed', '1');\n")
          .append("      shortObserver.observe(card);\n")
          .append("    });\n")
          .append("  }\n")
          .append("  let startX = 0, startY = 0;\n")
          .append("  const container = document.getElementById('shorts-container');\n")
          .append("  container.addEventListener('touchstart', (e) => { startX = e.touches[0].clientX; startY = e.touches[0].clientY; }, { passive: true });\n")
          .append("  container.addEventListener('touchend', (e) => {\n")
          .append("    const diffX = e.changedTouches[0].clientX - startX;\n")
          .append("    if (Math.abs(diffX) > 50) {\n")
          .append("      const activeVideo = document.getElementById('short-video-' + activeIndex);\n")
          .append("      if (activeVideo) activeVideo.currentTime += (diffX > 0 ? 10 : -10);\n")
          .append("    }\n")
          .append("  }, { passive: true });\n")
          .append("  document.addEventListener('keydown', (e) => {\n")
          .append("    const cards = document.querySelectorAll('.shorts-card');\n")
          .append("    if (e.key === 'ArrowDown' && activeIndex < cards.length - 1) cards[activeIndex + 1].scrollIntoView({ behavior: 'smooth' });\n")
          .append("    else if (e.key === 'ArrowUp' && activeIndex > 0) cards[activeIndex - 1].scrollIntoView({ behavior: 'smooth' });\n")
          .append("    else if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {\n")
          .append("      const v = document.getElementById('short-video-' + activeIndex);\n")
          .append("      if (v) v.currentTime += (e.key === 'ArrowRight' ? 10 : -10);\n")
          .append("    }\n")
          .append("  });\n")
          .append("  document.addEventListener('DOMContentLoaded', fetchShortsFeed);\n")
          .append("</script>\n");

        return wrapInTemplate("Reels - LocalTube", sb.toString(), isTv);
    }
}
