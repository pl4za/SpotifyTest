package com.pl4za.help;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by jason on 16-Mar-16.
 */
public class DatabaseAdapter extends SQLiteOpenHelper {

    /**
     * Created by jason on 16-Mar-16.
     */
    public DatabaseAdapter(Context context) {
        super(context, DBTable.DATABASE_NAME, null, DBTable.DATABASE_VERSION);
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + DBTable.TABLE_NAME + "(" +
                    DBTable.COLUMN_NAME_PLAYLIST_ID + TEXT_TYPE + COMMA_SEP +
                    DBTable.COLUMN_NAME_TRACK_NAME + TEXT_TYPE + COMMA_SEP +
                    DBTable.COLUMN_NAME_TRACK_ARTIST + TEXT_TYPE + COMMA_SEP +
                    DBTable.COLUMN_NAME_TRACK_ID + TEXT_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    DBTable.COLUMN_NAME_ENTRY_TIME + TEXT_TYPE + COMMA_SEP +
                    DBTable.COLUMN_NAME_TRACK_ALBUM + TEXT_TYPE + COMMA_SEP +
                    DBTable.COLUMN_NAME_ADDED + TEXT_TYPE + COMMA_SEP +
                    DBTable.COLUMN_NAME_TRACK_URI + TEXT_TYPE + COMMA_SEP +
                    DBTable.COLUMN_NAME_TRACK_ART + TEXT_TYPE + COMMA_SEP +
                    DBTable.COLUMN_NAME_TRACK_ART_BIG + TEXT_TYPE +
                    ")";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + DBTable.TABLE_NAME;

    public SQLiteDatabase getWritableDB() {
        return this.getWritableDatabase();
    }

    public SQLiteDatabase getReadableDB() {
        return this.getReadableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DBTable.TABLE_NAME);
        // Create tables again
        onCreate(db);
    }

    public void dropDB() {
        this.getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + DBTable.TABLE_NAME);
    }

    /* Inner class that defines the table contents */
    public static abstract class DBTable implements BaseColumns {

        private DBTable() {
        }

        // Database Version
        protected static final int DATABASE_VERSION = 1;
        // Database Name
        protected static final String DATABASE_NAME = "spotlight";
        // Table Name
        protected static final String TABLE_NAME = "tracks";
        // Rows
        protected static final String COLUMN_NAME_PLAYLIST_ID = "playlist_id";
        protected static final String COLUMN_NAME_TRACK_NAME = "track_name";
        protected static final String COLUMN_NAME_TRACK_ARTIST = "track_artist";
        protected static final String COLUMN_NAME_TRACK_ID = "track_id";
        protected static final String COLUMN_NAME_ENTRY_TIME = "track_time";
        protected static final String COLUMN_NAME_TRACK_ALBUM = "track_album";
        protected static final String COLUMN_NAME_ADDED = "track_added";
        protected static final String COLUMN_NAME_TRACK_URI = "track_uri";
        protected static final String COLUMN_NAME_TRACK_ART = "track_art";
        protected static final String COLUMN_NAME_TRACK_ART_BIG = "track_art_big";
    }
}
