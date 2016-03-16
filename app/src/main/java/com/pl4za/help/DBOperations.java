package com.pl4za.help;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pl4za.spotlight.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jason on 16-Mar-16.
 */
public class DBOperations {

    DatabaseAdapter dbAdapter = null;

    public DBOperations(DatabaseAdapter db) {
        this.dbAdapter = db;
    }

    public void dropDB() {
        dbAdapter.dropDB();
    }

    public void addTrack(Track track, String playlistID) {
        SQLiteDatabase db = dbAdapter.getWritableDB();
        ContentValues values = new ContentValues();
        values.put(DatabaseAdapter.DBTable.COLUMN_NAME_PLAYLIST_ID, playlistID);
        values.put(DatabaseAdapter.DBTable.COLUMN_NAME_TRACK_ALBUM, track.getAlbum());
        values.put(DatabaseAdapter.DBTable.COLUMN_NAME_TRACK_ID, track.getID());
        values.put(DatabaseAdapter.DBTable.COLUMN_NAME_TRACK_ART, track.getAlbumArt());
        values.put(DatabaseAdapter.DBTable.COLUMN_NAME_ENTRY_TIME, track.getTime());
        values.put(DatabaseAdapter.DBTable.COLUMN_NAME_ADDED, track.getAdded());
        values.put(DatabaseAdapter.DBTable.COLUMN_NAME_TRACK_ART_BIG, track.getBigAlbumArt());
        String artists = "";
        for (int i = 0; i < track.getArtist().length; i++) {
            if (i == 0) {
                artists = track.getArtist()[i];
            } else {
                artists = artists + "," + track.getArtist()[i];
            }
        }
        values.put(DatabaseAdapter.DBTable.COLUMN_NAME_TRACK_ARTIST, artists);
        values.put(DatabaseAdapter.DBTable.COLUMN_NAME_TRACK_ID, track.getID());
        values.put(DatabaseAdapter.DBTable.COLUMN_NAME_TRACK_NAME, track.getTrack());
        values.put(DatabaseAdapter.DBTable.COLUMN_NAME_TRACK_URI, track.getTrackURI());
        // Inserting Row
        db.insertWithOnConflict(DatabaseAdapter.DBTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close(); // Closing database connection
    }

    // Getting All Contacts
    public List<Track> getAllTracks(String playlistID) {
        List<Track> tracklist = new ArrayList<Track>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " +
                DatabaseAdapter.DBTable.TABLE_NAME +
                " WHERE " +
                DatabaseAdapter.DBTable.COLUMN_NAME_PLAYLIST_ID +
                " LIKE '" +
                playlistID +
                "'";
        SQLiteDatabase db = dbAdapter.getReadableDB();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Track track = new Track();
                track.setTrack(cursor.getString(1));
                track.setArtist(cursor.getString(2).split(","));
                track.setID(cursor.getString(3));
                track.setTime(cursor.getString(4));
                track.setAlbum(cursor.getString(5));
                track.setAdded(cursor.getString(6));
                track.setTrackURI(cursor.getString(7));
                track.setAlbumArt(cursor.getString(8));
                track.setBigAlbumArt(cursor.getString(9));
                tracklist.add(track);
            } while (cursor.moveToNext());
        }
        // return contact list
        return tracklist;
    }
}
