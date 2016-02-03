package com.pl4za.interfaces;

import com.pl4za.spotifytest.Track;

import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public interface FragmentOptions {

    void updateView();

    void updateFilter(String query);

    void setList(List<Track> list);

    void onSwipe(int position);

    void onDoubleClick(int position);

    void loadTracks(String userID, String playlistID);
}
