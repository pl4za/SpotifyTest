package com.pl4za.interfaces;

import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public interface ServiceOptions {

    void initializePlayer();

    void startNotification();

    void resumePause();

    void nextTrack();

    void prevTrack();

    void shuffle();

    void repeat();

    void addToQueue(String trackUri);

    void addToQueue(List<String> queue, int listStart);

    boolean isActive();
}
