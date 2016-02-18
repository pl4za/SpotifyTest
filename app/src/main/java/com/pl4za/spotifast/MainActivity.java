package com.pl4za.spotifast;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.github.mrengineer13.snackbar.SnackBar;
import com.pl4za.help.CustomListAdapterDrawer;
import com.pl4za.help.Params;
import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.NetworkRequests;
import com.pl4za.volley.AppController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

//TODO: finish undo option
//TODO: Logout & login in webview
//TODO: Switch recycleView with cardView
//TODO: animations and shadows (material design)
//TODO: custom audio controler
//TODO: transactions crash
public class MainActivity extends ActionBarActivity implements ActivityOptions, NetworkRequests {

    private static final String TAG = "MainActivity";

    private static boolean ENABLE_SEARCH = false;
    private static boolean ENABLE_CLEAR = false;
    private static boolean ENABLE_REFRESH = false;
    private static boolean DISABLE_LOGIN = false;
    private static boolean REFRESH = false;

    private static boolean landscape = false;
    private boolean mBound = false;
    private int lastSelection = -1;
    private SearchView searchView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private FragmentMain fragment;

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            PlayService.LocalBinder binder = (PlayService.LocalBinder) service;
            playCtrl.setService(binder.getService());
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
        }
    };

    // Delegators
    private final PlayCtrl playCtrl = PlayCtrl.getInstance();
    private final QueueCtrl queueCtrl = QueueCtrl.getInstance();
    private final TracklistCtrl tracklistCtrl = TracklistCtrl.getInstance();
    private final SettingsManager settings = SettingsManager.getInstance();
    private final SpotifyNetworkRequests spotifyNetwork = SpotifyNetworkRequests.getInstance();
    private final ViewCtrl viewCtrl = ViewCtrl.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings.setContext(this);
        viewCtrl.setActivityView(this);
        Context context = getApplicationContext();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                setListItemChecked(settings.getLastDrawerItem());
            }

        };
        checkOrientation();
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        spotifyNetwork.addNetworkListener(this);
        if (!settings.getUserID().isEmpty()) {
            DISABLE_LOGIN = true;
            ENABLE_REFRESH = true;
            supportInvalidateOptionsMenu();
            populateDrawer(settings.getPlaylistsNames());spotifyNetwork.getCurrentUserPlaylists(settings.getUserID(), settings.getAccessToken());
        } else {
            activateDrawer(false);
            Toast.makeText(context, "Please add a user", Toast.LENGTH_SHORT).show();
        }
        fragment = new FragmentMain();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!settings.getUserID().isEmpty()) {
            activateDrawer(true);
            populateDrawer(settings.getPlaylistsNames());
            spotifyNetwork.refreshToken(settings.getRefreshToken());
            loadPlaylist(settings.getLastPlaylistPosition());
            if (playCtrl.isPlaying() && settings.getPlayerOnTop()) {
                FragmentPlayer playFrag = new FragmentPlayer();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, playFrag, "FragmentPlayer")
                        .addToBackStack("FragmentPlayer")
                        .commit();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    private void activateDrawer(boolean status) {
        if (status) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        getSupportActionBar().setHomeButtonEnabled(status);
        getSupportActionBar().setDisplayShowHomeEnabled(status);
        getSupportActionBar().setDisplayHomeAsUpEnabled(status);
        mDrawerToggle.setDrawerIndicatorEnabled(status);
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
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        if (mBound) {
            if (!playCtrl.isPlaying()) {
                playCtrl.destroyPlayer();
            }
            mBound = false;
            unbindService(mConnection);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBound) {
            if (!playCtrl.isActive()) {
                playCtrl.destroyPlayer();
            } else {
                unbindService(mConnection);
            }
            mBound = false;
        }
        AppController.getInstance().cancelPendingRequests(Params.TAG_exchangeCodeForToken);
        AppController.getInstance().cancelPendingRequests(Params.TAG_getCurrentUserProfile);
        AppController.getInstance().cancelPendingRequests(Params.TAG_getCurrentUserProfilePicture);
        AppController.getInstance().cancelPendingRequests(Params.TAG_getCurrentUserPlaylists);
        AppController.getInstance().cancelPendingRequests(Params.TAG_getCurrentUserNextPlaylists);
        AppController.getInstance().cancelPendingRequests(Params.TAG_refreshToken);
        AppController.getInstance().cancelPendingRequests(Params.TAG_getArtistID);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_login).setVisible(!DISABLE_LOGIN);
        menu.findItem(R.id.action_search).setVisible(ENABLE_SEARCH);
        menu.findItem(R.id.action_refresh).setVisible(ENABLE_REFRESH);
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
                viewCtrl.updateFilter(query);
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
            if (!mDrawerToggle.isDrawerIndicatorEnabled()) {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
                getSupportFragmentManager().popBackStack();
                return false;
            }
        }
        if (id == R.id.action_login) {
            authorization();
            return true;
        }
        if (id == R.id.action_refresh) {
            AppController.getInstance().cancelPendingRequests(Params.TAG_getCurrentUserPlaylists);
            REFRESH = true;
            spotifyNetwork.getCurrentUserPlaylists(settings.getUserID(), settings.getAccessToken());
            showSnackBar("Refreshing..");
            if (mBound) {
                playCtrl.initializePlayer();
            }
        }
        if (id == R.id.action_clear_queue) {
            queueCtrl.clear();
            playCtrl.cancelNotification();
            updateActionBar(1);
            viewCtrl.updateView();
        }
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        mDrawerLayout.closeDrawers();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        String mTitle = title.toString();
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            viewCtrl.updateFilter(query);
        } else { // Browser login
            Uri uri = intent.getData();
            if (uri != null) {
                String[] parts = uri.toString().split("&");
                String code = parts[0].split("=")[1];
                spotifyNetwork.exchangeCodeForToken(code);
                REFRESH = true;
            }
        }
    }

    @Override
    public void updateActionBar(int fragmentNumber) {
        //0: tracks, 1: queue, 2: player
        //int index = mViewPager.getCurrentItem();
        //lastPagerPosition = index;
        boolean clear = queueCtrl.hasTracks();
        if (landscape) {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            ENABLE_SEARCH = true;
            ENABLE_CLEAR = clear;
            if (tracklistCtrl.hasTracks()) {
                setTitle(tracklistCtrl.getPlaylistName());
            }
            if (fragmentNumber == 2) {
                //lastPagerPosition = 2;
                ENABLE_SEARCH = false;
                ENABLE_CLEAR = false;
                mDrawerToggle.setDrawerIndicatorEnabled(false);
                setTitle("Playing");
                clearSearch();
            }
        } else {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            if (fragmentNumber == 0) {
                //boolean search = tracklistCtrl.hasTracks();
                if (tracklistCtrl.hasTracks()) {
                    setTitle(tracklistCtrl.getPlaylistName());
                }
                ENABLE_SEARCH = true;
                ENABLE_CLEAR = false;
            } else if (fragmentNumber == 1) {
                //boolean search = queueCtrl.hasTracks();
                ENABLE_SEARCH = false;
                ENABLE_CLEAR = clear;
                setTitle("Queue");
                clearSearch();
            }
            if (fragmentNumber == 2) {
                //lastPagerPosition = 2;
                ENABLE_SEARCH = false;
                ENABLE_CLEAR = false;
                mDrawerToggle.setDrawerIndicatorEnabled(false);
                setTitle("Playing");
                clearSearch();
            }
        }
        mDrawerToggle.syncState();
        supportInvalidateOptionsMenu();
    }

    @Override
    public void showSnackBar(String text) {
        new SnackBar.Builder(this)
                .withMessage(text)
                .withVisibilityChangeListener(new SnackBar.OnVisibilityChangeListener() {
                    @Override
                    public void onShow(int i) {
                        viewCtrl.hideFab(true);
                    }

                    @Override
                    public void onHide(int i) {
                        viewCtrl.hideFab(false);
                    }
                })
                .withDuration(SnackBar.SHORT_SNACK)
                .show();
    }

    @Override
    public boolean isLandscape() {
        return landscape;
    }

    private void checkOrientation() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            landscape = true;
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            landscape = false;
        }
    }

    private void populateDrawer(String[] list) {
        if (list.length > 0) {
            String path = getAplicationPath().toString();
            Bitmap profilepicture = loadImageFromStorage(path);
            CustomListAdapterDrawer mAdapter = new CustomListAdapterDrawer(this, list, settings.getUserID(), settings.getProduct(), profilepicture);
            mDrawerList.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
        }
    }

    public void clearSearch() {
        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.clearFocus();
        }
    }

    private void openDrawer() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, PlayService.class);
        this.startService(serviceIntent);
        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
    }

    private void loadPlaylist(int position) {
        if (position >= 0) {
            tracklistCtrl.setPlaylistName(settings.getPlaylists().get(position).get(Params.playlist_name));
            // Set playlist name if resuming to pager 0.
            if (settings.getLastPagerPosition() == 0) {
                setTitle(tracklistCtrl.getPlaylistName());
            }
            if (fragment == null) {
                fragment = new FragmentMain();
            }
            this.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment, "FragmentMain")
                    .commit();
            String playlistID = settings.getPlaylists().get(position).get(Params.playlist_id);
            String userID = settings.getPlaylists().get(position).get(Params.playlist_user_id);
            viewCtrl.loadTracks(userID, playlistID);
            viewCtrl.setViewPagerPosition(0);
            mDrawerLayout.closeDrawers();
        } else {
            openDrawer();
        }
    }

    private void authorization() {
        String url = "https://accounts.spotify.com/authorize/" + "?client_id=" + Params.CLIENT_ID + "&response_type=code" + "&redirect_uri=" + Params.REDIRECT_URI + "&scope=" + Params.SCOPES + "&state=34fFs29kd09";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private void saveToInternalSorage(Bitmap bitmapImage) {
        // path to /data/data/yourapp/app_data/imageDir
        File directory = getAplicationPath();
        File mypath = new File(directory, settings.getUserID());
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
            File f = new File(path, settings.getUserID());
            return BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            Log.e("MainActivity", "Profile image not found");
            return BitmapFactory.decodeResource(getResources(), R.mipmap.spotify_white);
        }
    }

    private File getAplicationPath() {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        return cw.getDir("imageDir", Context.MODE_PRIVATE);
    }

    private void setPictureinDrawer() {
        if (!settings.getRandomArtistImage().equals("")) {
            View view = getViewByPosition(0, mDrawerList);
            NetworkImageView thumbNail = (NetworkImageView) view.findViewById(R.id.ivArtistImage);
            ImageLoader imageLoader = AppController.getInstance().getImageLoader();
            thumbNail.setImageUrl(settings.getRandomArtistImage(), imageLoader);
            imageLoader.get(settings.getRandomArtistImage(), ImageLoader.getImageListener(
                    thumbNail, R.drawable.drawer_header, R.drawable.drawer_header));
        }
    }

    private void setListItemChecked(int position) {
        if (position > 0) {
            settings.setLastDrawerItem(position);
            TextView tv;
            if (lastSelection != -1) {
                getViewByPosition(lastSelection, mDrawerList).setBackgroundColor(Color.WHITE);
                tv = (TextView) getViewByPosition(lastSelection, mDrawerList).findViewById(R.id.tvPlaylist);
                tv.setTextColor(getResources().getColor(R.color.darkgrey));
            }
            tv = (TextView) getViewByPosition(position, mDrawerList).findViewById(R.id.tvPlaylist);
            tv.setTextColor(Color.WHITE);
            getViewByPosition(position, mDrawerList).setBackgroundColor(getResources().getColor(R.color.colorSecondary));
            lastSelection = position;
        }
    }

    private View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;
        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    /*
    Spotify network related requests
     */

    @Override
    public void onTokenReceived(String acessToken, String refreshToken) {
        settings.setAcessToken(acessToken);
        settings.setRefreshToken(refreshToken);
        spotifyNetwork.getCurrentUserProfile(acessToken);
        DISABLE_LOGIN = true;
        ENABLE_REFRESH = true;
        REFRESH = true;
        supportInvalidateOptionsMenu();
        spotifyNetwork.getCurrentUserPlaylists(settings.getUserID(), settings.getAccessToken());
        showSnackBar("Fetching playlists...");
    }

    @Override
    public void onTokenRefresh(String newToken) {
        settings.setAcessToken(newToken);
        spotifyNetwork.getCurrentUserPlaylists(settings.getUserID(), settings.getAccessToken());
        startService();
    }

    @Override
    public void onProfileReceived(String userID, String product, String userPictureURL) {
        settings.setUserID(userID);
        settings.setProduct(product);
        new getImageFromURL(userPictureURL).execute();
        REFRESH = true;
        spotifyNetwork.getCurrentUserPlaylists(userID, settings.getAccessToken());
        startService();
    }

    @Override
    public void onPlaylistsReceived(ArrayList<Playlist> playlists) {
        if (REFRESH) {
            activateDrawer(true);
            REFRESH = false;
            openDrawer();
        }
        if (!playlists.isEmpty()) {
            settings.setPlayLists(playlists);
            populateDrawer(settings.getPlaylistsNames());
        }
    }

    @Override
    public void onRandomArtistPictureURLReceived(String artistPictureURL) {
        if (!artistPictureURL.isEmpty()) {
            settings.setRandomArtistImage(artistPictureURL);
            setPictureinDrawer();
        }
    }

    @Override
    public void onPlaylistTracksReceived(String json) {

    }

    @Override
    public void onEtagUpdate(String etag) {

    }

    private class getImageFromURL extends AsyncTask<Void, Void, Bitmap> {

        final String url;

        public getImageFromURL(String url) {
            this.url = url;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            URL formedUrl = null;
            try {
                formedUrl = new URL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (formedUrl != null) {
                try {
                    return BitmapFactory.decodeStream(formedUrl.openConnection().getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap image) {
            super.onPostExecute(image);
            if (image!=null) {
                saveToInternalSorage(image);
            }
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position > 0) {
                loadPlaylist(position - 1);
                setListItemChecked(position);
                updateActionBar(0);
            }
        }
    }
}
