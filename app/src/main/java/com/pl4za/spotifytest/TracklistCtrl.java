package com.pl4za.spotifytest;

import com.pl4za.interfaces.TracklistOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public class TracklistCtrl implements TracklistOptions {

    private static List<Track> trackList;

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

}
