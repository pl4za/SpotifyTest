package com.pl4za.spotifytest;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Jason on 01/02/2015.
 */
public class Queue {

    private static final List<Track> TRACK_LIST = Collections.synchronizedList(new ArrayList<Track>());
    private static Track playingTrack = null;
    private static int trackNumber = 0;
    private static boolean queueChanged = false;

    public void addToQueue(Track track) {
        if (TRACK_LIST.isEmpty()) {
            playingTrack = track;
        }
        TRACK_LIST.add(track);
    }

    public void addToQueue(List<Track> tracklist, int position) {
        trackNumber = position;
        List<Track> temp = new ArrayList<>(tracklist);
        clearQueue();
        TRACK_LIST.addAll(temp);
        playingTrack = tracklist.get(position);
    }

    public boolean isEmpty() {
        return TRACK_LIST.isEmpty();
    }

    public void clearQueue() {
        TRACK_LIST.clear();
        trackNumber = 0;
    }

    public void updateTrackNumberAndPlayingTrack(String uri) {
        if (TRACK_LIST.size() > 0) {
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

    private void updatedTrackNumber(int removedPosition) {
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
                    //PlayService.nextTrack();
                    //playingTrack = TRACK_LIST.get(trackNumber);
                }
            }
            Log.i("Queue", "Current: " + playingTrack.getTrack());
        } else {
            Log.i("Queue", "3");
            trackNumber = 0;
        }
    }

    public void removeFromQueue(int position) {
        TRACK_LIST.remove(position);
        updatedTrackNumber(position);
        queueChanged = true;
    }

    public boolean hasNext() {
        Log.i("Queue", trackNumber + 1 + " - " + TRACK_LIST.size());
        return trackNumber + 1 < TRACK_LIST.size();
    }

    public boolean hasPrevious() {
        Log.i("Queue", "Track number: " + trackNumber);
        return trackNumber > 0;
    }

    public List<Track> getQueue() {
        return TRACK_LIST;
    }

    public int getQueuePosition() {
        return trackNumber;
    }

    public int getQueuePosition(String trackURI) {
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

    public boolean queueChanged() {
        return queueChanged;
    }

    public void setQueueChanged(boolean changed) {
        queueChanged=changed;
    }

    public Track getCurrentTrack() {
        return TRACK_LIST.get(trackNumber);
    }

}
