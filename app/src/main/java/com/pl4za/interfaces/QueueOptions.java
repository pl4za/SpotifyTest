package com.pl4za.interfaces;

import com.pl4za.spotlight.Track;

import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public interface QueueOptions extends TracklistOptions {

    void updateTrackNumberAndPlayingTrack(String trackURI);

    List<Track> getTrackList();

    List<String> getTrackURIList(List<Track> queueToList);

    Track getCurrentTrack();

    int getQueuePosition();

    boolean isQueueChanged();

    void setQueueChanged(boolean changed);

    int getTrackNumberUpdate();

    boolean hasNext();

    boolean hasPrevious();

}
