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
import com.github.mrengineer13.snackbar.SnackBar;
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
    private static List<Track> TrackList = new ArrayList<>();
    private CustomListAdapter mAdapter;
    private AsyncTask<Void, Void, JSONObject> taskCheckCache;
    private String globalUrl = "";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshView;
    private boolean animate = true;
    private boolean isCacheNull = true;
    private View view;
    private FloatingActionButton fabPlay;
    private FloatingActionButton fabQueue;
    // interfaces
    private QueueCtrl queueCtrl = QueueCtrl.getInstance();
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
                TrackList.clear();
                mAdapter.notifyDataSetChanged();
                load();
            }
        });
        refreshView.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        fabPlay = (FloatingActionButton) view.findViewById(R.id.fab);
        fabQueue = (FloatingActionButton) view.findViewById(R.id.fabQueue);
        fabPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFragmentPlayer();
            }
        });
        fabQueue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mViewPager.setCurrentItem(1);
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == SCROLL_STATE_IDLE) {
                    fabPlay.show(true);
                    fabQueue.show(true);

                } else {
                    fabPlay.hide(true);
                    fabQueue.hide(true);
                }
                if (listIsAtTop()) {
                    refreshView.setEnabled(true);
                } else {
                    refreshView.setEnabled(false);
                }
            }
        });
        recyclerView.setEnabled(false);
        mAdapter = new CustomListAdapter(TrackList);
        mAdapter.setSwipeListener(this);
        mAdapter.setSwipeDirection("right");
        recyclerView.setAdapter(mAdapter);
        mAdapter.setMode(Attributes.Mode.Single);
        if (MainActivity.landscape) {
            fabPlay.setVisibility(View.INVISIBLE);
            fabQueue.setVisibility(View.INVISIBLE);
        } else {
            fabPlay.setVisibility(View.VISIBLE);
            fabQueue.setVisibility(View.VISIBLE);
        }
        spotifyNetwork.addNetworkListener(this);
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
    public void updateFilter(String query) {
        mAdapter.getFilter().filter(query);
    }

    @Override
    public void setList(List<Track> list) {
        TrackList = list;
    }

    @Override
    public void onSwipe(int position) {
        Track track = TrackList.get(position);
        queueCtrl.addToQueue(track);
        if (animate) {
            fabPlay.hide(true);
            fabQueue.hide(true);
        }
        viewCtrl.updateActionBar(true, true);
        animate = false;
        new SnackBar.Builder(getActivity())
                .withMessage("Queued: " + track.getTrack())
                .withVisibilityChangeListener(new SnackBar.OnVisibilityChangeListener() {
                    @Override
                    public void onShow(int i) {
                    }

                    @Override
                    public void onHide(int i) {
                        if (!animate) {
                            fabPlay.show(true);
                            fabQueue.show(true);
                            animate = true;
                        }
                    }
                })
                .withDuration(SnackBar.SHORT_SNACK)
                .show();
    }

    @Override
    public void onDoubleClick(int position) {
        FragmentPlayer playFrag = new FragmentPlayer();
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, playFrag, "FragmentPlayer")
                .addToBackStack(null)
                .commit();
        queueCtrl.addToQueue(TrackList.subList(position, TrackList.size()), 0);
    }

    private class checkCache extends AsyncTask<Void, Void, JSONObject> {

        final String url;

        public checkCache(String url) {
            this.url = url;
        }

        @Override
        protected void onPreExecute() {
            if (TrackList.isEmpty()) {
                refreshView.setEnabled(false);
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
                isCacheNull = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    new fillListViewAndQueue(jsonObject).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    new fillListViewAndQueue(jsonObject).execute();
                }
            } else {
                Log.i("FragmentTracks", "Cache is null..");
                isCacheNull = true;
            }
            globalUrl = url;
            getSelectedPlaylistTracks(url);
        }
    }

    private class fillListViewAndQueue extends AsyncTask<Void, Void, String> {

        final JSONObject json;

        public fillListViewAndQueue(JSONObject jsonObject) {
            this.json = jsonObject;
        }

        @Override
        protected String doInBackground(Void... params) {
            //Log.i("fillListView", "Loading track page...............................");
            try {
                if (json != null) {
                    JSONArray playlists = json.getJSONArray("items");
                    for (int i = 0; i < playlists.length(); i++) {
                        //Log.i("json", "Track n: " + i);
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
                        TrackList.add(new Track(tracks.getString("name"), artists, artistID, tracks.getString("duration_ms"), tracks.getJSONObject("album").getString("name"),
                                ids.getString("added_at"), tracks.getString("uri"), albumArt, bigAlbumArt));
                    }
                    /*
                    if (TrackList.isEmpty()) {
                        //Log.i("FragmentTracks", "Empty tracklist: " + TrackList.size());
                        AppController.getInstance().getRequestQueue().getCache().clear();
                        getSelectedPlaylistTracks(globalUrl);
                    }
                    */
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
            //Log.i("FragmentTracks", "Tracklist: " + TrackList.size() + " next: " + next);
            if (!next.equals("null")) {
                Log.i("json", "Loading next page");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    taskCheckCache = new checkCache(next).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    taskCheckCache = new checkCache(next).execute();
                }
            } else {
                Log.i("json", "No more pages");
                if (!TrackList.isEmpty()) {
                    Collections.sort(TrackList, new ListComparator());
                    int i = 0;
                    for (Track s : TrackList) {
                        s.setPosition(i);
                        i++;
                    }
                }
                refreshView.setEnabled(true);
                mAdapter.notifyDataSetChanged();
                refreshView.setRefreshing(false);
                Random rand = new Random();
                String ranArtist = TrackList.get((rand.nextInt(FragmentTracks.TrackList.size()))).getID();
                spotifyNetwork.getRandomArtistPicture(ranArtist);
                recyclerView.setEnabled(true);
                viewCtrl.updateActionBar(true, true);
            }
        }
    }

    public void load() {
        if (taskCheckCache != null) {
            if (taskCheckCache.getStatus() == AsyncTask.Status.PENDING || taskCheckCache.getStatus() == AsyncTask.Status.RUNNING) {
                taskCheckCache.cancel(true);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            taskCheckCache = new checkCache(globalUrl).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            taskCheckCache = new checkCache(globalUrl).execute();
        }
    }

    public void setURL(String user_id, String playlist_id) {
        globalUrl = "https://api.spotify.com/v1/users/" + user_id + "/playlists/" + playlist_id + "/tracks";
    }

    private void getSelectedPlaylistTracks(String url) {
        spotifyNetwork.getSelectedPlaylistTracks(url, settings.getAccessToken(), settings.getEtag(url));
    }

    private boolean listIsAtTop() {
        if (recyclerView.getChildCount() == 0) return true;
        return recyclerView.getChildAt(0).getTop() == 0;
    }

    private void openFragmentPlayer() {
        FragmentPlayer playFrag = new FragmentPlayer();
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, playFrag, "FragmentPlayer")
                .addToBackStack(null)
                .commit();
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

    }

    @Override
    public void onPlaylistTracksReceived(String json) {
        try {
            JSONObject list = new JSONObject(json);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new fillListViewAndQueue(list).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                new fillListViewAndQueue(list).execute();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEtagUpdate(String etag) {
        settings.setEtag(etag);
    }

}