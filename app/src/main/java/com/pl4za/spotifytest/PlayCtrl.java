package com.pl4za.spotifytest;

import com.pl4za.interfaces.ServiceOptions;

import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public class PlayCtrl implements ServiceOptions {

    private ServiceOptions service;
    private static final PlayCtrl INSTANCE = new PlayCtrl();

    private PlayCtrl() {
    }

    public void setService(ServiceOptions service) {
        this.service = service;
    }

    public static PlayCtrl getInstance() {
        return INSTANCE;
    }

    @Override
    public void initializePlayer() {
        service.initializePlayer();
    }

    @Override
    public void startNotification() {
        service.startNotification();
    }

    @Override
    public void resumePause() {
        service.resumePause();
    }

    @Override
    public void nextTrack() {
        service.nextTrack();
    }

    @Override
    public void prevTrack() {
        service.prevTrack();
    }

    @Override
    public void shuffle() {
        service.shuffle();
    }

    @Override
    public void repeat() {
        service.repeat();
    }

    @Override
    public void addToQueue(String trackUri) {

    }

    @Override
    public void addToQueue(List<String> queue, int listStart) {

    }

    @Override
    public boolean isActive() {
        return service!=null && service.isActive();
    }
}
