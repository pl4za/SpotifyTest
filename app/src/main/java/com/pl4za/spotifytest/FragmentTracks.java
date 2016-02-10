package com.pl4za.spotifytest;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Cache;
import com.android.volley.Cache.Entry;
import com.daimajia.swipe.util.Attributes;
import com.melnykov.fab.FloatingActionButton;
import com.pl4za.help.CustomListAdapter;
import com.pl4za.help.ListComparator;
import com.pl4za.help.Params;
import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.FragmentOptions;
import com.pl4za.interfaces.NetworkRequests;
import com.pl4za.volley.AppController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FragmentTracks extends Fragment implements FragmentOptions, NetworkRequests {

    private static final String TAG = "FragmentTracks";
    private static final int SCROLL_STATE_IDLE = 0;
    private CustomListAdapter mAdapter;
    private AsyncTask<Void, Void, JSONObject> taskCheckCache;
    private AsyncTask<Void, Void, String> parseJsonToList;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshView;
    private View view;
    private FloatingActionButton fabPlay;
    private FloatingActionButton fabQueue;
    String userID, playlistID;
    List<Track> tempTrackList, tempSortList;

    // interfaces
    private QueueCtrl queueCtrl = QueueCtrl.getInstance();
    private TracklistCtrl tracklistCtrl = TracklistCtrl.getInstance();
    private ViewCtrl viewCtrl = ViewCtrl.getInstance();
    private SettingsManager settings = SettingsManager.getInstance();
    private SpotifyNetworkRequests spotifyNetwork = SpotifyNetworkRequests.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewCtrl.setActivityView((ActivityOptions) getActivity());
        viewCtrl.addFragmentView(this);
        view = inflater.inflate(R.layout.fragment_tracks, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.listview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        refreshView = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        refreshView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                AppController.getInstance().cancelPendingRequests(Params.TAG_getSelectedPlaylistTracks);
                loadTracks(userID, playlistID);
            }
        });
        refreshView.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        fabPlay = (FloatingActionButton) view.findViewById(R.id.fabPlay);
        fabQueue = (FloatingActionButton) view.findViewById(R.id.fabQueue);
        FabClickListener fabClick = new FabClickListener();
        fabPlay.setOnClickListener(fabClick);
        fabQueue.setOnClickListener(fabClick);
        recyclerView.addOnScrollListener(new ListViewScrollListener());
        recyclerView.setEnabled(false);
        mAdapter = new CustomListAdapter(tracklistCtrl.getTrackList());
        mAdapter.setSwipeListener(this);
        mAdapter.setSwipeDirection("right");
        recyclerView.setAdapter(mAdapter);
        mAdapter.setMode(Attributes.Mode.Single);
        if (viewCtrl.isLandscape()) {
            fabPlay.setVisibility(View.INVISIBLE);
            fabQueue.setVisibility(View.INVISIBLE);
        } else {
            fabPlay.setVisibility(View.VISIBLE);
            fabQueue.setVisibility(View.VISIBLE);
        }
        spotifyNetwork.addNetworkListener(this);
        String playlistID = settings.getPlaylists().get(settings.getLastDrawerItem()-1).get(Params.playlist_id);
        String userID = settings.getPlaylists().get(settings.getLastDrawerItem()-1).get(Params.playlist_user_id);
        loadTracks(userID, playlistID);
        return view;
    }

    @Override
    public void onPause() {
        refreshView.setRefreshing(false);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        AppController.getInstance().cancelPendingRequests(Params.TAG_getSelectedPlaylistTracks);
        refreshView.setRefreshing(false);
        super.onDestroy();
    }

    @Override
    public void updateView() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void hideFab(boolean hide) {
        fabPlay.hide(hide);
        fabQueue.hide(hide);
        if (!hide) {
            fabPlay.show(true);
            fabQueue.show(true);
        }
    }

    @Override
    public void updateFilter(String query) {
        mAdapter.getFilter().filter(query);
    }

    @Override
    public void setList(List<Track> list) {
        tracklistCtrl.setTrackList(list);
    }

    @Override
    public void onSwipe(int position) {
        Track track = tracklistCtrl.getTrack(position);
        queueCtrl.addTrack(track);
        viewCtrl.updateView();
        viewCtrl.showSnackBar("Queued: " + track.getTrack());
        viewCtrl.updateActionBar(0);
    }

    @Override
    public void onDoubleClick(int position) {
        getActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.container, new FragmentPlayer(), "FragmentPlayer")
                .addToBackStack("FragmentPlayer")
                .commit();
        queueCtrl.clear();
        queueCtrl.addTrackList(tracklistCtrl.getTrackList().subList(position, tracklistCtrl.getTrackList().size()), 0);
    }

    @Override
    public synchronized void loadTracks(String userID, String playlistID) {
        this.userID = userID;
        this.playlistID = playlistID;
        // Reset temp list
        tempTrackList = new ArrayList<>();
        tempTrackList.clear();
        String url = "https://api.spotify.com/v1/users/" + userID + "/playlists/" + playlistID + "/tracks";
        if (taskCheckCache != null) {
            if (taskCheckCache.getStatus() == AsyncTask.Status.PENDING || taskCheckCache.getStatus() == AsyncTask.Status.RUNNING) {
                taskCheckCache.cancel(true);
            }
        }
        taskCheckCache = new checkCache(url).execute();
    }

    private class checkCache extends AsyncTask<Void, Void, JSONObject> {

        final String url;

        public checkCache(String url) {
            this.url = url;
        }

        @Override
        protected void onPreExecute() {
            refreshView.setEnabled(true);
            refreshView.post(new Runnable() {
                @Override
                public void run() {
                    refreshView.setRefreshing(true);
                }
            });
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            Cache cache = AppController.getInstance().getRequestQueue().getCache();
            Entry entry = cache.get(url);
            if (entry != null) {
                try {
                    String data = new String(entry.data, "UTF-8");
                    return new JSONObject(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            if (jsonObject != null) {
                if (parseJsonToList != null) {
                    if (parseJsonToList.getStatus() == AsyncTask.Status.PENDING || parseJsonToList.getStatus() == AsyncTask.Status.RUNNING) {
                        parseJsonToList.cancel(true);
                    }
                }
                new parseJsonToList(jsonObject).execute();
            } else {
                Log.i(TAG, "Cache is null..");
                spotifyNetwork.getSelectedPlaylistTracks(url, settings.getAccessToken(), settings.getEtag(url));
            }
        }
    }

    private class parseJsonToList extends AsyncTask<Void, Void, String> {

        final JSONObject json;

        public parseJsonToList(JSONObject jsonObject) {
            this.json = jsonObject;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                if (json != null) {
                    JSONArray playlists = json.getJSONArray("items");
                    for (int i = 0; i < playlists.length(); i++) {
                        JSONObject ids = playlists.getJSONObject(i);
                        JSONObject tracks = ids.getJSONObject("track");
                        try {
                            if (tracks.getString("uri").startsWith("spotify:local")) {
                                continue;
                            }
                        } catch (JSONException e) {
                            Log.e("json", "No track uri or local track. Skipping...");
                            break;
                        }
                        JSONArray albumArtArray = tracks.getJSONObject("album").getJSONArray("images");
                        JSONArray artistArray = tracks.getJSONArray("artists");
                        String albumArt = "";
                        String bigAlbumArt = "";
                        String artistID = "";
                        String[] artists = new String[artistArray.length()];
                        try {
                            int artistSize = artistArray.length();
                            for (int j = 0; j < artistSize; j++) {
                                JSONObject artistNumber = artistArray.getJSONObject(j);
                                artists[j] = artistNumber.getString("name");
                                artistID = artistNumber.getString("id");
                            }
                            albumArt = albumArtArray.getJSONObject(1).getString("url");
                            bigAlbumArt = albumArtArray.getJSONObject(0).getString("url");
                        } catch (JSONException e) {
                            Log.e("json", "No album art or No artists");
                        }
                        tempTrackList.add(new Track(tracks.getString("name"), artists, artistID, tracks.getString("duration_ms"), tracks.getJSONObject("album").getString("name"),
                                ids.getString("added_at"), tracks.getString("uri"), albumArt, bigAlbumArt));
                    }
                    return json.getString("next");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return "null";
        }

        @Override
        protected void onPostExecute(String next) {
            super.onPostExecute(next);
            //Log.i(TAG, "Tracklist: " + trackList.size() + " next: " + next);
            if (!next.equals("null")) {
                Log.i(TAG, "Loading next page");
                if (taskCheckCache != null) {
                    if (taskCheckCache.getStatus() == AsyncTask.Status.PENDING || taskCheckCache.getStatus() == AsyncTask.Status.RUNNING) {
                        taskCheckCache.cancel(true);
                    }
                }
                taskCheckCache = new checkCache(next).execute();
            } else {
                Log.i(TAG, "No more pages");
                tempSortList = new ArrayList<>();
                if (!tempTrackList.isEmpty()) {
                    tempSortList.addAll(tempTrackList);
                    Collections.sort(tempSortList, new ListComparator());
                    Random rand = new Random();
                    String ranArtist = tempTrackList.get((rand.nextInt(tempTrackList.size()))).getID();
                    spotifyNetwork.getArtistPicture(ranArtist);
                    int i = 0;
                    for (Track s : tempSortList) {
                        s.setPosition(i);
                        i++;
                    }
                }
                refreshView.setEnabled(true);
                refreshView.setRefreshing(false);
                recyclerView.setEnabled(true);
                tracklistCtrl.clear();
                tracklistCtrl.addTrackList(tempSortList, 0);
                tempSortList.clear();
                mAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(0);
            }
        }
    }

    /*
    Spotify network related requests
     */

    @Override
    public void onProfilePictureReceived(Bitmap image) {

    }

    @Override
    public void onTokenReceived(String acessToken, String refreshToken) {

    }

    @Override
    public void onTokenRefresh(String newToken) {

    }

    @Override
    public void onProfileReceived(String userID, String product, String profilePicture) {

    }

    @Override
    public void onPlaylistsReceived(ArrayList<Playlist> playlists) {

    }

    @Override
    public void onRandomArtistPictureURLReceived(String artistPictureURL) {
        if (artistPictureURL.equals("none")) {
            Random rand = new Random();
            String ranArtist = tracklistCtrl.getTrackList().get((rand.nextInt(tracklistCtrl.getTrackList().size()))).getID();
            spotifyNetwork.getArtistPicture(ranArtist);
        }
    }

    @Override
    public void onPlaylistTracksReceived(String json) {
        try {
            JSONObject list = new JSONObject(json);
            if (parseJsonToList != null) {
                if (parseJsonToList.getStatus() == AsyncTask.Status.PENDING || parseJsonToList.getStatus() == AsyncTask.Status.RUNNING) {
                    parseJsonToList.cancel(true);
                }
            }
            parseJsonToList = new parseJsonToList(list).execute();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEtagUpdate(String etag) {
        settings.setEtag(etag);
    }

    private class ListViewScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == SCROLL_STATE_IDLE) {
                hideFab(false);

            } else {
                hideFab(true);
            }
        }
    }

    private class FabClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.fabQueue) {
                viewCtrl.setViewPagerPosition(1);
                viewCtrl.updateActionBar(1);
            } else if (v.getId() == R.id.fabPlay) {
                FragmentPlayer playFrag = new FragmentPlayer();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, playFrag, "FragmentPlayer")
                        .addToBackStack("FragmentPlayer")
                        .commit();
                viewCtrl.updateActionBar(2);
            }
        }
    }
}