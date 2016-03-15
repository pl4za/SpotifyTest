package com.pl4za.spotlight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Jason on 01/02/2015.
 */
class Queue {
    private static final String TAG = "Queue";
    private static List<Track> TRACK_LIST = Collections.synchronizedList(new ArrayList<Track>());
    private static int trackNumber = 0;
    private static int trackNumberUpdate = 0;
    private static boolean queueChanged = false;

    public void addToQueue(Track track) {
        TRACK_LIST.add(track);
    }

    public void addToQueue(List<Track> tracklist, int position) {
        if (isEmpty()) {
            List<Track> temp = new ArrayList<>(tracklist);
            TRACK_LIST.addAll(temp);
        }
        trackNumber = position;
    }

    public boolean isEmpty() {
        return TRACK_LIST.isEmpty();
    }

    public void setTrackNumberUpdate(int update) {
        trackNumberUpdate = update;
    }

    public int getTrackNumberUpdate() {
        return trackNumberUpdate;
    }

    public void clearQueue() {
        TRACK_LIST = Collections.synchronizedList(new ArrayList<Track>());
        trackNumber = 0;
    }

    public void updateTrackNumberAndPlayingTrack(String uri) {
        if (TRACK_LIST.size() > 0) {
            int i = 0;
            for (Track a : TRACK_LIST) {
                if (a.getTrackURI().equals(uri)) {
                    trackNumber = i;
                    break;
                }
                i++;
            }
        } else {
            trackNumber = 0;
        }
    }

    private void updateQueuePosition(int removedPosition) {
        if (TRACK_LIST.size() > 0) {
            if (removedPosition < trackNumber) {
                //updateTrackNumberAndPlayingTrack(playingTrack.getTrackURI());
                trackNumber--;
            } else if (removedPosition == trackNumber) {
                if (removedPosition == TRACK_LIST.size()) {
                    trackNumber--;
                }
            }
        } else {
            trackNumber = 0;
        }
    }

    public void removeFromQueue(int position) {
        TRACK_LIST.remove(position);
        queueChanged = true;
        updateQueuePosition(position);
    }

    public boolean hasNext() {
        return trackNumber + 1 < TRACK_LIST.size();
    }

    public boolean hasPrevious() {
        return trackNumber > 0;
    }

    public List<Track> getQueue() {
        return TRACK_LIST;
    }

    public int getQueuePosition() {
        return trackNumber;
    }

    public boolean queueChanged() {
        return queueChanged;
    }

    public void setQueueChanged(boolean changed) {
        queueChanged = changed;
    }

    public Track getCurrentTrack() {
        return TRACK_LIST.get(trackNumber);
    }

    public void setQueue(List<Track> queue) {
        TRACK_LIST = queue;
    }

}
