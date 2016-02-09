package com.pl4za.spotifytest;

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
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.github.mrengineer13.snackbar.SnackBar;
import com.pl4za.help.CustomListAdapterDrawer;
import com.pl4za.help.MyViewPager;
import com.pl4za.help.PageAdapter;
import com.pl4za.help.Params;
import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.NetworkRequests;
import com.pl4za.volley.AppController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    private int lastSelectedDrawerItem = -1;
    private int lastPagerPosition = 0;
    private static MyViewPager mViewPager;
    private static boolean landscape = false;
    private boolean mBound = false;
    private SearchView searchView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private Intent serviceIntent;
    private Context context;
    private PageAdapter viewPagerAdapter;

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            PlayService.LocalBinder binder = (PlayService.LocalBinder) service;
            playCtrl.setService(binder.getService());
            mBound = true;
            playCtrl.initializePlayer();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
        }
    };

    // Delegators
    private PlayCtrl playCtrl = PlayCtrl.getInstance();
    private QueueCtrl queueCtrl = QueueCtrl.getInstance();
    private TracklistCtrl tracklistCtrl = TracklistCtrl.getInstance();
    private SettingsManager settings = SettingsManager.getInstance();
    private SpotifyNetworkRequests spotifyNetwork = SpotifyNetworkRequests.getInstance();
    private ViewCtrl viewCtrl = ViewCtrl.getInstance();

    @Override
    public void onBackPressed() {
        if (!settings.getUserID().isEmpty()) {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            updateActionBar(1);
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings.setContext(this);
        context = getApplicationContext();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mViewPager = (MyViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(new MyViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                lastPagerPosition = position;
                clearSearch();
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
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.pager, new PlaceholderFragment()).commit();
        }
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if(lastPagerPosition==2) {
                    mDrawerToggle.setDrawerIndicatorEnabled(false);
                } else {
                    mDrawerToggle.setDrawerIndicatorEnabled(true);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setHomeButtonEnabled(true);
                }
                mDrawerToggle.syncState();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                setListItemChecked(lastSelectedDrawerItem, true);
            }

        };
        checkOrientation();
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        spotifyNetwork.addNetworkListener(this);
        viewCtrl.setActivityView(this);
        if (!settings.getUserID().isEmpty()) {
            DISABLE_LOGIN = true;
            ENABLE_REFRESH = true;
            supportInvalidateOptionsMenu();
            populateDrawer(settings.getPlaylistsNames());
            spotifyNetwork.getCurrentUserPlaylists(settings.getUserID(), settings.getAccessToken());
        } else {
            activateDrawer(false);
            Toast.makeText(context, "Please add a user", Toast.LENGTH_SHORT).show();
        }
        REFRESH = true;
        viewPagerAdapter = new PageAdapter(getSupportFragmentManager());
        viewPagerAdapter.setViewCtrl(viewCtrl);
    }

    @Override
    protected void onResume() {
        super.onResume();
        settings.setContext(this);
        lastSelectedDrawerItem = settings.getLastDrawerItem();
        lastPagerPosition = settings.getLastPagerPosition();
        if (!settings.getUserID().isEmpty()) {
            activateDrawer(true);
            populateDrawer(settings.getPlaylistsNames());
            spotifyNetwork.refreshToken(settings.getRefreshToken());
            updateActionBar(lastPagerPosition);
            mViewPager.setCurrentItem(lastPagerPosition);
            if (tracklistCtrl.hasTracks()) {
                mViewPager.setAdapter(viewPagerAdapter);
            }
        }
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
        if (!settings.getUserID().isEmpty() && tracklistCtrl.hasTracks()) {
            mViewPager.setAdapter(viewPagerAdapter);
            AppController.getInstance().cancelPendingRequests(Params.TAG_getCurrentUserPlaylists);
            spotifyNetwork.getCurrentUserPlaylists(settings.getUserID(), settings.getAccessToken());
        }
        updateActionBar(lastPagerPosition);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBound) {
            if (!playCtrl.isActive()) {
                playCtrl.destroyPlayer();
            }
            mBound = false;
            unbindService(mConnection);
        }
        settings.setLastDrawerItem(lastSelectedDrawerItem);
        settings.setLastPagerPosition(lastPagerPosition);
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
                onBackPressed();
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
            if (mBound) {
                playCtrl.initializePlayer();
            }
        }
        if (id == R.id.action_clear_queue) {
            queueCtrl.clear();
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
                String state = parts[1].split("=")[1];
                spotifyNetwork.exchangeCodeForToken(code);
            }
        }
    }

    @Override
    public void updateActionBar(int fragment) {
        //0: tracks, 1: queue, 2: player
        int index = mViewPager.getCurrentItem();
        lastPagerPosition=index;
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
            if (fragment == 2) {
                lastPagerPosition = 2;
                clearSearch();
                ENABLE_SEARCH = false;
                ENABLE_CLEAR = false;
                mDrawerToggle.setDrawerIndicatorEnabled(false);
                setTitle("Playing");
            }
        } else {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            if (index == 0) {
                boolean search = tracklistCtrl.hasTracks();
                if (tracklistCtrl.hasTracks()) {
                    setTitle(tracklistCtrl.getPlaylistName());
                }
                ENABLE_SEARCH = search;
                ENABLE_CLEAR = false;
            } else if (index == 1) {
                boolean search = queueCtrl.hasTracks();
                ENABLE_SEARCH = search;
                ENABLE_CLEAR = clear;
                setTitle("Queue");
            }
            if (fragment == 2) {
                lastPagerPosition = 2;
                ENABLE_SEARCH = false;
                ENABLE_CLEAR = false;
                mDrawerToggle.setDrawerIndicatorEnabled(false);
                setTitle("Playing");
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
    public void setViewPagerPosition(int position) {
        MainActivity.mViewPager.setCurrentItem(position);
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
            setListItemChecked(lastSelectedDrawerItem, true);
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
        if (!playCtrl.hasInstance()) {
            serviceIntent = new Intent(this, PlayService.class);
            this.startService(serviceIntent);
            bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
        }
    }

    private void selectItem(int position) {
        if (position >= 0) {
            tracklistCtrl.setPlaylistName(settings.getPlaylists().get(position).get(Params.playlist_name));
            mViewPager.setAdapter(viewPagerAdapter);
            mViewPager.setCurrentItem(0);
            String playlistID = settings.getPlaylists().get(position).get(Params.playlist_id);
            String userID = settings.getPlaylists().get(position).get(Params.playlist_user_id);
            viewCtrl.loadTracks(userID, playlistID);
            updateActionBar(0);
            mDrawerLayout.closeDrawers();
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
            return BitmapFactory.decodeResource(getResources(), R.drawable.ic_spotifyicon);
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

    private void setListItemChecked(final int position, final boolean confChange) {
        if (position > 0) { //Playlist not header
            TextView tv;
            if (lastSelectedDrawerItem != -1 && !confChange) {
                getViewByPosition(lastSelectedDrawerItem, mDrawerList).setBackgroundColor(Color.WHITE);
                tv = (TextView) getViewByPosition(lastSelectedDrawerItem, mDrawerList).findViewById(R.id.tvPlaylist);
                tv.setTextColor(getResources().getColor(R.color.darkgrey));
            }
            tv = (TextView) getViewByPosition(position, mDrawerList).findViewById(R.id.tvPlaylist);
            tv.setTextColor(Color.WHITE);
            getViewByPosition(position, mDrawerList).setBackgroundColor(getResources().getColor(R.color.colorSecondary));
            lastSelectedDrawerItem = position;
        }
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
    /*
    Spotify network related requests
     */

    @Override
    public void onProfilePictureReceived(Bitmap image) {
        if (image != null) {
            saveToInternalSorage(image);
        }
        spotifyNetwork.getCurrentUserPlaylists(settings.getUserID(), settings.getAccessToken());
        startService();
    }

    @Override
    public void onTokenReceived(String acessToken, String refreshToken) {
        settings.setAcessToken(acessToken);
        settings.setRefreshToken(refreshToken);
        spotifyNetwork.getCurrentUserProfile(acessToken);
        DISABLE_LOGIN = true;
        ENABLE_REFRESH = true;
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
        spotifyNetwork.getProfilePicture(userPictureURL);
    }

    @Override
    public void onPlaylistsReceived(ArrayList<Playlist> playlists) {
        if ((settings.getPlaylists().isEmpty() && !playlists.isEmpty()) || REFRESH) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            openDrawer();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            REFRESH = false;
        }
        if (!playlists.isEmpty()) {
            settings.setPlayLists(playlists);
            populateDrawer(settings.getPlaylistsNames());
        }
    }

    @Override
    public void onRandomArtistPictureURLReceived(String artistPictureURL) {
        settings.setRandomArtistImage(artistPictureURL);
        setPictureinDrawer();
    }

    @Override
    public void onPlaylistTracksReceived(String json) {

    }

    @Override
    public void onEtagUpdate(String etag) {

    }

    public static class PlaceholderFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
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
