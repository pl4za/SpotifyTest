package com.pl4za.spotifytest;

import android.content.Context;
import android.content.SharedPreferences;

import com.pl4za.help.Params;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by jasoncosta on 2/3/2016.
 */
public class SettingsManager {

    private Context context;

    private SettingsManager settingsManager = SettingsManager.getInstance();

    private static final SettingsManager INSTANCE = new SettingsManager();

    public static SettingsManager getInstance() {
        return INSTANCE;
    }

    private SettingsManager() {
    }

    public void setContext(Context context) {
        this.context = context;
    }

    /*
    Set
     */
    public void setUserID(String userID) {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.SpotifyPreferences, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Params.user_id, userID);
        editor.apply();
    }

    public void setAcessToken(String acessToken) {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.SpotifyPreferences, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Params.access_token, acessToken);
        editor.apply();
    }

    public void setRefreshToken(String refreshToken) {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.SpotifyPreferences, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Params.refresh_token, refreshToken);
        editor.apply();
    }

    public void setEtag(String etag) {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.SpotifyPreferences, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Params.Etag, etag);
        editor.apply();
    }

    public void setProduct(String product) {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.SpotifyPreferences, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Params.product, product);
        editor.apply();
    }

    public void setPlayLists(ArrayList<Playlist> playlists) {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.Playlists, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("list_size", playlists.size());
        int i = 0;
        for (Playlist a : playlists) {
            //Log.i("MainActivity", a.getName());
            editor.putString("playlist_" + i, a.getName());
            editor.putString("playlistID_" + i, a.getid());
            editor.putString("playlistUSERID_" + i, a.getUserID());
            i++;
        }
        editor.apply();
    }
    /*
    Get
     */
    public String getUserID() {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.SpotifyPreferences, Context.MODE_PRIVATE);
        return sharedPref.getString(Params.user_id, "");
    }

    public String getAccessToken() {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.SpotifyPreferences, Context.MODE_PRIVATE);
        return sharedPref.getString(Params.access_token, "");
    }

    public String getRefreshToken() {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.SpotifyPreferences, Context.MODE_PRIVATE);
        return sharedPref.getString(Params.refresh_token, "");
    }

    public String getEtag(String url) {
        SharedPreferences sharedPrefCache = context.getSharedPreferences(Params.SpotifyCache, Context.MODE_PRIVATE);
        return sharedPrefCache.getString(url, "");
    }

    public String getProduct() {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.SpotifyPreferences, Context.MODE_PRIVATE);
        return sharedPref.getString(Params.product, "");
    }

    public ArrayList<HashMap<String, String>> getPlaylists() {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.Playlists, Context.MODE_PRIVATE);
        int size = sharedPref.getInt("list_size", 0);
        HashMap<String, String> hm = new HashMap<String, String>();
        ArrayList<HashMap<String, String>> al = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            hm.put("name", sharedPref.getString("playlist_" + i, null));
            hm.put("id", sharedPref.getString("playlistID_" + i, null));
            hm.put("userID", sharedPref.getString("playlistUSERID_" + i, null));
            al.add(hm);
        }
        return al;
    }

    public String[] getPlaylistsNames() {
        SharedPreferences sharedPref = context.getSharedPreferences(Params.Playlists, Context.MODE_PRIVATE);
        int size = sharedPref.getInt("list_size", 0);
        String[] list = new String[size];
        int i = 0;
        for (HashMap<String, String> hm : getPlaylists()) {
            list[i] = hm.get("name");
            i++;
        }
        return list;
    }
}
