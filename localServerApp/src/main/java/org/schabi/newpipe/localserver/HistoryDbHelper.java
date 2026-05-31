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
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_HISTORY = "watch_history";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_URL = "url";
    private static final String KEY_UPLOADER = "uploader";
    private static final String KEY_THUMBNAIL = "thumbnail_url";
    private static final String KEY_TIMESTAMP = "timestamp";

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
        String CREATE_TABLE = "CREATE TABLE " + TABLE_HISTORY + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TITLE + " TEXT,"
                + KEY_URL + " TEXT UNIQUE,"
                + KEY_UPLOADER + " TEXT,"
                + KEY_THUMBNAIL + " TEXT,"
                + KEY_TIMESTAMP + " INTEGER"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
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
}
