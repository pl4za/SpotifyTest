package com.pl4za.interfaces;

import com.pl4za.spotifytest.Track;

import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public interface QueueOptions {

    void addToQueue(Track track);

    void addToQueue(List<Track> queue, int listStart);

    void removeFromQueue(int position);

    void clearQueue();

    boolean hasTracks();

    boolean hasNext();

    boolean hasPrevious();

    void updateTrackNumberAndPlayingTrack(String trackURI);

    List<Track> getQueue();

    List<String> getQueueURIList(List<Track> queueToList);

    Track getCurrentTrack();

    int getQueuePosition();

    boolean isQueueChanged();

    void setQueueChanged(boolean changed);
}
