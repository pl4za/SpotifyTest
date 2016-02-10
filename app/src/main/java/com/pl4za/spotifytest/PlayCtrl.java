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

    public boolean hasInstance() {
        return service != null;
    }

    @Override
    public void initializePlayer() {
        if (service != null)
            service.initializePlayer();
    }

    @Override
    public void startNotification() {
        if (service != null)
            service.startNotification();
    }

    @Override
    public void resumePause() {
        if (service != null) service.resumePause();
    }

    @Override
    public void nextTrack() {
        if (service != null) service.nextTrack();
    }

    @Override
    public void prevTrack() {
        if (service != null)
            service.prevTrack();
    }

    @Override
    public void shuffle() {
        if (service != null)
            service.shuffle();
    }

    @Override
    public void repeat() {
        if (service != null)
            service.repeat();
    }

    @Override
    public void addToQueue(String trackUri) {
        if (service != null)
            service.addToQueue(trackUri);
    }

    @Override
    public void addToQueue(List<String> queue, int listStart) {
        if (service != null)
            service.addToQueue(queue, listStart);
    }

    @Override
    public boolean isActive() {
        return service != null && service.isActive();
    }

    @Override
    public void destroyPlayer() {
        if (service != null) {
            service.destroyPlayer();
        }
    }

    @Override
    public void clearQueue() {
        if (service != null)
            service.clearQueue();
    }

    @Override
    public boolean isPlaying() {
        if (service==null) {
            return false;
        }
        return service.isPlaying();
    }
}
