package com.pl4za.spotlight;

import android.content.res.Configuration;
import android.os.AsyncTask;
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
import com.pl4za.help.TracksAdapter;
import com.pl4za.help.Params;
import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.FragmentOptions;
import com.pl4za.interfaces.NetworkRequests;
import com.pl4za.volley.AppController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FragmentTracks extends Fragment implements FragmentOptions, NetworkRequests {

    private static final String TAG = "FragmentTracks";
    private static int pageNumber = 0;
    private static final int SCROLL_STATE_IDLE = 0;
    private String userID;
    private String playlistID;
    private List<Track> tempTrackList;
    private TracksAdapter mAdapter;
    private AsyncTask<Void, Void, JSONObject> taskCheckCache;
    private AsyncTask<Void, Void, String> parseJsonToList;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshView;
    private FloatingActionButton fabPlay;
    private FloatingActionButton fabQueue;

    // interfaces
    private final QueueCtrl queueCtrl = QueueCtrl.getInstance();
    private final TracklistCtrl tracklistCtrl = TracklistCtrl.getInstance();
    private final ViewCtrl viewCtrl = ViewCtrl.getInstance();
    private final SettingsManager settings = SettingsManager.getInstance();
    private final SpotifyNetworkRequests spotifyNetwork = SpotifyNetworkRequests.getInstance();
    private final PlayCtrl playCtrl = PlayCtrl.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewCtrl.setActivityView((ActivityOptions) getActivity());
        viewCtrl.addFragmentView(this);
        View view = inflater.inflate(R.layout.fragment_tracks, container, false);
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
                android.R.color.holo_red_light);        fabPlay = (FloatingActionButton) view.findViewById(R.id.fabPlay);
        fabQueue = (FloatingActionButton) view.findViewById(R.id.fabQueue);
        FabClickListener fabClick = new FabClickListener();
        fabPlay.setOnClickListener(fabClick);
        fabQueue.setOnClickListener(fabClick);
        recyclerView.addOnScrollListener(new ListViewScrollListener());
        recyclerView.setEnabled(false);
        mAdapter = new TracksAdapter(tracklistCtrl.getTrackList());
        mAdapter.setSwipeListener(this);
        mAdapter.setSwipeDirection("right");
        recyclerView.setAdapter(mAdapter);
        //recyclerView.setItemAnimator(new AdapterAnimator());
        mAdapter.setMode(Attributes.Mode.Single);
        if (viewCtrl.isLandscape()) {
            fabPlay.setVisibility(View.INVISIBLE);
            fabQueue.setVisibility(View.INVISIBLE);
        } else {
            fabPlay.setVisibility(View.VISIBLE);
            fabQueue.setVisibility(View.VISIBLE);
        }
        if (savedInstanceState == null) {
            spotifyNetwork.addNetworkListener(this);
            String playlistID = settings.getPlaylists().get(settings.getLastDrawerItem() - 1).get(Params.playlist_id);
            String userID = settings.getPlaylists().get(settings.getLastDrawerItem() - 1).get(Params.playlist_user_id);
            loadTracks(userID, playlistID);
        }
        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppController.getInstance().cancelPendingRequests(Params.TAG_getSelectedPlaylistTracks);
        refreshView.setRefreshing(false);
        //recyclerView.removeAllViews();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            fabPlay.setVisibility(View.INVISIBLE);
            fabQueue.setVisibility(View.INVISIBLE);
        } else {
            fabPlay.setVisibility(View.VISIBLE);
            fabQueue.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPause() {
        refreshView.setRefreshing(false);
        super.onPause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppController.getInstance().cancelPendingRequests(Params.TAG_getSelectedPlaylistTracks);
        recyclerView.removeAllViews();
        if (parseJsonToList != null) {
            if (parseJsonToList.getStatus() == AsyncTask.Status.PENDING || parseJsonToList.getStatus() == AsyncTask.Status.RUNNING) {
                parseJsonToList.cancel(true);
            }
        }
        if (taskCheckCache != null) {
            if (taskCheckCache.getStatus() == AsyncTask.Status.PENDING || taskCheckCache.getStatus() == AsyncTask.Status.RUNNING) {
                taskCheckCache.cancel(true);
            }
        }
    }

    @Override
    public void updateView() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void hideFab(boolean hide) {
        if (hide) {
            fabPlay.hide(true);
            fabQueue.hide(true);
        } else {
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
        viewCtrl.updateView(1);
        //viewCtrl.updateActionBar(0);
    }

    @Override
    public void onDoubleClick(int position) {
        queueCtrl.clear();
        queueCtrl.addTrackList(tracklistCtrl.getTrackList().subList(position, tracklistCtrl.getTrackList().size()), 0);
        if (playCtrl.isActive()) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new FragmentPlayer(), "FragmentPlayer")
                    .addToBackStack("FragmentPlayer")
                    .commit();
        }
    }

    @Override
    public synchronized void loadTracks(String userID, String playlistID) {
        this.userID = userID;
        this.playlistID = playlistID;
        // Reset temp list
        if (tempTrackList != null) {
            tempTrackList.clear();
        }
        tempTrackList = new ArrayList<>();
        pageNumber = 0;
        String url = "https://api.spotify.com/v1/users/" + userID + "/playlists/" + playlistID + "/tracks";
        if (taskCheckCache != null) {
            if (taskCheckCache.getStatus() == AsyncTask.Status.PENDING || taskCheckCache.getStatus() == AsyncTask.Status.RUNNING) {
                taskCheckCache.cancel(true);
            }
        }
        taskCheckCache = new checkCache(url).execute();
    }

    @Override
    public void onTokenReceived(String acessToken, String refreshToken) {

    }

    private boolean listIsAtTop() {
        return recyclerView.getChildCount() == 0 || recyclerView.getChildAt(0).getTop() == 0;
    }

    /*
    Spotify network related requests
     */

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

    private class checkCache extends AsyncTask<Void, Void, JSONObject> {

        final String url;

        public checkCache(String url) {
            this.url = url;
        }

        @Override
        protected void onPreExecute() {
            refreshView.setEnabled(true);
            if (!refreshView.isRefreshing()) {
                refreshView.post(new Runnable() {
                    @Override
                    public void run() {
                        refreshView.setRefreshing(true);
                    }
                });
            }
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
                    JSONObject ids, tracks, artistNumber;
                    JSONArray albumArtArray, artistArray;
                    String albumArt, bigAlbumArt, artistID;
                    albumArt = bigAlbumArt = artistID = "";
                    String[] artists;
                    int artistSize;
                    for (int i = 0; i < playlists.length(); i++) {
                        ids = playlists.getJSONObject(i);
                        tracks = ids.getJSONObject("track");
                        try {
                            if (tracks.getString("uri").startsWith("spotify:local")) {
                                continue;
                            }
                        } catch (JSONException e) {
                            Log.e("json", "No track uri or local track. Skipping...");
                            break;
                        }
                        albumArtArray = tracks.getJSONObject("album").getJSONArray("images");
                        artistArray = tracks.getJSONArray("artists");
                        artists = new String[artistArray.length()];
                        try {
                            artistSize = artistArray.length();
                            for (int j = 0; j < artistSize; j++) {
                                artistNumber = artistArray.getJSONObject(j);
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
            Log.i(TAG, "Loading page: "+pageNumber);
            if (!next.equals("null")) {
                if (pageNumber<100) { //TODO: stops at x pages because of memory
                    taskCheckCache = new checkCache(next).execute();
                }
                if (pageNumber==0) {
                    tracklistCtrl.clear();
                }
                for (Track track : tempTrackList) {
                    tracklistCtrl.addTrack(0, track);
                }
                tempTrackList = new ArrayList<>();
                mAdapter.notifyDataSetChanged();
            } else {
                Log.i(TAG, "No more pages");
                // add last page
                if (pageNumber==0) {
                    tracklistCtrl.clear();
                }
                for (Track track : tempTrackList) {
                    tracklistCtrl.addTrack(0, track);
                }
                refreshView.setEnabled(true);
                refreshView.setRefreshing(false);
                recyclerView.setEnabled(true);
                mAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(0);
            }
            pageNumber++;
        }
    }

    private class ListViewScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == SCROLL_STATE_IDLE) {
                hideFab(false);
            } else {
                if (!listIsAtTop()) {
                    hideFab(true);
                }
            }
        }
    }

    private class FabClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.fabQueue) {
                viewCtrl.setViewPagerPosition(1);
                //viewCtrl.updateActionBar(1);
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