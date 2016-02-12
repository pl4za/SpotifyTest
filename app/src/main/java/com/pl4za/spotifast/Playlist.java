package com.pl4za.spotifast;

public class Playlist {

    private String id, name, UserID;
    
    public Playlist() {
    }
 
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
    
    public void setID(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setUserID(String UserID) {
        this.UserID = UserID;
    }
}
