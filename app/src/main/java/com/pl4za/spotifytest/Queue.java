package com.pl4za.spotifytest;

import android.util.Log;

import com.pl4za.interfaces.IqueueRefresh;
import com.pl4za.interfaces.IrefreshActionBar;
import com.pl4za.interfaces.ItracksRefresh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Jason on 01/02/2015.
 */
public class Queue {

    public static final List<Track> queue = Collections.synchronizedList(new ArrayList<Track>());
    //public static List<Track> queueBackup = Collections.synchronizedList(new ArrayList<Track>());
    public static Track playingTrack = null;
    public static int trackNumber = 0;
    public static boolean queueChanged = false;
    private static IqueueRefresh callBackRefreshQueue;
    private static ItracksRefresh callBackRefreshTracks;
    private static IrefreshActionBar callBackRefreshActionBar;

    public static void addToQueue(List<Track> playlist) {
        Log.i("Queue", "Adding List to queue");
        Log.i("Queue", "Queue size: " + queue.size());
        queue.addAll(playlist);
        callBackRefreshQueue.refreshList();
        MainActivity.ENABLE_UNDO_VALUE = true;
        callBackRefreshActionBar.refreshActionBar();
        Log.i("Queue", "Queue new size: " + queue.size());
        Log.i("Queue", "Queue 0: " + queue.get(0).getTrack());
        Log.i("Queue", "Queue last: " + queue.get(queue.size() - 1).getTrack());
    }

    public static void addToQueue(Track track) {
        //Log.i("Queue", "Adding track to queue");
        //Log.i("Queue", "Queue size: " + queue.size());
        queue.add(track);
        if (queue.size() - 1 == 0)
            playingTrack = queue.get(0);
        if (callBackRefreshQueue != null)
            callBackRefreshQueue.refreshList();
        MainActivity.ENABLE_UNDO_VALUE = true;
        callBackRefreshActionBar.refreshActionBar();
        //Log.i("Queue", "Queue new size: " + queue.size());
        // Log.i("Queue", "Queue 0: " + queue.get(0).getTrack());
        //Log.i("Queue", "Queue last: " + queue.get(queue.size() - 1).getTrack());
    }

    public static void updateQueue(int position) {
        Log.i("Queue", "Updating queue: " + position);
        queue.subList(0, position).clear();
        trackNumber = 0;
        playingTrack = Queue.queue.get(0);
    }

    public static void clearQueue() {
        queue.clear();
        callBackRefreshTracks.refreshTrackList();
        callBackRefreshQueue.refreshList();
        trackNumber = 0;
    }

    public static void updateTrackNumberAndPlayingTrack(String uri) {
        if (queue.size()> 0) {
            int i = 0;
            for (Track a : queue) {
                if (a.getTrackURI().equals(uri)) {
                    Log.i("Queue", "found track: " + i + " " + a.getTrack());
                    trackNumber = i;
                    playingTrack = a;
                    break;
                }
                i++;
            }
        } else {
            trackNumber = 0;
            playingTrack = null;
        }
    }

    private static void updatedTrackNumber(int removedPosition) {
        if (queue.size() > 0) {
            if (removedPosition > trackNumber) {
                Log.i("Queue", "Removed after playing track");
            } else if (removedPosition < trackNumber) {
                Log.i("Queue", "Removed before playing track");
                //updateTrackNumberAndPlayingTrack(playingTrack.getTrackURI());
                trackNumber--;
            } else if (removedPosition == trackNumber) {
                Log.i("Queue", "Removed playing track");
                if (removedPosition == queue.size()) {
                    trackNumber--;
                } else {
                    Log.i("Queue", "Next track");
                    PlayService.nextTrack();
                    //playingTrack = queue.get(trackNumber);
                }
            }
            Log.i("Queue", "Current: " + playingTrack.getTrack());
        } else {
            Log.i("Queue", "3");
            trackNumber = 0;
        }
    }

    public static List<String> getQueue(List<Track> queueToList) {
        Log.i("Queue", "Receiving queue");
        List<String> uriQueue = new ArrayList<>();
        for (Track t : queueToList) {
            uriQueue.add(t.getTrackURI());
        }
        return uriQueue;
    }

    public static void removeFromQueue(int position) {
        Log.i("Queue", "Removing from queue: " + position);
        queue.remove(position);
        callBackRefreshTracks.refreshTrackList();
        updatedTrackNumber(position);
        queueChanged = true;
        MainActivity.ENABLE_UNDO_VALUE = true;
        callBackRefreshActionBar.refreshActionBar();
        if (callBackRefreshQueue != null)
            callBackRefreshQueue.refreshList();
        if (callBackRefreshTracks != null)
            callBackRefreshTracks.refreshTrackList();
    }

    public static boolean hasNext() {
        Log.i("Queue", trackNumber + 1 + " - " + queue.size());
        return trackNumber + 1 < queue.size();
    }

    public static boolean hasPrevious() {
        Log.i("Queue", "Track number: " + trackNumber);
        return trackNumber > 0;
    }

    public static int getPosition(String trackURI) {
        if (queue.size() > 0) {
            int i = 0;
            for (Track a : queue) {
                if (a.getTrackURI().equals(trackURI)) {
                    Log.i("Queue", "found track: " + i + " " + a.getTrack());
                    return i;
                }
                i++;
            }
        } else {
            return 0;
        }
        return 0;
    }

    public static void QueueRefreshListener(IqueueRefresh qrf) {
        callBackRefreshQueue = qrf;
    }

    public static void TracksRefreshListener(ItracksRefresh trf) {
        callBackRefreshTracks = trf;
    }

    public static void refreshActionBarListener(IrefreshActionBar qrf) {
        callBackRefreshActionBar = qrf;
    }

}
