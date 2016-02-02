package com.pl4za.interfaces;

/**
 * Created by Admin on 31/03/2015.
 */
public interface SpotifyPlayerOptions {

    String actionPlayPause = "com.example.spotifytest.action.playpause";
    String actionNext = "com.example.spotifytest.action.next";
    String actionDismiss = "com.example.spotifytest.action.dismiss";
    String CLIENT_ID = "6d4eddf8161c434994500be4a48cab9b";

    void initializePlayer();

    void startNotification();
}
