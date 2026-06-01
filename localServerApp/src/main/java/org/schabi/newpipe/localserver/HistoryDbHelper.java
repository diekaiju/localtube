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

import java.util.ArrayList;
import java.util.List;

public class HistoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "history.db";
    private static final int DATABASE_VERSION = 2;

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
}
