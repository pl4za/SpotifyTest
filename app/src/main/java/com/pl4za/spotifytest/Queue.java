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

    public static final List<Track> TRACK_LIST = Collections.synchronizedList(new ArrayList<Track>());
    public static Track playingTrack = null;
    public static int trackNumber = 0;
    public static boolean queueChanged = false;
    // Interfaces
    private static IqueueRefresh callBackRefreshQueue;
    private static ItracksRefresh callBackRefreshTracks;
    private static IrefreshActionBar callBackRefreshActionBar;

    public static void addToQueue(Track track) {
        if (TRACK_LIST.isEmpty()) {
            playingTrack = track;
        }
        TRACK_LIST.add(track);
        if (callBackRefreshQueue != null) {
            callBackRefreshQueue.refreshList();
        }
        callBackRefreshActionBar.refreshActionBar(-1);
    }

    public static void addToQueue(List<Track> queue) {
        trackNumber = 0;
        playingTrack = queue.get(0);
        TRACK_LIST.addAll(queue);
        callBackRefreshQueue.refreshList();
        callBackRefreshActionBar.refreshActionBar(-1);
    }

    public static boolean isEmpty() {
        return TRACK_LIST.isEmpty();
    }

    public static void clearQueue() {
        TRACK_LIST.clear();
        callBackRefreshTracks.refreshTrackList();
        callBackRefreshQueue.refreshList();
        trackNumber = 0;
    }

    public static void updateTrackNumberAndPlayingTrack(String uri) {
        if (TRACK_LIST.size()> 0) {
            int i = 0;
            for (Track a : TRACK_LIST) {
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
        if (TRACK_LIST.size() > 0) {
            if (removedPosition > trackNumber) {
                Log.i("Queue", "Removed after playing track");
            } else if (removedPosition < trackNumber) {
                Log.i("Queue", "Removed before playing track");
                //updateTrackNumberAndPlayingTrack(playingTrack.getTrackURI());
                trackNumber--;
            } else if (removedPosition == trackNumber) {
                Log.i("Queue", "Removed playing track");
                if (removedPosition == TRACK_LIST.size()) {
                    trackNumber--;
                } else {
                    Log.i("Queue", "Next track");
                    PlayService.nextTrack();
                    //playingTrack = TRACK_LIST.get(trackNumber);
                }
            }
            Log.i("Queue", "Current: " + playingTrack.getTrack());
        } else {
            Log.i("Queue", "3");
            trackNumber = 0;
        }
    }

    public static List<String> getQueueURIList(List<Track> queueToList) {
        Log.i("Queue", "Receiving TRACK_LIST");
        List<String> uriQueue = new ArrayList<>();
        for (Track t : queueToList) {
            uriQueue.add(t.getTrackURI());
        }
        return uriQueue;
    }

    public static void removeFromQueue(int position) {
        Log.i("Queue", "Removing from TRACK_LIST: " + position);
        TRACK_LIST.remove(position);
        callBackRefreshTracks.refreshTrackList();
        updatedTrackNumber(position);
        queueChanged = true;
        callBackRefreshActionBar.refreshActionBar(3);
        if (callBackRefreshQueue != null)
            callBackRefreshQueue.refreshList();
        if (callBackRefreshTracks != null)
            callBackRefreshTracks.refreshTrackList();
    }

    public static boolean hasNext() {
        Log.i("Queue", trackNumber + 1 + " - " + TRACK_LIST.size());
        return trackNumber + 1 < TRACK_LIST.size();
    }

    public static boolean hasPrevious() {
        Log.i("Queue", "Track number: " + trackNumber);
        return trackNumber > 0;
    }

    public static int getPosition(String trackURI) {
        if (TRACK_LIST.size() > 0) {
            int i = 0;
            for (Track a : TRACK_LIST) {
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
