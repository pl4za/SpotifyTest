package com.pl4za.spotifytest;

import android.content.Context;
import android.content.SharedPreferences;
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

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Cache.Entry;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.daimajia.swipe.util.Attributes;
import com.github.mrengineer13.snackbar.SnackBar;
import com.melnykov.fab.FloatingActionButton;
import com.pl4za.help.CustomListAdapter;
import com.pl4za.help.ListComparator;
import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.FragmentOptions;
import com.pl4za.volley.AppController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FragmentTracks extends Fragment implements FragmentOptions {

    private static final String TAG = "FragmentTracks";
    private static final int SCROLL_STATE_IDLE = 0;
    private final static int MY_SOCKET_TIMEOUT_MS = 7000;
    public static List<Track> TrackList = new ArrayList<>();
    public CustomListAdapter mAdapter;
    private AsyncTask<Void, Void, JSONObject> taskCheckCache;
    private String globalUrl = "";
    private int statusCode = 0;
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

    public static void setFilteredList(List<Track> TrackListUpdated) {
        TrackList = TrackListUpdated;
    }

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
                AppController.getInstance().cancelPendingRequests(getString(R.string.TAG_getSelectedPlaylistTracks));
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
        return view;
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

    @Override
    public void onPause() {
        refreshView.setRefreshing(false);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        AppController.getInstance().cancelPendingRequests(getString(R.string.TAG_getSelectedPlaylistTracks));
        refreshView.setRefreshing(false);
        super.onDestroy();
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
        Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                if (statusCode == 200) {
                    JSONObject json = createJsonFromString(list);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        new fillListViewAndQueue(json).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        new fillListViewAndQueue(json).execute();
                    }
                }
            }
        };
        ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Volley Error", "Code: " + statusCode + " - " + error.toString());
                if (statusCode != 304) {
                    //TODO: refresh token
                }
            }
        };

        // Main request
        StringRequest fileRequest = new StringRequest(Request.Method.GET, url, jsonListerner, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
                SharedPreferences sharedPrefCache = getActivity().getSharedPreferences(getString(R.string.SpotifyCache), Context.MODE_PRIVATE);
                String access_token = sharedPref.getString(getString(R.string.access_token), "");
                String etag = sharedPrefCache.getString(globalUrl, "");
                //Log.i("FragmentTracks", "Access Token for tracks: " + access_token + "\n" + "etag: " + etag);
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + access_token);
                if (!isCacheNull && !etag.equals("")) {
                    headers.put("If-None-Match", etag);
                }
                return headers;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                statusCode = response.statusCode;
                Log.i("FragmentTracks", "status: " + statusCode);
                if (statusCode == 200) {
                    Map<String, String> responseHeaders = response.headers;
                    saveLastModified(responseHeaders.get("ETag"));
                }
                return super.parseNetworkResponse(response);
            }
        };
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        AppController.getInstance().addToRequestQueue(fileRequest, getString(R.string.TAG_getSelectedPlaylistTracks));
    }

    private void saveLastModified(String eTag) {
        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.SpotifyCache), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(globalUrl, eTag);
        editor.apply();
    }

    private JSONObject createJsonFromString(String list) {
        try {
            return new JSONObject(list);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void updateView() {
        mAdapter.notifyDataSetChanged();
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
                    return createJsonFromString(data);
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
                ((MainActivity) getActivity()).getRandomArtistPicture();
                recyclerView.setEnabled(true);
                viewCtrl.updateActionBar(true, true);
            }
        }
    }
}