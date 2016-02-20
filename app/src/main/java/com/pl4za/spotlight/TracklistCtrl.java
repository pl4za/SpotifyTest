package com.pl4za.spotlight;

import com.pl4za.interfaces.PlaylistOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public class TracklistCtrl implements PlaylistOptions {

    private static List<Track> trackList;
    private String trackListName;

    private static final TracklistCtrl INSTANCE = new TracklistCtrl();

    private TracklistCtrl() {
        trackList = new ArrayList<>();
    }

    public static TracklistCtrl getInstance() {
        return INSTANCE;
    }

    @Override
    public void addTrack(Track track) {
        trackList.add(track);
    }

    @Override
    public void addTrack(int position, Track track) {
        trackList.add(position, track);
    }

    @Override
    public void addTrackList(List<Track> queue, int listStart) {
        trackList.addAll(queue);
    }

    @Override
    public void setTrackList(List<Track> newTrackList) {
        trackList=newTrackList;
    }

    @Override
    public void removeFromList(int position) {
        trackList.remove(position);
    }

    @Override
    public void clear() {
        trackList.clear();
    }

    @Override
    public boolean hasTracks() {
        return !trackList.isEmpty();
    }

    @Override
    public Track getTrack(int position) {
        return trackList.get(position);
    }

    @Override
    public List<Track> getTrackList() {
        return trackList;
    }

    @Override
    public String getPlaylistName() {
        return trackListName;
    }

    @Override
    public void setPlaylistName(String tracklistName) {
        this.trackListName = tracklistName;
    }

}
