package org.schabi.newpipe.localserver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;

import java.util.ArrayList;
import java.util.List;

public class HistoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "history.db";
    private static final int DATABASE_VERSION = 8;

    private static final String TABLE_HISTORY = "watch_history";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_URL = "url";
    private static final String KEY_UPLOADER = "uploader";
    private static final String KEY_THUMBNAIL = "thumbnail_url";
    private static final String KEY_TIMESTAMP = "timestamp";

    private static final String TABLE_CACHED = "cached_videos";
    private static final String KEY_THUMBNAIL_LOCAL = "thumbnail_local_path";
    private static final String KEY_FILE_LOCAL = "video_local_path";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_STATUS = "status";
    private static final String KEY_PROGRESS = "progress";

    private static final String TABLE_SETTINGS = "filter_settings";
    private static final String KEY_SETTING_KEY = "setting_key";
    private static final String KEY_SETTING_VALUE = "setting_value";

    private static final String TABLE_SUBSCRIPTIONS = "subscriptions";
    private static final String KEY_CHANNEL_URL = "channel_url";
    private static final String KEY_CHANNEL_NAME = "channel_name";
    private static final String KEY_CHANNEL_AVATAR = "channel_avatar";

    private static final String TABLE_BOOKMARKED_PLAYLISTS = "bookmarked_playlists";
    private static final String KEY_PLAYLIST_URL = "playlist_url";
    private static final String KEY_PLAYLIST_NAME = "playlist_name";
    private static final String KEY_PLAYLIST_UPLOADER = "playlist_uploader";

    private static final String TABLE_WATCH_LATER = "watch_later";
    private static final String KEY_WATCH_LATER_TYPE = "watch_later_type"; // "video" or "playlist"

    private static final String TABLE_SEARCH_HISTORY = "search_history";
    private static final String KEY_QUERY = "search_query";

    private static HistoryDbHelper instance;

    public static synchronized HistoryDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private HistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TITLE + " TEXT,"
                + KEY_URL + " TEXT UNIQUE,"
                + KEY_UPLOADER + " TEXT,"
                + KEY_THUMBNAIL + " TEXT,"
                + KEY_TIMESTAMP + " INTEGER"
                + ")";
        db.execSQL(CREATE_HISTORY_TABLE);

        String CREATE_CACHED_TABLE = "CREATE TABLE " + TABLE_CACHED + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TITLE + " TEXT,"
                + KEY_URL + " TEXT UNIQUE,"
                + KEY_UPLOADER + " TEXT,"
                + KEY_THUMBNAIL_LOCAL + " TEXT,"
                + KEY_FILE_LOCAL + " TEXT,"
                + KEY_DESCRIPTION + " TEXT,"
                + KEY_STATUS + " TEXT,"
                + KEY_PROGRESS + " INTEGER DEFAULT 0,"
                + KEY_TIMESTAMP + " INTEGER"
                + ")";
        db.execSQL(CREATE_CACHED_TABLE);

        String CREATE_SETTINGS_TABLE = "CREATE TABLE " + TABLE_SETTINGS + "("
                + KEY_SETTING_KEY + " TEXT PRIMARY KEY,"
                + KEY_SETTING_VALUE + " TEXT"
                + ")";
        db.execSQL(CREATE_SETTINGS_TABLE);

        String CREATE_SUBSCRIPTIONS_TABLE = "CREATE TABLE " + TABLE_SUBSCRIPTIONS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_CHANNEL_URL + " TEXT UNIQUE,"
                + KEY_CHANNEL_NAME + " TEXT,"
                + KEY_CHANNEL_AVATAR + " TEXT,"
                + KEY_TIMESTAMP + " INTEGER"
                + ")";
        db.execSQL(CREATE_SUBSCRIPTIONS_TABLE);

        String CREATE_PLAYLISTS_TABLE = "CREATE TABLE " + TABLE_BOOKMARKED_PLAYLISTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_PLAYLIST_URL + " TEXT UNIQUE,"
                + KEY_PLAYLIST_NAME + " TEXT,"
                + KEY_PLAYLIST_UPLOADER + " TEXT,"
                + KEY_TIMESTAMP + " INTEGER"
                + ")";
        db.execSQL(CREATE_PLAYLISTS_TABLE);

        String CREATE_WATCH_LATER_TABLE = "CREATE TABLE " + TABLE_WATCH_LATER + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_URL + " TEXT UNIQUE,"
                + KEY_TITLE + " TEXT,"
                + KEY_UPLOADER + " TEXT,"
                + KEY_THUMBNAIL + " TEXT,"
                + KEY_WATCH_LATER_TYPE + " TEXT,"
                + KEY_TIMESTAMP + " INTEGER"
                + ")";
        db.execSQL(CREATE_WATCH_LATER_TABLE);

        String CREATE_SEARCH_HISTORY_TABLE = "CREATE TABLE " + TABLE_SEARCH_HISTORY + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_QUERY + " TEXT UNIQUE,"
                + KEY_TIMESTAMP + " INTEGER"
                + ")";
        db.execSQL(CREATE_SEARCH_HISTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String CREATE_CACHED_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CACHED + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_TITLE + " TEXT,"
                    + KEY_URL + " TEXT UNIQUE,"
                    + KEY_UPLOADER + " TEXT,"
                    + KEY_THUMBNAIL_LOCAL + " TEXT,"
                    + KEY_FILE_LOCAL + " TEXT,"
                    + KEY_DESCRIPTION + " TEXT,"
                    + KEY_STATUS + " TEXT,"
                    + KEY_PROGRESS + " INTEGER DEFAULT 0,"
                    + KEY_TIMESTAMP + " INTEGER"
                    + ")";
            db.execSQL(CREATE_CACHED_TABLE);
        }
        if (oldVersion < 3) {
            String CREATE_SETTINGS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + "("
                    + KEY_SETTING_KEY + " TEXT PRIMARY KEY,"
                    + KEY_SETTING_VALUE + " TEXT"
                    + ")";
            db.execSQL(CREATE_SETTINGS_TABLE);
        }
        if (oldVersion < 4) {
            String CREATE_SUBSCRIPTIONS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SUBSCRIPTIONS + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_CHANNEL_URL + " TEXT UNIQUE,"
                    + KEY_CHANNEL_NAME + " TEXT,"
                    + KEY_CHANNEL_AVATAR + " TEXT,"
                    + KEY_TIMESTAMP + " INTEGER"
                    + ")";
            db.execSQL(CREATE_SUBSCRIPTIONS_TABLE);
        }
        if (oldVersion < 5) {
            String CREATE_PLAYLISTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_BOOKMARKED_PLAYLISTS + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_PLAYLIST_URL + " TEXT UNIQUE,"
                    + KEY_PLAYLIST_NAME + " TEXT,"
                    + KEY_PLAYLIST_UPLOADER + " TEXT,"
                    + KEY_TIMESTAMP + " INTEGER"
                    + ")";
            db.execSQL(CREATE_PLAYLISTS_TABLE);
        }
        if (oldVersion < 6) {
            String CREATE_WATCH_LATER_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_WATCH_LATER + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_URL + " TEXT UNIQUE,"
                    + KEY_TITLE + " TEXT,"
                    + KEY_UPLOADER + " TEXT,"
                    + KEY_THUMBNAIL + " TEXT,"
                    + KEY_WATCH_LATER_TYPE + " TEXT,"
                    + KEY_TIMESTAMP + " INTEGER"
                    + ")";
            db.execSQL(CREATE_WATCH_LATER_TABLE);
        }
        if (oldVersion < 7) {
            String CREATE_SEARCH_HISTORY_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SEARCH_HISTORY + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_QUERY + " TEXT UNIQUE,"
                    + KEY_TIMESTAMP + " INTEGER"
                    + ")";
            db.execSQL(CREATE_SEARCH_HISTORY_TABLE);
        }
        if (oldVersion < 8) {
            try {
                Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SUBSCRIPTIONS, null);
                List<ContentValues> tempSubs = new ArrayList<>();
                if (cursor != null && cursor.moveToFirst()) {
                    int urlIdx = cursor.getColumnIndex(KEY_CHANNEL_URL);
                    int nameIdx = cursor.getColumnIndex(KEY_CHANNEL_NAME);
                    int avatarIdx = cursor.getColumnIndex(KEY_CHANNEL_AVATAR);
                    int timeIdx = cursor.getColumnIndex(KEY_TIMESTAMP);
                    do {
                        String url = urlIdx != -1 ? cursor.getString(urlIdx) : "";
                        String name = nameIdx != -1 ? cursor.getString(nameIdx) : "";
                        String avatar = avatarIdx != -1 ? cursor.getString(avatarIdx) : "";
                        long timestamp = timeIdx != -1 ? cursor.getLong(timeIdx) : System.currentTimeMillis();

                        String normalizedUrl = normalizeChannelUrl(url);
                        ContentValues values = new ContentValues();
                        values.put(KEY_CHANNEL_URL, normalizedUrl);
                        values.put(KEY_CHANNEL_NAME, name);
                        values.put(KEY_CHANNEL_AVATAR, avatar);
                        values.put(KEY_TIMESTAMP, timestamp);
                        tempSubs.add(values);
                    } while (cursor.moveToNext());
                    cursor.close();
                }

                db.execSQL("DELETE FROM " + TABLE_SUBSCRIPTIONS);
                for (ContentValues values : tempSubs) {
                    db.insertWithOnConflict(TABLE_SUBSCRIPTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveToHistory(String title, String url, String uploader, String thumbnailUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title);
        values.put(KEY_URL, url);
        values.put(KEY_UPLOADER, uploader);
        values.put(KEY_THUMBNAIL, thumbnailUrl);
        values.put(KEY_TIMESTAMP, System.currentTimeMillis());

        db.insertWithOnConflict(TABLE_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<InfoItem> getHistory() {
        List<InfoItem> historyList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                int titleIdx = cursor.getColumnIndex(KEY_TITLE);
                int urlIdx = cursor.getColumnIndex(KEY_URL);
                int uploaderIdx = cursor.getColumnIndex(KEY_UPLOADER);
                int thumbIdx = cursor.getColumnIndex(KEY_THUMBNAIL);

                do {
                    String title = titleIdx != -1 ? cursor.getString(titleIdx) : "";
                    String url = urlIdx != -1 ? cursor.getString(urlIdx) : "";
                    String uploader = uploaderIdx != -1 ? cursor.getString(uploaderIdx) : "";
                    String thumbUrl = thumbIdx != -1 ? cursor.getString(thumbIdx) : "";

                    StreamInfoItem item = new StreamInfoItem(0, url, title, StreamType.VIDEO_STREAM);
                    item.setUploaderName(uploader);
                    item.setUploaderUrl("");
                    if (thumbUrl != null && !thumbUrl.isEmpty()) {
                        item.setThumbnails(List.of(new Image(thumbUrl, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN)));
                    }
                    historyList.add(item);
                } while (cursor.moveToNext());
            }
        }
        return historyList;
    }

    public void addCachedVideo(String url, String title, String uploader, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title);
        values.put(KEY_URL, url);
        values.put(KEY_UPLOADER, uploader);
        values.put(KEY_DESCRIPTION, description);
        values.put(KEY_STATUS, "PENDING");
        values.put(KEY_PROGRESS, 0);
        values.put(KEY_TIMESTAMP, System.currentTimeMillis());

        db.insertWithOnConflict(TABLE_CACHED, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void updateCachedVideoMetadata(String url, String title, String uploader, String description, String thumbnailPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title);
        values.put(KEY_UPLOADER, uploader);
        values.put(KEY_DESCRIPTION, description);
        if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
            values.put(KEY_THUMBNAIL_LOCAL, thumbnailPath);
        }
        db.update(TABLE_CACHED, values, KEY_URL + " = ?", new String[]{url});
    }

    public void updateCachedVideoProgress(String url, String status, int progress, String videoPath, String thumbnailPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_STATUS, status);
        values.put(KEY_PROGRESS, progress);
        if (videoPath != null && !videoPath.isEmpty()) {
            values.put(KEY_FILE_LOCAL, videoPath);
        }
        if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
            values.put(KEY_THUMBNAIL_LOCAL, thumbnailPath);
        }
        db.update(TABLE_CACHED, values, KEY_URL + " = ?", new String[]{url});
    }

    public CachedVideo getCachedVideo(String url) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_CACHED + " WHERE " + KEY_URL + " = ?";
        try (Cursor cursor = db.rawQuery(selectQuery, new String[]{url})) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(KEY_ID);
                int titleIdx = cursor.getColumnIndex(KEY_TITLE);
                int uploaderIdx = cursor.getColumnIndex(KEY_UPLOADER);
                int thumbIdx = cursor.getColumnIndex(KEY_THUMBNAIL_LOCAL);
                int fileIdx = cursor.getColumnIndex(KEY_FILE_LOCAL);
                int descIdx = cursor.getColumnIndex(KEY_DESCRIPTION);
                int statusIdx = cursor.getColumnIndex(KEY_STATUS);
                int progressIdx = cursor.getColumnIndex(KEY_PROGRESS);
                int timeIdx = cursor.getColumnIndex(KEY_TIMESTAMP);

                int id = idIdx != -1 ? cursor.getInt(idIdx) : 0;
                String title = titleIdx != -1 ? cursor.getString(titleIdx) : "";
                String uploader = uploaderIdx != -1 ? cursor.getString(uploaderIdx) : "";
                String thumbnail = thumbIdx != -1 ? cursor.getString(thumbIdx) : "";
                String file = fileIdx != -1 ? cursor.getString(fileIdx) : "";
                String description = descIdx != -1 ? cursor.getString(descIdx) : "";
                String status = statusIdx != -1 ? cursor.getString(statusIdx) : "";
                int progress = progressIdx != -1 ? cursor.getInt(progressIdx) : 0;
                long timestamp = timeIdx != -1 ? cursor.getLong(timeIdx) : 0;

                return new CachedVideo(id, title, url, uploader, thumbnail, file, description, status, progress, timestamp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<CachedVideo> getCachedVideos() {
        List<CachedVideo> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_CACHED + " ORDER BY " + KEY_TIMESTAMP + " DESC";
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(KEY_ID);
                int titleIdx = cursor.getColumnIndex(KEY_TITLE);
                int urlIdx = cursor.getColumnIndex(KEY_URL);
                int uploaderIdx = cursor.getColumnIndex(KEY_UPLOADER);
                int thumbIdx = cursor.getColumnIndex(KEY_THUMBNAIL_LOCAL);
                int fileIdx = cursor.getColumnIndex(KEY_FILE_LOCAL);
                int descIdx = cursor.getColumnIndex(KEY_DESCRIPTION);
                int statusIdx = cursor.getColumnIndex(KEY_STATUS);
                int progressIdx = cursor.getColumnIndex(KEY_PROGRESS);
                int timeIdx = cursor.getColumnIndex(KEY_TIMESTAMP);

                do {
                    int id = idIdx != -1 ? cursor.getInt(idIdx) : 0;
                    String title = titleIdx != -1 ? cursor.getString(titleIdx) : "";
                    String url = urlIdx != -1 ? cursor.getString(urlIdx) : "";
                    String uploader = uploaderIdx != -1 ? cursor.getString(uploaderIdx) : "";
                    String thumbnail = thumbIdx != -1 ? cursor.getString(thumbIdx) : "";
                    String file = fileIdx != -1 ? cursor.getString(fileIdx) : "";
                    String description = descIdx != -1 ? cursor.getString(descIdx) : "";
                    String status = statusIdx != -1 ? cursor.getString(statusIdx) : "";
                    int progress = progressIdx != -1 ? cursor.getInt(progressIdx) : 0;
                    long timestamp = timeIdx != -1 ? cursor.getLong(timeIdx) : 0;

                    list.add(new CachedVideo(id, title, url, uploader, thumbnail, file, description, status, progress, timestamp));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void deleteCachedVideo(String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CACHED, KEY_URL + " = ?", new String[]{url});
    }

    public void setSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SETTING_KEY, key);
        values.put(KEY_SETTING_VALUE, value);
        db.insertWithOnConflict(TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getSetting(String key, String defaultValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT " + KEY_SETTING_VALUE + " FROM " + TABLE_SETTINGS + " WHERE " + KEY_SETTING_KEY + " = ?";
        try (Cursor cursor = db.rawQuery(selectQuery, new String[]{key})) {
            if (cursor.moveToFirst()) {
                int valIdx = cursor.getColumnIndex(KEY_SETTING_VALUE);
                return valIdx != -1 ? cursor.getString(valIdx) : defaultValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public boolean getHideWatched() {
        return "true".equals(getSetting("hide_watched", "false"));
    }

    public boolean getHideShorts() {
        return "true".equals(getSetting("hide_shorts", "false"));
    }

    public String getVideoQuality() {
        return getSetting("video_quality", "360p");
    }

    public String getHomeFeedMode() {
        return getSetting("home_feed_mode", "mix");
    }


    public List<String> getBlockedKeywords() {
        String val = getSetting("blocked_keywords", "");
        List<String> list = new ArrayList<>();
        if (!val.isEmpty()) {
            for (String s : val.split("\n")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    public List<String> getBlockedChannels() {
        String val = getSetting("blocked_channels", "");
        List<String> list = new ArrayList<>();
        if (!val.isEmpty()) {
            for (String s : val.split("\n")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    public List<String> getPreferredKeywords() {
        String val = getSetting("preferred_keywords", "");
        List<String> list = new ArrayList<>();
        if (!val.isEmpty()) {
            for (String s : val.split("\n")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    public static String normalizeChannelUrl(String url) {
        if (url == null) return null;
        String normalized = url.trim();
        if (normalized.contains("m.youtube.com")) {
            normalized = normalized.replace("m.youtube.com", "www.youtube.com");
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public void addSubscription(String channelUrl, String channelName, String channelAvatar) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CHANNEL_URL, normalizeChannelUrl(channelUrl));
        values.put(KEY_CHANNEL_NAME, channelName);
        if (channelAvatar != null) {
            channelAvatar = channelAvatar.trim();
            if (channelAvatar.startsWith("//")) {
                channelAvatar = "https:" + channelAvatar;
            }
        }
        values.put(KEY_CHANNEL_AVATAR, channelAvatar);
        values.put(KEY_TIMESTAMP, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_SUBSCRIPTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removeSubscription(String channelUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SUBSCRIPTIONS, KEY_CHANNEL_URL + " = ?", new String[]{normalizeChannelUrl(channelUrl)});
    }

    public boolean isSubscribed(String channelUrl) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT 1 FROM " + TABLE_SUBSCRIPTIONS + " WHERE " + KEY_CHANNEL_URL + " = ?";
        try (Cursor cursor = db.rawQuery(selectQuery, new String[]{normalizeChannelUrl(channelUrl)})) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<InfoItem> getSubscriptions() {
        List<InfoItem> subList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SUBSCRIPTIONS + " ORDER BY " + KEY_CHANNEL_NAME + " ASC";
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                int urlIdx = cursor.getColumnIndex(KEY_CHANNEL_URL);
                int nameIdx = cursor.getColumnIndex(KEY_CHANNEL_NAME);
                int avatarIdx = cursor.getColumnIndex(KEY_CHANNEL_AVATAR);

                do {
                    String url = urlIdx != -1 ? cursor.getString(urlIdx) : "";
                    String name = nameIdx != -1 ? cursor.getString(nameIdx) : "";
                    String avatar = avatarIdx != -1 ? cursor.getString(avatarIdx) : "";
                    if (avatar != null) {
                        avatar = avatar.trim();
                        if (avatar.startsWith("//")) {
                            avatar = "https:" + avatar;
                        }
                    }

                    ChannelInfoItem item = new ChannelInfoItem(0, url, name);
                    if (avatar != null && !avatar.isEmpty()) {
                        item.setThumbnails(List.of(new Image(avatar, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN)));
                    }
                    subList.add(item);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subList;
    }

    public void addPlaylistBookmark(String playlistUrl, String playlistName, String playlistUploader) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PLAYLIST_URL, playlistUrl);
        values.put(KEY_PLAYLIST_NAME, playlistName);
        values.put(KEY_PLAYLIST_UPLOADER, playlistUploader);
        values.put(KEY_TIMESTAMP, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_BOOKMARKED_PLAYLISTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removePlaylistBookmark(String playlistUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BOOKMARKED_PLAYLISTS, KEY_PLAYLIST_URL + " = ?", new String[]{playlistUrl});
    }

    public boolean isPlaylistBookmarked(String playlistUrl) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT 1 FROM " + TABLE_BOOKMARKED_PLAYLISTS + " WHERE " + KEY_PLAYLIST_URL + " = ?";
        try (Cursor cursor = db.rawQuery(selectQuery, new String[]{playlistUrl})) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<InfoItem> getBookmarkedPlaylists() {
        List<InfoItem> list = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_BOOKMARKED_PLAYLISTS + " ORDER BY " + KEY_PLAYLIST_NAME + " ASC";
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                int urlIdx = cursor.getColumnIndex(KEY_PLAYLIST_URL);
                int nameIdx = cursor.getColumnIndex(KEY_PLAYLIST_NAME);
                int uploaderIdx = cursor.getColumnIndex(KEY_PLAYLIST_UPLOADER);

                do {
                    String url = urlIdx != -1 ? cursor.getString(urlIdx) : "";
                    String name = nameIdx != -1 ? cursor.getString(nameIdx) : "";
                    String uploader = uploaderIdx != -1 ? cursor.getString(uploaderIdx) : "";

                    PlaylistInfoItem item = new PlaylistInfoItem(0, url, name);
                    item.setUploaderName(uploader);
                    // Add generic fallback thumbnail for playlists
                    item.setThumbnails(List.of(new Image("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&auto=format&fit=crop", Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN)));
                    list.add(item);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public String exportToJson() {
        try {
            org.json.JSONObject backup = new org.json.JSONObject();
            SQLiteDatabase db = this.getReadableDatabase();

            // 1. watch_history
            org.json.JSONArray historyArr = new org.json.JSONArray();
            try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_HISTORY, null)) {
                if (cursor.moveToFirst()) {
                    do {
                        org.json.JSONObject row = new org.json.JSONObject();
                        row.put(KEY_TITLE, cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)));
                        row.put(KEY_URL, cursor.getString(cursor.getColumnIndexOrThrow(KEY_URL)));
                        row.put(KEY_UPLOADER, cursor.getString(cursor.getColumnIndexOrThrow(KEY_UPLOADER)));
                        row.put(KEY_THUMBNAIL, cursor.getString(cursor.getColumnIndexOrThrow(KEY_THUMBNAIL)));
                        row.put(KEY_TIMESTAMP, cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP)));
                        historyArr.put(row);
                    } while (cursor.moveToNext());
                }
            }
            backup.put(TABLE_HISTORY, historyArr);

            // 2. cached_videos
            org.json.JSONArray cachedArr = new org.json.JSONArray();
            try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CACHED, null)) {
                if (cursor.moveToFirst()) {
                    do {
                        org.json.JSONObject row = new org.json.JSONObject();
                        row.put(KEY_TITLE, cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)));
                        row.put(KEY_URL, cursor.getString(cursor.getColumnIndexOrThrow(KEY_URL)));
                        row.put(KEY_UPLOADER, cursor.getString(cursor.getColumnIndexOrThrow(KEY_UPLOADER)));
                        row.put(KEY_THUMBNAIL_LOCAL, cursor.getString(cursor.getColumnIndexOrThrow(KEY_THUMBNAIL_LOCAL)));
                        row.put(KEY_FILE_LOCAL, cursor.getString(cursor.getColumnIndexOrThrow(KEY_FILE_LOCAL)));
                        row.put(KEY_DESCRIPTION, cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)));
                        row.put(KEY_STATUS, cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)));
                        row.put(KEY_PROGRESS, cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROGRESS)));
                        row.put(KEY_TIMESTAMP, cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP)));
                        cachedArr.put(row);
                    } while (cursor.moveToNext());
                }
            }
            backup.put(TABLE_CACHED, cachedArr);

            // 3. filter_settings
            org.json.JSONArray settingsArr = new org.json.JSONArray();
            try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SETTINGS, null)) {
                if (cursor.moveToFirst()) {
                    do {
                        org.json.JSONObject row = new org.json.JSONObject();
                        row.put(KEY_SETTING_KEY, cursor.getString(cursor.getColumnIndexOrThrow(KEY_SETTING_KEY)));
                        row.put(KEY_SETTING_VALUE, cursor.getString(cursor.getColumnIndexOrThrow(KEY_SETTING_VALUE)));
                        settingsArr.put(row);
                    } while (cursor.moveToNext());
                }
            }
            backup.put(TABLE_SETTINGS, settingsArr);

            // 4. subscriptions
            org.json.JSONArray subsArr = new org.json.JSONArray();
            try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SUBSCRIPTIONS, null)) {
                if (cursor.moveToFirst()) {
                    do {
                        org.json.JSONObject row = new org.json.JSONObject();
                        row.put(KEY_CHANNEL_URL, cursor.getString(cursor.getColumnIndexOrThrow(KEY_CHANNEL_URL)));
                        row.put(KEY_CHANNEL_NAME, cursor.getString(cursor.getColumnIndexOrThrow(KEY_CHANNEL_NAME)));
                        row.put(KEY_CHANNEL_AVATAR, cursor.getString(cursor.getColumnIndexOrThrow(KEY_CHANNEL_AVATAR)));
                        row.put(KEY_TIMESTAMP, cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP)));
                        subsArr.put(row);
                    } while (cursor.moveToNext());
                }
            }
            backup.put(TABLE_SUBSCRIPTIONS, subsArr);

            // 5. bookmarked_playlists
            org.json.JSONArray playlistsArr = new org.json.JSONArray();
            try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BOOKMARKED_PLAYLISTS, null)) {
                if (cursor.moveToFirst()) {
                    do {
                        org.json.JSONObject row = new org.json.JSONObject();
                        row.put(KEY_PLAYLIST_URL, cursor.getString(cursor.getColumnIndexOrThrow(KEY_PLAYLIST_URL)));
                        row.put(KEY_PLAYLIST_NAME, cursor.getString(cursor.getColumnIndexOrThrow(KEY_PLAYLIST_NAME)));
                        row.put(KEY_PLAYLIST_UPLOADER, cursor.getString(cursor.getColumnIndexOrThrow(KEY_PLAYLIST_UPLOADER)));
                        row.put(KEY_TIMESTAMP, cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP)));
                        playlistsArr.put(row);
                    } while (cursor.moveToNext());
                }
            }
            backup.put(TABLE_BOOKMARKED_PLAYLISTS, playlistsArr);

            return backup.toString(2);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    public boolean importFromJson(String jsonString) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            org.json.JSONObject backup = new org.json.JSONObject(jsonString);

            // 1. watch_history
            if (backup.has(TABLE_HISTORY)) {
                org.json.JSONArray arr = backup.getJSONArray(TABLE_HISTORY);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject row = arr.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put(KEY_TITLE, row.optString(KEY_TITLE));
                    values.put(KEY_URL, row.optString(KEY_URL));
                    values.put(KEY_UPLOADER, row.optString(KEY_UPLOADER));
                    values.put(KEY_THUMBNAIL, row.optString(KEY_THUMBNAIL));
                    values.put(KEY_TIMESTAMP, row.optLong(KEY_TIMESTAMP));
                    db.insertWithOnConflict(TABLE_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 2. cached_videos
            if (backup.has(TABLE_CACHED)) {
                org.json.JSONArray arr = backup.getJSONArray(TABLE_CACHED);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject row = arr.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put(KEY_TITLE, row.optString(KEY_TITLE));
                    values.put(KEY_URL, row.optString(KEY_URL));
                    values.put(KEY_UPLOADER, row.optString(KEY_UPLOADER));
                    values.put(KEY_THUMBNAIL_LOCAL, row.optString(KEY_THUMBNAIL_LOCAL));
                    values.put(KEY_FILE_LOCAL, row.optString(KEY_FILE_LOCAL));
                    values.put(KEY_DESCRIPTION, row.optString(KEY_DESCRIPTION));
                    values.put(KEY_STATUS, row.optString(KEY_STATUS));
                    values.put(KEY_PROGRESS, row.optInt(KEY_PROGRESS));
                    values.put(KEY_TIMESTAMP, row.optLong(KEY_TIMESTAMP));
                    db.insertWithOnConflict(TABLE_CACHED, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 3. filter_settings
            if (backup.has(TABLE_SETTINGS)) {
                org.json.JSONArray arr = backup.getJSONArray(TABLE_SETTINGS);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject row = arr.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put(KEY_SETTING_KEY, row.optString(KEY_SETTING_KEY));
                    values.put(KEY_SETTING_VALUE, row.optString(KEY_SETTING_VALUE));
                    db.insertWithOnConflict(TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 4. subscriptions
            if (backup.has(TABLE_SUBSCRIPTIONS)) {
                org.json.JSONArray arr = backup.getJSONArray(TABLE_SUBSCRIPTIONS);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject row = arr.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put(KEY_CHANNEL_URL, row.optString(KEY_CHANNEL_URL));
                    values.put(KEY_CHANNEL_NAME, row.optString(KEY_CHANNEL_NAME));
                    values.put(KEY_CHANNEL_AVATAR, row.optString(KEY_CHANNEL_AVATAR));
                    values.put(KEY_TIMESTAMP, row.optLong(KEY_TIMESTAMP));
                    db.insertWithOnConflict(TABLE_SUBSCRIPTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 5. bookmarked_playlists
            if (backup.has(TABLE_BOOKMARKED_PLAYLISTS)) {
                org.json.JSONArray arr = backup.getJSONArray(TABLE_BOOKMARKED_PLAYLISTS);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject row = arr.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put(KEY_PLAYLIST_URL, row.optString(KEY_PLAYLIST_URL));
                    values.put(KEY_PLAYLIST_NAME, row.optString(KEY_PLAYLIST_NAME));
                    values.put(KEY_PLAYLIST_UPLOADER, row.optString(KEY_PLAYLIST_UPLOADER));
                    values.put(KEY_TIMESTAMP, row.optLong(KEY_TIMESTAMP));
                    db.insertWithOnConflict(TABLE_BOOKMARKED_PLAYLISTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public void addWatchLater(String url, String title, String uploader, String thumbnailUrl, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_URL, url);
        values.put(KEY_TITLE, title);
        values.put(KEY_UPLOADER, uploader);
        values.put(KEY_THUMBNAIL, thumbnailUrl);
        values.put(KEY_WATCH_LATER_TYPE, type);
        values.put(KEY_TIMESTAMP, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_WATCH_LATER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removeWatchLater(String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WATCH_LATER, KEY_URL + " = ?", new String[]{url});
    }

    public boolean isWatchLater(String url) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT 1 FROM " + TABLE_WATCH_LATER + " WHERE " + KEY_URL + " = ?";
        try (Cursor cursor = db.rawQuery(selectQuery, new String[]{url})) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<InfoItem> getWatchLaterItems() {
        List<InfoItem> list = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_WATCH_LATER + " ORDER BY " + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                int urlIdx = cursor.getColumnIndex(KEY_URL);
                int titleIdx = cursor.getColumnIndex(KEY_TITLE);
                int uploaderIdx = cursor.getColumnIndex(KEY_UPLOADER);
                int thumbIdx = cursor.getColumnIndex(KEY_THUMBNAIL);
                int typeIdx = cursor.getColumnIndex(KEY_WATCH_LATER_TYPE);

                do {
                    String url = urlIdx != -1 ? cursor.getString(urlIdx) : "";
                    String title = titleIdx != -1 ? cursor.getString(titleIdx) : "";
                    String uploader = uploaderIdx != -1 ? cursor.getString(uploaderIdx) : "";
                    String thumbnail = thumbIdx != -1 ? cursor.getString(thumbIdx) : "";
                    String type = typeIdx != -1 ? cursor.getString(typeIdx) : "video";

                    if ("playlist".equalsIgnoreCase(type)) {
                        PlaylistInfoItem item = new PlaylistInfoItem(0, url, title);
                        item.setUploaderName(uploader);
                        if (thumbnail != null && !thumbnail.isEmpty()) {
                            item.setThumbnails(List.of(new Image(thumbnail, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN)));
                        }
                        list.add(item);
                    } else {
                        StreamInfoItem item = new StreamInfoItem(0, url, title, StreamType.VIDEO_STREAM);
                        item.setUploaderName(uploader);
                        item.setUploaderUrl("");
                        if (thumbnail != null && !thumbnail.isEmpty()) {
                            item.setThumbnails(List.of(new Image(thumbnail, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN)));
                        }
                        list.add(item);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_QUERY, query);
        values.put(KEY_TIMESTAMP, System.currentTimeMillis());
        db.replace(TABLE_SEARCH_HISTORY, null, values);
    }

    public List<String> getSearchHistory() {
        List<String> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SEARCH_HISTORY, new String[]{KEY_QUERY}, null, null, null, null, KEY_TIMESTAMP + " DESC", "10");
            if (cursor != null && cursor.moveToFirst()) {
                int queryIdx = cursor.getColumnIndex(KEY_QUERY);
                do {
                    if (queryIdx != -1) {
                        history.add(cursor.getString(queryIdx));
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return history;
    }

    public void deleteSearchQuery(String query) {
        if (query == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SEARCH_HISTORY, KEY_QUERY + "=?", new String[]{query.trim()});
    }
}
