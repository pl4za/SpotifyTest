package com.pl4za.spotifytest;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.pl4za.help.CustomListAdapterDrawer;
import com.pl4za.help.MyViewPager;
import com.pl4za.help.PageAdapter;
import com.pl4za.interfaces.IrefreshActionBar;
import com.pl4za.interfaces.IrefreshToken;
import com.pl4za.interfaces.IspotifyPlayerOptions;
import com.pl4za.volley.AppController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

//TODO: finish undo option
//TODO: Logout & login in webview
//TODO: Switch recycleView with cardView
//TODO: animations and shadows (material design)
//TODO: custom audio controler
//TODO: transactions crash
public class MainActivity extends ActionBarActivity implements IrefreshToken, IrefreshActionBar {

    private static final int MY_SOCKET_TIMEOUT_MS = 8000;
    public static boolean ENABLE_UNDO_VALUE = false;
    public static int currentPage = 0;
    public static boolean isHomeAsUpEnabled = false;
    public static MyViewPager mViewPager;
    public static boolean ENABLE_SEARCH = false;
    public static boolean ENABLE_CLEAR = false;
    public static boolean ENABLE_REFRESH = true;
    public static boolean landscape = false;
    private static boolean DISABLE_LOGIN = false;
    private static boolean PLAYLISTS_EXIST = false;
    private static boolean PLAYLISTS_FORCE_WEBLOAD = false;
    private final List<Playlist> Playlists = new ArrayList<>();
    public String randomArtistPictureURL;
    Handler mHandler;
    // public boolean ENABLE_UNDO = false;
    private boolean mBound = false;
    private IspotifyPlayerOptions mSpotifyPlayerOptions;
    private SearchView searchView;
    private DrawerLayout mDrawerLayout;
    private RelativeLayout mDrawerRelativeLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private ProgressDialog pDialog;
    private PlayService playService;
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            PlayService.LocalBinder binder = (PlayService.LocalBinder) service;
            playService = binder.getService();
            mSpotifyPlayerOptions = playService;
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
        }
    };
    private Intent serviceIntent;
    private Context context;
    private int oldPosition = -1;

    private static String makeFragmentName(int index) {
        return "android:switcher:" + R.id.pager + ":" + index;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mViewPager = (MyViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(new MyViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
                mDrawerToggle.syncState();
                clearSearch();
                currentPage = position;
                if (position == 0) {
                    if (mViewPager.getAdapter() != null) {
                        ENABLE_CLEAR = false;
                        ENABLE_REFRESH = true;
                        ENABLE_SEARCH = !FragmentTracks.TrackList.isEmpty();
                        //ENABLE_UNDO = false;
                        if (PLAYLISTS_EXIST)
                            setTitle(PlayService.playlistName);
                    }
                } else if (position == 1) {
                    setTitle("Queue");
                    ENABLE_REFRESH = false;
                    ENABLE_SEARCH = !Queue.queue.isEmpty();
                    //ENABLE_UNDO = true && ENABLE_UNDO_VALUE;
                    if (!Queue.queue.isEmpty())
                        ENABLE_CLEAR = true;
                }/* else if (position == 2) {
                    ENABLE_REFRESH = false;
                    ENABLE_SEARCH = false;
                    ENABLE_CLEAR = false;
                    //ENABLE_UNDO = false;
                    setTitle("Playing");
                }*/
                refreshActionBar();
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerRelativeLayout = (RelativeLayout) findViewById(R.id.rlDrawerListView);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.pager, new PlaceholderFragment()).commit();
        }
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //ActivityCompat.invalidateOptionsMenu(MainActivity.this);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (oldPosition != -1) {
                    setListItemChecked(oldPosition, true);
                }
                if (randomArtistPictureURL != null) {
                    setPictureinDrawer(randomArtistPictureURL);
                }
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mHandler = new Handler();
        checkOrientation();
        startApp();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();
    }

    private void checkOrientation() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            landscape = true;
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            landscape = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!Queue.queue.isEmpty()) {
            FragmentPlayer playFrag = new FragmentPlayer();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, playFrag, "FragmentPlayer")
                    .addToBackStack(null)
                    .commit();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            MainActivity.isHomeAsUpEnabled = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PLAYLISTS_FORCE_WEBLOAD = false;
        if (tokenExists()) {
            startService();
        }
    }

    @Override
    public void onBackPressed() {
        Log.i("MainActivity", "Back pressed");
        if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
            layoutFromPlaying();
        } else {
            super.onBackPressed();
        }
    }

    private void layoutFromPlaying() {
        getSupportFragmentManager().popBackStack();
        if (isHomeAsUpEnabled) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            mDrawerToggle.syncState();
            if (!FragmentTracks.TrackList.isEmpty()) {
                ENABLE_SEARCH = true;
            }
            ENABLE_REFRESH = true;
            if (currentPage == 0) {
                setTitle(PlayService.playlistName);
            } else {
                setTitle("Queue");
                ENABLE_REFRESH = false;
                if (!Queue.queue.isEmpty()) {
                    ENABLE_SEARCH = true;
                    ENABLE_CLEAR = true;
                } else {
                    ENABLE_SEARCH = false;
                    ENABLE_CLEAR = false;
                }
            }
            isHomeAsUpEnabled = false;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            landscape = true;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            landscape = false;
        }
        //PageAdapter viewPagerAdapter = new PageAdapter(getSupportFragmentManager());
        //mViewPager.setAdapter(viewPagerAdapter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hidePDialog();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        hidePDialog();
        AppController.getInstance().cancelPendingRequests(getString(R.string.TAG_exchangeCodeForToken));
        AppController.getInstance().cancelPendingRequests(getString(R.string.TAG_getCurrentUserProfile));
        AppController.getInstance().cancelPendingRequests(getString(R.string.TAG_getCurrentUserPlaylists));
        AppController.getInstance().cancelPendingRequests(getString(R.string.TAG_refreshToken));
        AppController.getInstance().cancelPendingRequests(getString(R.string.TAG_getArtistID));
        if (serviceIntent != null) {
            if (!playService.notificationActive) {
                stopService(serviceIntent);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_login).setVisible(!DISABLE_LOGIN);
        menu.findItem(R.id.action_search).setVisible(ENABLE_SEARCH);
        menu.findItem(R.id.action_refresh).setVisible(ENABLE_REFRESH);
        //menu.findItem(R.id.action_undo).setVisible(ENABLE_UNDO);
        menu.findItem(R.id.action_clear_queue).setVisible(ENABLE_CLEAR);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                search(query);
                menu.findItem(R.id.action_search).collapseActionView();
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    search("");
                } else
                    search(newText);
                return true;
            }

            public void search(String query) {
                ////Log.i("MainActivity", "Search: "+ query+" - "+size);
                if (currentPage == 0) {
                    FragmentTracks fragment = (FragmentTracks) getSupportFragmentManager().findFragmentByTag(makeFragmentName(0));
                    if (fragment != null)
                        fragment.mAdapter.getFilter().filter(query);
                } else if (currentPage == 1) {
                    FragmentQueue fragment = (FragmentQueue) getSupportFragmentManager().findFragmentByTag(makeFragmentName(1));
                    if (fragment != null)
                        fragment.mAdapter.getFilter().filter(query);
                }
            }

        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            return true;
        }
        if (id == android.R.id.home) {
            Log.i("MainActivity", "status: " + isHomeAsUpEnabled);
            if (isHomeAsUpEnabled) {
                if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    onBackPressed();
                }
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                mDrawerToggle.syncState();
                if (currentPage == 0) {
                    if (!FragmentTracks.TrackList.isEmpty()) {
                        ENABLE_SEARCH = true;
                    } else {
                        ENABLE_SEARCH = false;
                    }
                    ENABLE_REFRESH = true;
                    setTitle(PlayService.playlistName);
                } else {
                    if (!Queue.queue.isEmpty()) {
                        ENABLE_SEARCH = true;
                        ENABLE_CLEAR = true;
                    } else {
                        ENABLE_SEARCH = false;
                        ENABLE_CLEAR = false;
                    }
                    ENABLE_REFRESH = false;
                    setTitle("Queue");
                }
                refreshActionBar();
                isHomeAsUpEnabled = false;
                return true;
            }
        }
        if (id == R.id.action_login) {
            authorization();
            return true;
        }
        if (id == R.id.action_refresh) {
            AppController.getInstance().cancelPendingRequests(getString(R.string.TAG_getCurrentUserPlaylists));
            PLAYLISTS_EXIST = false;
            PLAYLISTS_FORCE_WEBLOAD = true;
            startApp();
        }
        if (id == R.id.action_clear_queue) {
            copyTracklistBack();
            Queue.clearQueue();
            PlayService.clearQueue();
            if (serviceIntent != null) {
                if (playService.notificationActive) {
                    PlayService.destroyPlayer();
                }
            }
            ENABLE_CLEAR = false;
            ENABLE_SEARCH = false;
            refreshActionBar();
        }
        /*
        if (id == R.id.action_undo) {
            Queue.restoreLast();
            ENABLE_UNDO_VALUE = false;
            ENABLE_UNDO = false;
            refreshActionBar();
        }*/
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        mDrawerLayout.closeDrawers();
        return super.onOptionsItemSelected(item);
    }

    private void copyTracklistBack() {
        FragmentTracks.TrackList.addAll(Queue.queue);
    }

    @Override
    public void refreshActionBar() {
        supportInvalidateOptionsMenu();
    }

    @Override
    public void setTitle(CharSequence title) {
        String mTitle = title.toString();
        getSupportActionBar().setTitle(mTitle);
        refreshActionBar();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //Log.i("MainActivity", "1: " + Intent.ACTION_SEARCH + " - " + intent.getAction());
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            //Log.i("MainActivity", "SEARCHING!");
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (currentPage == 0) {
                FragmentTracks fragment = (FragmentTracks) getSupportFragmentManager().findFragmentByTag(makeFragmentName(0));
                if (fragment != null)
                    fragment.mAdapter.getFilter().filter(query);
            } else if (currentPage == 1) {
                FragmentQueue fragment = (FragmentQueue) getSupportFragmentManager().findFragmentByTag(makeFragmentName(1));
                if (fragment != null)
                    fragment.mAdapter.getFilter().filter(query);
            }
        } else {
            Uri uri = intent.getData();
            if (uri != null) {
                String code = getAndSaveCodeAndState(uri.toString());
                //Log.i("MainActivity", "Login: " + code);
                exchangeCodeForToken(code);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("PLAYLIST_NUMBER", oldPosition);
        savedInstanceState.putString("ARTIST_PICTURE_URL", randomArtistPictureURL);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        oldPosition = savedInstanceState.getInt("PLAYLIST_NUMBER");
        randomArtistPictureURL = savedInstanceState.getString("ARTIST_PICTURE_URL");
    }

    private void startApp() {
        if (tokenExists()) {
            refreshActionBar();
            Playlists.clear();
            DISABLE_LOGIN = true;
            getPlaylistsFromSettings();
            if (PLAYLISTS_EXIST) {
                if (Queue.queue.isEmpty()) {
                    openDrawer();
                }
            }
        }
        if (isNetworkOnline()) {
            if (tokenExists()) {
                getCurrentUserPlaylists();
            } else {
                Toast.makeText(context, "Please add a user", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Internet connection required.", Toast.LENGTH_SHORT).show();
        }
        populateDrawer(convertListToArray(Playlists));
    }

    private void populateDrawer(String[] list) {
        String path = getAplicationPath().toString();
        Bitmap profilepicture = loadImageFromStorage(path);
        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
        CustomListAdapterDrawer mAdapter = new CustomListAdapterDrawer(this, list, sharedPref.getString("user_id", "Spot"), sharedPref.getString("product", "Please login.."), profilepicture);
        mDrawerList.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        if (oldPosition != -1) {
            setListItemChecked(oldPosition, true);
        }
        if (randomArtistPictureURL != null) {
            setPictureinDrawer(randomArtistPictureURL);
        }
    }

    public void clearSearch() {
        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.clearFocus();
        }
    }

    private boolean isNetworkOnline() {
        boolean status = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(0);
            if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                status = true;
            } else {
                netInfo = cm.getNetworkInfo(1);
                if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED)
                    status = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return status;

    }

    private void openDrawer() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    private void savePlaylistsToSettings() {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.Playlists), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("list_size", Playlists.size());
        int i = 0;
        for (Playlist a : Playlists) {
            //Log.i("MainActivity", a.getName());
            editor.putString("playlist_" + i, a.getName());
            editor.putString("playlistID_" + i, a.getid());
            editor.putString("playlistUSERID_" + i, a.getUserID());
            i++;
        }
        editor.apply();
    }

    private void getPlaylistsFromSettings() {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.Playlists), Context.MODE_PRIVATE);
        int size = sharedPref.getInt("list_size", 0);
        //Log.i("MainActivity", "Size: " + String.valueOf(size));
        for (int i = 0; i < size; i++) {
            String name = sharedPref.getString("playlist_" + i, null);
            String id = sharedPref.getString("playlistID_" + i, null);
            String userId = sharedPref.getString("playlistUSERID_" + i, null);
            Playlists.add(new Playlist(id, name, userId));
        }
        PLAYLISTS_EXIST = Playlists.size() > 0;
    }

    private void hidePDialog() {
        if (pDialog != null) {
            pDialog.dismiss();
            pDialog = null;
        }
    }

    private void createPDialog(String text) {
        if (pDialog == null) {
            pDialog = new ProgressDialog(this);
            pDialog.setMessage(text);
            // pDialog.setCancelable(false);
            pDialog.show();
        } else {
            pDialog.show();
        }
    }

    private boolean tokenExists() {
        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
        String access_token = sharedPref.getString(getString(R.string.access_token), "");
        return !access_token.equals("");
    }

    private void startService() {
        if (playService == null) {
            SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
            String access_token = sharedPref.getString(getString(R.string.access_token), "");
            Bundle b = new Bundle();
            b.putString("acess_token", access_token);
            serviceIntent = new Intent(this, PlayService.class);
            serviceIntent.putExtras(b);
            this.startService(serviceIntent);
            bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
            PlayService.tokenRefreshListener(this);
        }
    }

    private void selectItem(int position) {
        //Log.i("MainActivity", "Tamanho playlist: " + Playlists.size());
        if (position >= 0 && position < Playlists.size()) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                layoutFromPlaying();
            }
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            isHomeAsUpEnabled = false;
            mDrawerList.setSelection(position);
            mDrawerList.setSelected(true);
            boolean reset = false;
            //Log.i("MainActivity", PlayService.playlistName + " - " + Playlists.get(position).getName());
            if (!PlayService.playlistName.equals(Playlists.get(position).getName())) {
                reset = true;
            }
            PlayService.playlistName = Playlists.get(position).getName();
            setTitle(PlayService.playlistName);
            AppController.getInstance().cancelPendingRequests(getString(R.string.TAG_getCurrentUserPlaylists));
            AppController.getInstance().cancelPendingRequests(getString(R.string.TAG_getSelectedPlaylistTracks));
            String playlistID = Playlists.get(position).getid();
            String userID = Playlists.get(position).getUserID();
            //Log.i("MainActivity", Playlists.get(position).getid() + " - " + Playlists.get(position).getUserID() + " - " + Playlists.get(position).getName());
            PageAdapter viewPagerAdapter = new PageAdapter(getSupportFragmentManager());
            mViewPager.setAdapter(viewPagerAdapter);
            selectPlaylist(userID, playlistID, reset);
            mDrawerLayout.closeDrawers();
        }
    }

    private void selectPlaylist(String userID, String playlistID, boolean reset) {
        FragmentTracks FragmentTracks = (FragmentTracks) getSupportFragmentManager().findFragmentByTag(makeFragmentName(0));
        if (FragmentTracks != null) {
            FragmentTracks.setURL(userID, playlistID);
            if (reset) {
                com.pl4za.spotifytest.FragmentTracks.TrackList.clear();
                FragmentTracks.load();
            }
            Queue.refreshActionBarListener(this);
        }
        ENABLE_SEARCH = true;
        refreshActionBar();
    }

    private String[] convertListToArray(List<Playlist> Playlists) {
        String[] list = new String[Playlists.size()];
        int i = 0;
        for (Playlist a : Playlists) {
            list[i] = a.getName();
            i++;
        }
        return list;
    }

    private void authorization() {
        String url = "https://accounts.spotify.com/authorize/" + "?client_id=" + getString(R.string.CLIENT_ID) + "&response_type=code" + "&redirect_uri=" + getString(R.string.REDIRECT_URI) + "&scope=" + getString(R.string.SCOPES) + "&state=34fFs29kd09";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private String getAndSaveCodeAndState(String uri) {
        String[] parts = uri.split("&");
        String CODE = parts[0].split("=")[1];
        String STATE = parts[1].split("=")[1];
        Context context = this;
        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.code), CODE);
        editor.putString(getString(R.string.state), STATE);
        editor.apply();
        return CODE;
    }

    private void exchangeCodeForToken(final String code) {
        String url = "https://accounts.spotify.com/api/token";
        createPDialog("Loading...");
        Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                try {
                    JSONObject jsonObj = new JSONObject(list);
                    String access_token = jsonObj.getString("access_token");
                    String refresh_token = jsonObj.getString("refresh_token");
                    // Save to prefs
                    SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.access_token), access_token);
                    editor.putString(getString(R.string.refresh_token), refresh_token);
                    ////Log.i("exchangeCodeForToken", "Acess token :" + access_token + "\n" + "Refresh token :" + refresh_token);
                    editor.apply();
                    hidePDialog();
                    getCurrentUserProfile();
                    DISABLE_LOGIN = true;
                    refreshActionBar();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(getString(R.string.TAG_exchangeCodeForToken), "Error: " + error.getMessage());
                hidePDialog();
            }
        };

        StringRequest fileRequest = new StringRequest(Request.Method.POST, url, jsonListerner, errorListener) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("grant_type", "authorization_code");
                params.put("code", code);
                params.put("redirect_uri", getString(R.string.REDIRECT_URI));
                params.put("client_id", getString(R.string.CLIENT_ID));
                params.put("client_secret", getString(R.string.CLIENT_SECRET));
                return params;
            }
        };
        fileRequest.setShouldCache(false);
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        AppController.getInstance().addToRequestQueue(fileRequest, getString(R.string.TAG_exchangeCodeForToken));
    }

    public void refreshToken(final String location) {
        String url = "https://accounts.spotify.com/api/token";
        Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                try {
                    JSONObject jsonObj = new JSONObject(list);
                    String access_token = jsonObj.getString("access_token");
                    // Save to prefs
                    SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(context.getString(R.string.access_token), access_token);
                    //Log.i("RefreshToken", "New token: " + access_token);
                    editor.apply();
                    switch (location) {
                        case "getTracksFailed":
                            updatePlaylistTracks();
                            break;
                        case "getCurrentUserPlaylists":
                            getCurrentUserPlaylists();
                            break;
                        case "PlayService":
                            mSpotifyPlayerOptions.initializePlayer();
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(getString(R.string.TAG_refreshToken), "Error: " + error.getMessage());
            }
        };

        StringRequest fileRequest = new StringRequest(Request.Method.POST, url, jsonListerner, errorListener) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
                String refresh_token = sharedPref.getString(context.getString(R.string.refresh_token), "");
                ////Log.i("RefreshToken", "Refresh Token: " + refresh_token);
                Map<String, String> params = new HashMap<>();
                params.put("grant_type", "refresh_token");
                params.put("refresh_token", refresh_token);
                params.put("client_id", getString(R.string.CLIENT_ID));
                params.put("client_secret", getString(R.string.CLIENT_SECRET));
                return params;
            }
        };
        fileRequest.setShouldCache(false);
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        AppController.getInstance().addToRequestQueue(fileRequest, getString(R.string.TAG_refreshToken));
    }

    private void getCurrentUserProfile() {
        String url = "https://api.spotify.com/v1/me";
        //createPDialog("Loading user...");
        Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                try {
                    JSONObject jsonObj = new JSONObject(list);
                    String userID = jsonObj.getString("id");
                    String product = jsonObj.getString("product");
                    String profilePicture = "";
                    try {
                        JSONArray profilePictures = jsonObj.getJSONArray("images");
                        profilePicture = profilePictures.getJSONObject(0).getString("url");
                    } catch (JSONException e) {
                        Log.e("json", "No profile image uri.");
                    }
                    // Save to prefs
                    SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.user_id), userID);
                    editor.putString(getString(R.string.product), product);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        new getProfilePicture(profilePicture).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        new getProfilePicture(profilePicture).execute();
                    }
                    //Log.i("GetCurrentUser", "User id: " + userID + " Product: " + product + " Image: " + profilePicture);
                    editor.apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(getString(R.string.TAG_getCurrentUserProfile), "Error: " + error.getMessage());
                //hidePDialog();
                refreshToken("getUserFailed");
            }
        };

        StringRequest fileRequest = new StringRequest(Request.Method.GET, url, jsonListerner, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
                String access_token = sharedPref.getString(getString(R.string.access_token), "");
                //Log.i("GetCurrentUser", "Access Token: " + access_token);
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + access_token);
                return headers;
            }

            @Override
            public Priority getPriority() {
                return Priority.HIGH;
            }
        };
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(fileRequest, getString(R.string.TAG_getCurrentUserProfile));
    }

    private void getCurrentUserPlaylists() {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
        String user_id = sharedPref.getString(getString(R.string.user_id), "");
        String url = "https://api.spotify.com/v1/users/" + user_id + "/playlists";
        if (PLAYLISTS_FORCE_WEBLOAD)
            createPDialog("Fetching playlists...");
        //Log.i("GetCurrentUserPlaylists", "Getting playlists...");
        Listener<String> jsonListerner = new Response.Listener<String>() {
            @Override
            public void onResponse(String list) {
                try {
                    Playlists.clear();
                    JSONObject json = new JSONObject(list);
                    JSONArray playlists = json.getJSONArray("items");
                    int size = playlists.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject ids = playlists.getJSONObject(i);
                        //Log.i("MainActivity", ids.getString("id") + " - " + ids.getString("name") + " - " + ids.getJSONObject("owner").getString("id"));
                        Playlists.add(new Playlist(ids.getString("id"), ids.getString("name"), ids.getJSONObject("owner").getString("id")));
                    }
                    if (Playlists.size() > 0) {
                        PLAYLISTS_EXIST = true;
                        String[] Arrayplaylists = convertListToArray(Playlists);
                        savePlaylistsToSettings();
                        populateDrawer(Arrayplaylists);
                        if (!mDrawerLayout.isDrawerOpen(mDrawerRelativeLayout)) {
                            if (PLAYLISTS_FORCE_WEBLOAD) {
                                openDrawer();
                            }
                        }
                    }
                    PLAYLISTS_FORCE_WEBLOAD = false;
                    hidePDialog();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(getString(R.string.TAG_getCurrentUserPlaylists), "Error: " + error.getMessage());
                hidePDialog();
                PLAYLISTS_FORCE_WEBLOAD = false;
                refreshToken("getCurrentUserPlaylists");
            }
        };
        StringRequest fileRequest = new StringRequest(Request.Method.GET, url, jsonListerner, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
                String access_token = sharedPref.getString(getString(R.string.access_token), "");
                ////Log.i("GetCurrentUserPlaylists", "Access Token for playlists: " + access_token);
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + access_token);
                return headers;
            }
        };
        fileRequest.setShouldCache(false);
        fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        AppController.getInstance().addToRequestQueue(fileRequest, getString(R.string.TAG_getCurrentUserPlaylists));
    }

    public void getRandomArtistPicture() {
        if (!FragmentTracks.TrackList.isEmpty()) {
            String url = "https://api.spotify.com/v1/artists/" + getRandomArtistId();
            Listener<String> jsonListerner = new Response.Listener<String>() {
                @Override
                public void onResponse(String list) {
                    try {
                        JSONObject json = new JSONObject(list);
                        try {
                            JSONArray artistImages = json.getJSONArray("images");
                            JSONObject artistimageObject = artistImages.getJSONObject(1);
                            setPictureinDrawer(artistimageObject.getString("url"));
                            randomArtistPictureURL = artistimageObject.getString("url");
                        } catch (JSONException e) {
                            Log.e("json", "No artist image...");
                            getRandomArtistPicture();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };
            ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    getRandomArtistPicture();
                }
            };
            StringRequest fileRequest = new StringRequest(Request.Method.GET, url, jsonListerner, errorListener);
            fileRequest.setShouldCache(false);
            fileRequest.setRetryPolicy(new DefaultRetryPolicy(
                    MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            AppController.getInstance().addToRequestQueue(fileRequest, getString(R.string.TAG_getArtistID));
        }
    }

    private void updatePlaylistTracks() {
        FragmentTracks FragmentTracks = (FragmentTracks) getSupportFragmentManager().findFragmentByTag(makeFragmentName(0));
        if (FragmentTracks != null) {
            //Log.i("MainActivity", "Updating tracks after token refresh");
            com.pl4za.spotifytest.FragmentTracks.TrackList.clear();
            FragmentTracks.load();
        }
    }

    private void saveToInternalSorage(Bitmap bitmapImage) {
        // path to /data/data/yourapp/app_data/imageDir
        File directory = getAplicationPath();
        // Create imageDir
        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
        String userId = sharedPref.getString(getString(R.string.user_id), "profile.jpg");
        File mypath = new File(directory, userId);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Log.e("MainActivity", e.getMessage());
        }
    }

    private Bitmap loadImageFromStorage(String path) {
        try {
            SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
            String userId = sharedPref.getString(getString(R.string.user_id), "profile.jpg");
            File f = new File(path, userId);
            return BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            Log.e("MainActivity", "Profile image not found");
            return BitmapFactory.decodeResource(getResources(), R.drawable.ic_spotifyicon);
        }
    }

    private File getAplicationPath() {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        return cw.getDir("imageDir", Context.MODE_PRIVATE);
    }

    public String getRandomArtistId() {
        Random rand = new Random();
        return FragmentTracks.TrackList.get((rand.nextInt(FragmentTracks.TrackList.size()))).getID();
    }

    private void setPictureinDrawer(String pictureinDrawer) {
        final String str = pictureinDrawer;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                View view = getViewByPosition(0, mDrawerList);
                NetworkImageView thumbNail = (NetworkImageView) view.findViewById(R.id.ivArtistImage);
                ImageLoader imageLoader = AppController.getInstance().getImageLoader();
                thumbNail.setImageUrl(str, imageLoader);
                imageLoader.get(str, ImageLoader.getImageListener(
                        thumbNail, R.drawable.drawer_header, R.drawable.drawer_header));
            }
        });
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;
        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    private void setListItemChecked(int position, boolean configurationChanged) {
        final int pos = position;
        final boolean confChanged = configurationChanged;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (pos > 0) {
                    TextView tv;
                    if (oldPosition != -1 && !confChanged) {
                        getViewByPosition(oldPosition, mDrawerList).setBackgroundColor(Color.WHITE);
                        tv = (TextView) getViewByPosition(oldPosition, mDrawerList).findViewById(R.id.tvPlaylist);
                        tv.setTextColor(getResources().getColor(R.color.darkgrey));
                    }
                    tv = (TextView) getViewByPosition(pos, mDrawerList).findViewById(R.id.tvPlaylist);
                    tv.setTextColor(Color.WHITE);
                    getViewByPosition(pos, mDrawerList).setBackgroundColor(getResources().getColor(R.color.colorSecondary));
                    oldPosition = pos;
                }
            }
        });

    }

    public static class PlaceholderFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }
    }

    private class getProfilePicture extends AsyncTask<Void, Void, Void> {

        final String url;

        public getProfilePicture(String url) {
            this.url = url;
        }


        @Override
        protected Void doInBackground(Void... params) {
            URL formedUrl = null;
            try {
                formedUrl = new URL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (formedUrl != null) {
                try {
                    Bitmap image = BitmapFactory.decodeStream(formedUrl.openConnection().getInputStream());
                    saveToInternalSorage(image);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            hidePDialog();
            PLAYLISTS_FORCE_WEBLOAD = true;
            getCurrentUserPlaylists();
            startService();
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position > 0) {
                selectItem(position - 1);
                setListItemChecked(position, false);
            }
        }
    }
}
