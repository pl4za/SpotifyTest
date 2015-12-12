package com.pl4za.interfaces;

/**
 * Created by Admin on 31/03/2015.
 */
public interface IspotifyPlayerOptions {

    String actionPlayPause = "com.example.spotifytest.action.playpause";
    String actionNext = "com.example.spotifytest.action.next";
    String actionDismiss = "com.example.spotifytest.action.dismiss";
    void initializePlayer();
    void startNotification();
}
