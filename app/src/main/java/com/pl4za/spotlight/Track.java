package com.pl4za.spotlight;

public class Track {
    private String track, time, album, added, trackuri, albumArt, bigAlbumArt, id;
    private String[] artist;
    private String simpleArtist;
    private int position;

    public Track() {
    }
 
    public Track(String track, String[] artist, String id, String time, String album, String added, String trackuri, String albumArt, String bigAlbumArt) {
        this.track = track;
        this.artist = artist;
        this.id = id;
        this.time = time;
        this.album = album;
        this.added = added;
        this.trackuri = trackuri;
        this.albumArt = albumArt;
        this.bigAlbumArt = bigAlbumArt;
    }
 
    public String getTrack() {
        return track;
    }

    public int getPosition() {
        return position;
    }

    public String[] getArtist() {
        return artist;
    }
    
    public String getTime() {
        return time;
    }
    
    public String getAlbum() {
        return album;
    }
    
    public String getAdded() {
        return added;
    }
    
    public String getTrackURI() {
        return trackuri;
    }
    
    public String getAlbumArt() {
        return albumArt;
    }

    public String getBigAlbumArt() {
        return bigAlbumArt;
    }

    public String getID() {
        return id;
    }

    // SET
    
    public void setTrack(String track) {
        this.track = track;
    }

    public void setArtist(String[] artist) {
        this.artist = artist;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public void setAlbum(String album) {
        this.album = album;
    }
    
    public void setAdded(String added) {
        this.added = added;
    }
 
    public void setTrackURI(String trackuri) {
        this.trackuri = trackuri;
    }
    
    public void setAlbumArt(String albumArt) {
        this.albumArt = albumArt;
    }

    public void setBigAlbumArt(String bigAlbumArt) {
        this.bigAlbumArt = bigAlbumArt;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getSimpleArtist() {
        String artistText = "";
        for (int i = 0; i < artist.length; i++) {
            artistText = artist[i];
            if (i+1<artist.length)
                artistText += " - ";
        }
        return artistText;
    }
}
