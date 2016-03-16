package com.pl4za.interfaces;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public interface PlaylistOptions extends TracklistOptions {

    String getPlaylistName();

    String getPlaylistID();

    void setPlaylistName(String tracklistName);

    void setPlaylistID(String trackListID);
}
