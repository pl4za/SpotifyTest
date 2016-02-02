package com.pl4za.spotifytest;

import android.util.Log;

import com.pl4za.interfaces.QueueOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public class QueueCtrl implements QueueOptions {

    private Queue queue;
    private PlayCtrl playCtrl = PlayCtrl.getInstance();

    private static final QueueCtrl INSTANCE = new QueueCtrl();

    private QueueCtrl() {
        queue = new Queue();
    }

    public static QueueCtrl getInstance() {
        return INSTANCE;
    }

    @Override
    public void addToQueue(Track track) {
        queue.addToQueue(track);
        playCtrl.addToQueue(track.getTrackURI());
    }

    @Override
    public void addToQueue(List<Track> tracklist, int listStart) {
        queue.addToQueue(tracklist.subList(listStart, tracklist.size()));
        playCtrl.addToQueue(getQueueURIList(tracklist), listStart);
    }

    @Override
    public void removeFromQueue(int position) {
        queue.removeFromQueue(position);
    }

    @Override
    public void clearQueue() {
        queue.clearQueue();
    }

    @Override
    public boolean hasTracks() {
        return !queue.isEmpty();
    }

    @Override
    public boolean hasNext() {
        return queue.hasNext();
    }

    @Override
    public boolean hasPrevious() {
        return queue.hasPrevious();
    }

    @Override
    public void updateTrackNumberAndPlayingTrack(String trackURI) {
        queue.updateTrackNumberAndPlayingTrack(trackURI);
    }

    @Override
    public List<Track> getQueue() {
        return queue.getQueue();
    }

    @Override
    public Track getCurrentTrack() {
        return queue.getCurrentTrack();
    }

    @Override
    public int getQueuePosition() {
        return queue.getQueuePosition();
    }

    @Override
    public boolean isQueueChanged() {
        return queue.queueChanged();
    }

    @Override
    public void setQueueChanged(boolean changed) {
        queue.setQueueChanged(changed);
    }

    public List<String> getQueueURIList(List<Track> queueToList) {
        Log.i("Queue", "Receiving TRACK_LIST");
        List<String> uriQueue = new ArrayList<>();
        for (Track t : queueToList) {
            uriQueue.add(t.getTrackURI());
        }
        return uriQueue;
    }

}
