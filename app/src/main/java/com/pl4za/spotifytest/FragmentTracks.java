package com.pl4za.spotifytest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

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
import com.pl4za.interfaces.ISwipeListener;
import com.pl4za.interfaces.IrefreshActionBar;
import com.pl4za.interfaces.IrefreshToken;
import com.pl4za.interfaces.IspotifyPlayerOptions;
import com.pl4za.interfaces.ItracksRefresh;
import com.pl4za.volley.AppController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FragmentTracks extends Fragment implements ItracksRefresh, ISwipeListener {

    private final static int MY_SOCKET_TIMEOUT_MS = 7000;
    public static List<Track> TrackList = new ArrayList<>();
    private static IspotifyPlayerOptions mInitializePlayer;
    public CustomListAdapter mAdapter;
    private IrefreshToken mRefreshToken;
    private IrefreshActionBar mRefreshActionBar;
    private AsyncTask<Void, Void, JSONObject> taskCheckCache;
    private String globalUrl = "";
    private int statusCode = 0;
    private Context context;
    private ListView swipeListView;
    private SwipeRefreshLayout refreshView;
    private boolean animate = true;
    private boolean isCacheNull = true;
    private View view;
    private FloatingActionButton fabPlay;
    private FloatingActionButton fabQueue;

    public static void insertTrack(Track track, int position) {
        if (position >= TrackList.size()) {
            TrackList.add(track);
        } else {
            TrackList.add(position, track);
        }
    }

    public static void updateTrackList(List<Track> TrackListUpdated) {
        TrackList = TrackListUpdated;
    }

    public static void playerInitializeListener(IspotifyPlayerOptions listener) {
        mInitializePlayer = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity();
        mRefreshToken = (IrefreshToken) context;
        mRefreshActionBar = (IrefreshActionBar) context;
        view = inflater.inflate(R.layout.fragment_tracks, container, false);
        swipeListView = (ListView) view.findViewById(R.id.listview);
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

        swipeListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
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

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
        swipeListView.setEnabled(false);
        mAdapter = new CustomListAdapter(getActivity(), TrackList);
        mAdapter.setSwipeListener(this);
        mAdapter.setSwipeDirection("right");
        swipeListView.setAdapter(mAdapter);
        mAdapter.setMode(Attributes.Mode.Single);
        Queue.TracksRefreshListener(this);
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
        if (swipeListView.getChildCount() == 0) return true;
        return swipeListView.getChildAt(0).getTop() == 0;
    }

    private void openFragmentPlayer() {
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Create new fragment to add (Fragment B)
            FragmentPlayer playFrag = new FragmentPlayer();
            playFrag.setSharedElementEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.change_image_transform));
            playFrag.setEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.explode));
            //TODO: crash on back
            // Our shared element (in Fragment A)
            ImageView ivAlbumArt = (ImageView) view.findViewById(R.id.thumbnail);

            // Add Fragment B
            FragmentTransaction ft = getFragmentManager().beginTransaction()
                    .replace(R.id.container, playFrag)
                    .addToBackStack(null)
                    .addSharedElement(ivAlbumArt, "MyTransition");
            ft.commit();
        } else {*/
        FragmentPlayer playFrag = new FragmentPlayer();
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, playFrag, "FragmentPlayer")
                .addToBackStack(null)
                .commit();
        // }
        ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((MainActivity) getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(false);
        MainActivity.isHomeAsUpEnabled = true;

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

    private void PlayOnSpotify(int position) {
        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
        String product = sharedPref.getString(getString(R.string.product), "");
        if (product.equals("premium")) {
            if (PlayService.mPlayer == null || PlayService.mPlayer.isShutdown()) {
                Toast.makeText(context, "Player not initialized.. Please wait or restart.", Toast.LENGTH_SHORT).show();
                mInitializePlayer.initializePlayer();
                mInitializePlayer.startNotification();
            }
            Queue.clearQueue();
            Queue.addToQueue(TrackList.subList(position, TrackList.size()));
            PlayService.clearQueue();
            PlayService.addToQueue(Queue.getQueueURIList(Queue.TRACK_LIST), 0);
            ((MainActivity) getActivity()).clearSearch();
            openFragmentPlayer();
            removeTracksInserted(position);
        } else {
            try {
                Intent spotify = new Intent(Intent.ACTION_VIEW, Uri.parse(TrackList.get(position).getTrackURI()));
                context.startActivity(spotify);
            } catch (Exception e) {
                Intent play = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.spotify.music"));
                context.startActivity(play);
            }
        }
    }

    private void removeTracksInserted(int position) {
        TrackList.subList(position, TrackList.size()).clear();
    }

    private void getSelectedPlaylistTracks(String url) {
        Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                if (statusCode == 200) {
                    JSONObject json = createJsonFromString(list);
                    ////Log.i("FragmentTracks", list);
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
                    if (mRefreshToken != null)
                        mRefreshToken.refreshToken("getTracksFailed");
                }
                //AppController.getInstance().getRequestQueue().getCache().remove(globalUrl);
                //AppController.getInstance().getRequestQueue().getCache().clear();
            }
        };

        // Main request
        StringRequest fileRequest = new StringRequest(Request.Method.GET, url, jsonListerner, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
                SharedPreferences sharedPrefCache = context.getSharedPreferences(getString(R.string.SpotifyCache), Context.MODE_PRIVATE);
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
        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.SpotifyCache), Context.MODE_PRIVATE);
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
    public void refreshTrackList() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSwipe(int position) {
        //Log.i("FragmentTracks", "y: " + fabQueue.getY());
        mRefreshActionBar.refreshActionBar(0);
        if (animate) {
            fabPlay.hide(true);
            fabQueue.hide(true);
        }
        animate = false;
        new SnackBar.Builder(getActivity())
                .withMessage("Queued: " + TrackList.get(position).getTrack())
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
        Log.d("swipe", "dismiss");
        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
        String product = sharedPref.getString(getString(R.string.product), "");
        if (product.equals("premium")) {
            if (PlayService.mPlayer == null || PlayService.mPlayer.isShutdown()) {
                Toast.makeText(context, "Initializing player", Toast.LENGTH_SHORT).show();
                mInitializePlayer.initializePlayer();
                mInitializePlayer.startNotification();
            }
            if (!TrackList.isEmpty()) {
                Queue.addToQueue(TrackList.get(position));
                PlayService.addToQueue(TrackList.get(position).getTrackURI());
                TrackList.remove(position);
                mAdapter.notifyDataSetChanged();
            }
        } else {
            try {
                Intent spotify = new Intent(Intent.ACTION_VIEW, Uri.parse(TrackList.get(position).getTrackURI()));
                context.startActivity(spotify);
            } catch (Exception e) {
                Intent play = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.spotify.music"));
                context.startActivity(play);
            }
        }
    }

    @Override
    public void onDoubleClick(int position) {
        Log.d("swipe", String.format("onClickFrontView %d", position));
        PlayService.TRACK_END = false;
        PlayService.SKIP_NEXT = false;
        PlayOnSpotify(position);
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
                swipeListView.setEnabled(true);
                mRefreshActionBar.refreshActionBar(0);
            }
        }
    }
}