package com.pl4za.interfaces;

import com.pl4za.spotlight.Track;

import java.util.List;

/**
 * Created by jason on 10-Mar-16.
 */
public interface TracklistOptions {

    void addTrack(Track track);

    void addTrack(int position, Track track);

    void addTrackList(List<Track> tracklist, int listStart);

    Track getTrack(int position) ;

    void removeFromList(int position);

    void clear();

    boolean hasTracks();

    void setTrackList(List<Track> newTrackList);

    List<Track> getTrackList();
}
