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
            "body { font-family: 'Roboto', sans-serif; background-color: var(--bg-color); color: var(--text-color); -webkit-font-smoothing: antialiased; transition: background-color 0.2s, color 0.2s; }\n" +
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
            ".card { display: flex; flex-direction: column; cursor: pointer; background-color: var(--card-bg); border-radius: 24px; padding: 12px; border: 1px solid var(--card-border); transition: transform 0.2s, box-shadow 0.2s; }\n" +
            ".card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }\n" +
            ".card-thumbnail { width: 100%; aspect-ratio: 16/9; background-color: var(--card-thumbnail-bg); object-fit: cover; border-radius: 16px; transition: border-radius 0.2s; }\n" +
            ".card-details { display: flex; gap: 12px; padding: 12px 0 0 0; }\n" +
            ".card-avatar { width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; color: white; font-size: 15px; flex-shrink: 0; }\n" +
            ".card-info { display: flex; flex-direction: column; flex-grow: 1; min-width: 0; }\n" +
            ".card-title { font-size: 15px; font-weight: 500; line-height: 1.4; max-height: 2.8em; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin-bottom: 4px; color: var(--card-title-color); }\n" +
            ".card-meta { font-size: 13px; color: var(--card-meta-color); display: flex; flex-direction: column; gap: 2px; }\n" +
            ".card-uploader { font-weight: 500; color: var(--card-meta-color); text-decoration: none; }\n" +
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
            "  #mobile-theme-row { display: flex !important; }\n" +
            "  .search-form { max-width: 40px; margin: 0; overflow: hidden; transition: max-width 0.3s ease; border-radius: 40px; }\n" +
            "  .search-form.search-active { max-width: 100%; width: 100%; margin-left: 8px; }\n" +
            "  .search-input { width: 0; padding: 0; border: none; transition: width 0.3s ease, opacity 0.3s ease; opacity: 0; }\n" +
            "  .search-form.search-active .search-input { width: calc(100% - 48px); padding: 0 16px; border: 1px solid var(--search-input-border); opacity: 1; }\n" +
            "  .search-btn { border-radius: 40px; border: none; background: transparent; }\n" +
            "  .search-form.search-active .search-btn { border-radius: 0 40px 40px 0; border: 1px solid var(--search-input-border); border-left: none; background-color: var(--search-btn-bg); }\n" +
            "  .top-bar.search-active div:first-child, .top-bar.search-active div:last-child { display: none !important; }\n" +
            "  .uploader-profile { display: grid !important; grid-template-columns: 40px 1fr auto; grid-template-areas: 'avatar info sub' 'actions actions actions'; gap: 8px 12px; align-items: center; }\n" +
            "  .uploader-avatar { grid-area: avatar; }\n" +
            "  .uploader-info { grid-area: info; min-width: 0; }\n" +
            "  .uploader-profile > .subscribe-btn:not(#cache-btn) { grid-area: sub; margin-left: 0 !important; margin-right: 0 !important; padding: 6px 16px !important; font-size: 13px !important; }\n" +
            "  .uploader-profile > #cache-btn { grid-area: actions; width: 100%; margin-top: 4px; padding: 8px 16px !important; font-size: 13px !important; }\n" +
            "}\n" +
            ".player-container { display: flex; flex-direction: column; gap: 20px; margin-top: 16px; }\n" +
            ".player-layout { display: flex; flex-direction: column; gap: 24px; }\n" +
            "@media (min-width: 1024px) {\n" +
            "  .player-layout { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }\n" +
            "}\n" +
            ".main-content { display: flex; flex-direction: column; gap: 16px; }\n" +
            ".sidebar { display: flex; flex-direction: column; gap: 16px; }\n" +
            ".native-player { width: 100%; aspect-ratio: 16/9; border-radius: 12px; background-color: #000; outline: none; }\n" +
            ".media-info { padding: 16px 0; border-bottom: 1px solid var(--media-info-border); }\n" +
            ".media-title { font-size: 20px; font-weight: 700; margin-bottom: 8px; color: var(--media-title-color); line-height: 1.4; }\n" +
            ".media-stats { font-size: 14px; color: var(--media-stats-color); margin-bottom: 12px; }\n" +
            ".uploader-profile { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }\n" +
            ".uploader-avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; }\n" +
            ".uploader-info { display: flex; flex-direction: column; flex-grow: 1; min-width: 0; }\n" +
            ".uploader-name { font-size: 16px; font-weight: 500; color: var(--uploader-name-color); text-decoration: none; }\n" +
            ".uploader-subs { font-size: 12px; color: var(--uploader-subs-color); }\n" +
            ".subscribe-btn { padding: 8px 24px; border-radius: 100px; font-size: 14px; font-weight: 500; border: none; cursor: pointer; text-decoration: none; text-align: center; color: #ffffff; background-color: #6750A4; }\n" +
            ".settings-card { background-color: var(--settings-card-bg); border-radius: 24px; padding: 24px; border: 1px solid var(--settings-card-border); box-shadow: 0 4px 12px rgba(0,0,0,0.02); }\n" +
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
            ".media-description { font-size: 14px; line-height: 1.5; color: var(--media-desc-color); white-space: pre-wrap; background-color: var(--media-desc-bg); padding: 12px; border-radius: 12px; border: 1px solid var(--media-desc-border); margin-top: 12px; }\n" +
            ".comments-section { padding-top: 16px; }\n" +
            ".comment-count { font-size: 16px; font-weight: 500; margin-bottom: 16px; color: var(--comment-count-color); }\n" +
            ".comment { display: flex; gap: 12px; margin-bottom: 16px; }\n" +
            ".comment-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; background-color: var(--card-thumbnail-bg); }\n" +
            ".comment-details { display: flex; flex-direction: column; gap: 4px; }\n" +
            ".comment-header { display: flex; gap: 8px; align-items: center; }\n" +
            ".comment-author { font-size: 13px; font-weight: 500; color: var(--comment-author-color); }\n" +
            ".comment-time { font-size: 12px; color: var(--comment-time-color); }\n" +
            ".comment-text { font-size: 14px; line-height: 1.4; color: var(--comment-text-color); white-space: pre-wrap; }\n" +
            ".channel-header { background-color: var(--channel-header-bg); border-radius: 12px; overflow: hidden; margin-bottom: 24px; border: 1px solid var(--channel-header-border); }\n" +
            ".channel-banner { width: 100%; height: 160px; object-fit: cover; background: #272727; }\n" +
            ".channel-details { display: flex; padding: 16px; align-items: center; gap: 16px; flex-wrap: wrap; }\n" +
            ".channel-avatar { width: 80px; height: 80px; border-radius: 50%; object-fit: cover; }\n" +
            ".channel-info-block { display: flex; flex-direction: column; gap: 4px; flex-grow: 1; }\n" +
            ".channel-name { font-size: 24px; font-weight: 700; color: var(--channel-name-color); }\n" +
            ".channel-desc { font-size: 14px; color: var(--channel-desc-color); max-width: 600px; margin-top: 8px; line-height: 1.4; }\n" +
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
            ".search-suggestion-delete:hover { background-color: rgba(204,0,0,0.1); }";

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
          .append("  <a href=\"/subscriptions\" class=\"bottom-nav-item ").append(subsActive).append("\">\n")
          .append("    <span class=\"bottom-nav-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M10 14.65v-5.3L15 12l-5 2.65zM19 4H5v1h14V4zm2 2H3v1h18V6zm2 2H1v12h22V8z\"/></svg></span>\n")
          .append("    <span>Subscriptions</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"/history\" class=\"bottom-nav-item ").append(histActive).append("\">\n")
          .append("    <span class=\"bottom-nav-icon\"><svg viewBox=\"0 0 24 24\" fill=\"currentColor\" width=\"24\" height=\"24\"><path d=\"M11.9 21.1c-4.3 0-8-3.1-8.7-7.4-.1-.7-.1-1.4 0-2.1.8-4.4 4.6-7.5 9-7.5H13V2l5.3 4.2-5.3 4.2V8.1H12.2c-3.1 0-5.7 2.2-6.2 5.2-.1.5-.1 1 0 1.5.5 3 3.1 5.2 6.2 5.2 3.5 0 6.3-2.8 6.3-6.3h2c0 4.6-3.7 8.2-8.6 8.2zm-.4-12.6h1v4.8l4.2 2.5-.5.9-4.7-2.8z\"/></svg></span>\n")
          .append("    <span>History</span>\n")
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
                "    <style>\n" + CSS + "\n" +
                "        /* Glow border outline style for selected/focused interactive elements */\n" +
                "        a:focus, button:focus, input:focus, select:focus, textarea:focus, [tabindex=\"0\"]:focus {\n" +
                "            outline: none !important;\n" +
                "            box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.35), 0 0 15px rgba(219, 39, 119, 0.6) !important;\n" +
                "            border-color: #7c3aed !important;\n" +
                "            transition: all 0.2s ease-in-out !important;\n" +
                "        }\n" +
                "    </style>\n" +
                "    <script>\n" +
                "        (function() {\n" +
                "            const theme = localStorage.getItem('theme') || 'light';\n" +
                "            document.documentElement.setAttribute('data-theme', theme);\n" +
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
                sb.append("        <div style=\"width:100%; border-radius:12px; overflow:hidden; background:#000;\">\n")
                  .append("          <video id=\"player\" class=\"video-js vjs-default-skin vjs-big-play-centered\" controls autoplay preload=\"auto\" style=\"width:100%; height:auto; aspect-ratio:16/9; display:block;\" poster=\"/thumbnail?id=").append(encodeUrl(info.getUrl())).append("\">\n")
                  .append("            <source src=\"/stream?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUrl())).append("\" type=\"video/mp4\">\n")
                  .append(trackTags.toString())
                  .append("          </video>\n")
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
                  .append("            })();\n")
                  .append("        </script>\n");
            } else {
                sb.append("        <div style=\"width:100%; border-radius:12px; overflow:hidden; background:#000;\">\n")
                  .append("          <video id=\"player\" class=\"video-js vjs-default-skin vjs-big-play-centered\" controls autoplay preload=\"auto\" style=\"width:100%; height:auto; aspect-ratio:16/9; display:block;\">\n")
                  .append("            <source src=\"/manifest?serviceId=").append(serviceId).append("&id=").append(encodeUrl(info.getUrl())).append("\" type=\"application/dash+xml\">\n")
                  .append(trackTags.toString())
                  .append("            Your browser does not support HTML5 video.\n")
                  .append("          </video>\n")
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

        String uploaderAvatar = getThumbnailUrl(info.getUploaderAvatars());
        if (isSubscribed) {
            sb.append("            <a href=\"/subscribe?action=unsubscribe&id=").append(encodeUrl(info.getUploaderUrl())).append("&back=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleSubscribe(event, this, '").append(escapeJs(info.getUploaderUrl())).append("', '").append(escapeJs(info.getUploaderName())).append("', '").append(escapeJs(uploaderAvatar)).append("')\" class=\"subscribe-btn subscribed\" style=\"margin-left:auto; margin-right:8px;\">Subscribed</a>\n");
        } else {
            sb.append("            <a href=\"/subscribe?action=subscribe&id=").append(encodeUrl(info.getUploaderUrl())).append("&name=").append(encodeUrl(info.getUploaderName())).append("&avatar=").append(encodeUrl(uploaderAvatar)).append("&back=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleSubscribe(event, this, '").append(escapeJs(info.getUploaderUrl())).append("', '").append(escapeJs(info.getUploaderName())).append("', '").append(escapeJs(uploaderAvatar)).append("')\" class=\"subscribe-btn\" style=\"margin-left:auto; margin-right:8px;\">Subscribe</a>\n");
        }

        if (cachedVideo == null) {
            sb.append("            <a id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" href=\"/cache?action=add&id=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleCache(event, this, '").append(escapeJs(info.getUrl())).append("', ").append(serviceId).append(")\" class=\"subscribe-btn\" style=\"background-color:var(--service-tab-bg); color:var(--text-color); margin-left:0;\">Download</a>\n");
        } else if ("COMPLETED".equals(cachedVideo.getStatus())) {
            sb.append("            <a id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" href=\"/cache?action=delete&id=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleCache(event, this, '").append(escapeJs(info.getUrl())).append("', ").append(serviceId).append(")\" class=\"subscribe-btn\" style=\"background-color:#c00c0c; color:#ffffff; margin-left:0;\">Delete Download</a>\n");
        } else if ("DOWNLOADING".equals(cachedVideo.getStatus()) || "PENDING".equals(cachedVideo.getStatus())) {
            sb.append("            <span id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" class=\"subscribe-btn\" style=\"background-color:var(--service-tab-bg); color:var(--text-color); cursor:default; pointer-events:none; margin-left:0;\">Downloading (").append(cachedVideo.getProgress()).append("%)</span>\n");
        } else if ("FAILED".equals(cachedVideo.getStatus())) {
            sb.append("            <a id=\"cache-btn\" data-url=\"").append(escapeJs(info.getUrl())).append("\" href=\"/cache?action=add&id=").append(encodeUrl(info.getUrl())).append("\" onclick=\"toggleCache(event, this, '").append(escapeJs(info.getUrl())).append("', ").append(serviceId).append(")\" class=\"subscribe-btn\" style=\"background-color:#c00c0c; color:#ffffff; margin-left:0;\">Retry Download</a>\n");
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
          .append("        <h3 style=\"font-size: 16px; font-weight: 700; margin-bottom: 16px;\">Related Content</h3>\n");
        for (InfoItem related : info.getRelatedItems()) {
            String uploader = "";
            String metaText = "";
            if (related instanceof StreamInfoItem) {
                StreamInfoItem stream = (StreamInfoItem) related;
                uploader = stream.getUploaderName();
                metaText = (stream.getViewCount() >= 0 ? stream.getViewCount() + " views" : "Live") + " • " + (stream.getTextualUploadDate() != null ? stream.getTextualUploadDate() : "");
            } else {
                uploader = related.getName();
            }
            if (uploader == null) uploader = "";

            sb.append("        <div class=\"card\" style=\"margin-bottom:8px; flex-direction:row; gap:8px; height:94px; background:transparent; border:none; box-shadow:none;\">\n")
              .append("          <a href=\"/watch?serviceId=").append(serviceId).append("&id=").append(related.getUrl()).append("\" style=\"flex-shrink:0; width:168px; height:94px; border-radius:8px; overflow:hidden; background:var(--card-thumbnail-bg);\">\n")
              .append("            <img src=\"").append(getThumbnailUrl(related.getThumbnails())).append("\" style=\"width:100%; height:100%; object-fit:cover;\">\n")
              .append("          </a>\n")
              .append("          <div class=\"card-details\" style=\"padding:0; display:flex; flex-direction:column; justify-content:flex-start; min-width:0; flex-grow:1;\">\n")
              .append("            <a href=\"/watch?serviceId=").append(serviceId).append("&id=").append(related.getUrl()).append("\" class=\"card-title\" style=\"font-size:14px; font-weight:500; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; line-height:1.2; margin-bottom:4px;\">")
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
                    clickUrl = "/watch?serviceId=" + serviceId + "&id=" + item.getUrl();
                    break;
            }

            if (item.getInfoType() == org.schabi.newpipe.extractor.InfoItem.InfoType.CHANNEL) {
                sb.append("    <div class=\"card\" style=\"flex-direction:row; align-items:center; gap:24px; padding:16px 0;\">\n")
                  .append("      <a href=\"").append(clickUrl).append("\" style=\"flex-shrink:0;\">\n")
                  .append("        <img class=\"card-thumbnail\" src=\"").append(getThumbnailUrl(item.getThumbnails())).append("\" style=\"width:100px; height:100px; border-radius:50%; aspect-ratio:1/1;\">\n")
                  .append("      </a>\n")
                  .append("      <div class=\"card-info\">\n")
                  .append("        <a href=\"").append(clickUrl).append("\" class=\"card-title\" style=\"font-size:18px; margin-bottom:4px;\">").append(item.getName()).append("</a>\n")
                  .append("        <div class=\"card-meta\">\n")
                  .append("          <span class=\"card-uploader\">").append(item.getName()).append("</span>\n")
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
                  .append("            <span>👁️ ").append(stream.getViewCount() >= 0 ? stream.getViewCount() + " views" : "Live / Dynamic").append(" • ")
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

    public static String renderSettings(int serviceId, String currentQuality, boolean hideWatched, boolean hideShorts, boolean saved, boolean isTv) {
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
          .append("          <select name=\"video_quality\" style=\"padding: 8px 16px; border-radius: 8px; border: 1px solid var(--search-input-border); background-color: var(--bg-color); color: var(--text-color); font-family: inherit; font-size: 14px; outline: none; cursor: pointer;\">\n");

        String[] qualities = {"144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p"};
        for (String q : qualities) {
            String selected = q.equals(currentQuality) ? "selected" : "";
            sb.append("            <option value=\"").append(q).append("\" ").append(selected).append(">").append(q).append("</option>\n");
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
          .append("            <span class=\"setting-label\">Hide Shorts</span>\n")
          .append("            <span class=\"setting-desc\">Hide vertical videos shorter than 2 minutes.</span>\n")
          .append("          </div>\n")
          .append("          <label class=\"switch\">\n")
          .append("            <input type=\"checkbox\" name=\"hide_shorts\" value=\"on\" ").append(hideShorts ? "checked" : "").append(">\n")
          .append("            <span class=\"slider\"></span>\n")
          .append("          </label>\n")
          .append("        </div>\n")
          .append("        <div class=\"setting-row\" id=\"mobile-theme-row\" style=\"display: none;\">\n")
          .append("          <div class=\"setting-label-group\">\n")
          .append("            <span class=\"setting-label\">Dark Theme</span>\n")
          .append("            <span class=\"setting-desc\">Toggle between dark and light appearance.</span>\n")
          .append("          </div>\n")
          .append("          <label class=\"switch\">\n")
          .append("            <input type=\"checkbox\" id=\"settings-theme-toggle\">\n")
          .append("            <span class=\"slider\"></span>\n")
          .append("          </label>\n")
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
}
