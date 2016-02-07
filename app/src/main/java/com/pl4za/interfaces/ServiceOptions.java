package com.pl4za.interfaces;

import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public interface ServiceOptions {

    String actionPlayPause = "com.example.spotifytest.action.playpause";
    String actionNext = "com.example.spotifytest.action.next";
    String actionDismiss = "com.example.spotifytest.action.dismiss";
    String CLIENT_ID = "6d4eddf8161c434994500be4a48cab9b";

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

    void destroyPlayer();

    void clearQueue();

    boolean isPlaying();
}
