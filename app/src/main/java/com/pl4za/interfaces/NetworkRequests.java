package com.pl4za.interfaces;

import com.pl4za.spotlight.Playlist;

import java.util.ArrayList;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public interface NetworkRequests {

    void onTokenReceived(String acessToken, String refreshToken);

    void onTokenRefresh(String newToken);

    void onProfileReceived(String userID, String product, String profilePicture);

    void onPlaylistsReceived(ArrayList<Playlist> playlists);

    void onRandomArtistPictureURLReceived(String artistPictureURL);

    void onPlaylistTracksReceived(String json);

    void onEtagUpdate(String etag);
}
