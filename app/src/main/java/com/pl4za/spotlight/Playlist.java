package com.pl4za.spotlight;

public class Playlist {

    private String id, name, UserID;

    public Playlist(String id, String name, String UserID) {
        this.id = id;
        this.name = name;
        this.UserID = UserID;
    }

    public String getid() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getUserID() {
        return UserID;
    }
    
    // SET

    public void setName(String name) {
        this.name = name;
    }
}
