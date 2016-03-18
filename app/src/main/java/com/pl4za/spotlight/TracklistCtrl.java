package com.pl4za.spotlight;

import com.pl4za.interfaces.PlaylistOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public class TracklistCtrl implements PlaylistOptions {

    private static List<Track> TRACK_LIST = Collections.synchronizedList(new ArrayList<Track>());
    private String trackListName;
    private String trackListID;

    private static final TracklistCtrl INSTANCE = new TracklistCtrl();

    private TracklistCtrl() {
        TRACK_LIST = new ArrayList<>();
    }

    public static TracklistCtrl getInstance() {
        return INSTANCE;
    }

    @Override
    public void addTrack(Track track) {
        TRACK_LIST.add(track);
    }

    @Override
    public void addTrack(int position, Track track) {
        TRACK_LIST.add(position, track);
    }

    @Override
    public void addTrackList(List<Track> queue, int listStart) {
        TRACK_LIST.addAll(queue);
    }

    @Override
    public void setTrackList(List<Track> newTrackList) {
        TRACK_LIST = newTrackList;
    }

    @Override
    public void removeFromList(int position) {
        TRACK_LIST.remove(position);
    }

    @Override
    public void clear() {
        TRACK_LIST.clear();
    }

    @Override
    public boolean hasTracks() {
        return !TRACK_LIST.isEmpty();
    }

    @Override
    public Track getTrack(int position) {
        return TRACK_LIST.get(position);
    }

    @Override
    public List<Track> getTrackList() {
        return TRACK_LIST;
    }

    @Override
    public String getPlaylistName() {
        return trackListName;
    }

    @Override
    public String getPlaylistID() {
        return trackListID;
    }

    @Override
    public void setPlaylistName(String tracklistName) {
        this.trackListName = tracklistName;
    }

    @Override
    public void setPlaylistID(String trackListID) {
        this.trackListID = trackListID;
    }

}
