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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.pl4za.help.CustomListAdapterDrawer;
import com.pl4za.help.MyViewPager;
import com.pl4za.help.PageAdapter;
import com.pl4za.help.Params;
import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.FragmentOptions;
import com.pl4za.interfaces.NetworkRequests;
import com.pl4za.volley.AppController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

//TODO: finish undo option
//TODO: Logout & login in webview
//TODO: Switch recycleView with cardView
//TODO: animations and shadows (material design)
//TODO: custom audio controler
//TODO: transactions crash
public class MainActivity extends ActionBarActivity implements ActivityOptions, NetworkRequests {

    private static boolean ENABLE_SEARCH = false;
    private static boolean ENABLE_CLEAR = false;
    private static boolean ENABLE_REFRESH = true;
    private static boolean DISABLE_LOGIN = false;

    public int currentPage, oldPosition = 0;
    public static boolean isHomeAsUpEnabled = false;
    public static MyViewPager mViewPager;
    public String randomArtistPictureURL;
    public static boolean landscape = false;
    private boolean mBound = false;
    private SearchView searchView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private Intent serviceIntent;
    private Context context;

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
    private SettingsManager settings = SettingsManager.getInstance();
    private SpotifyNetworkRequests spotifyNetwork = SpotifyNetworkRequests.getInstance();
    private ViewCtrl viewCtrl = ViewCtrl.getInstance();

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
                currentPage = position;
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
        checkOrientation();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();
        settings.setContext(this);
        spotifyNetwork.addNetworkListener(this);
        /*
        App start
         */
        if (!settings.getUserID().isEmpty()) {
            updateActionBar(false, false);
            DISABLE_LOGIN = true;
            supportInvalidateOptionsMenu();
            populateDrawer(settings.getPlaylistsNames());
            spotifyNetwork.getCurrentUserPlaylists(settings.getUserID(), settings.getAccessToken());
        }
        else {
            Toast.makeText(context, "Please add a user", Toast.LENGTH_SHORT).show();
            //TODO: disable drawer
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playCtrl.isActive()) {
            FragmentPlayer playFrag = new FragmentPlayer();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, playFrag, "FragmentPlayer")
                    .addToBackStack(null)
                    .commit();
            updateActionBar(false, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!settings.getUserID().isEmpty()) {
            spotifyNetwork.refreshToken(settings.getRefreshToken());
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
        if (!playCtrl.isActive()) {
            playCtrl.destroyPlayer();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        if (!playCtrl.isActive()) {
            playCtrl.destroyPlayer();
            stopService(serviceIntent);
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
                ////Log.i("MainActivity", "Search: "+ query+" - "+size);
                if (currentPage == 0 || landscape) {
                    FragmentOptions fragment = (FragmentTracks) getSupportFragmentManager().findFragmentByTag(makeFragmentName(0));
                    if (fragment != null)
                        fragment.updateFilter(query);
                } else if (currentPage == 1 || landscape) {
                    FragmentOptions fragment = (FragmentQueue) getSupportFragmentManager().findFragmentByTag(makeFragmentName(1));
                    if (fragment != null)
                        fragment.updateFilter(query);
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
        }
        if (id == R.id.action_login) {
            authorization();
            return true;
        }
        if (id == R.id.action_refresh) {
            AppController.getInstance().cancelPendingRequests(Params.TAG_getCurrentUserPlaylists);
            spotifyNetwork.getCurrentUserPlaylists(settings.getUserID(), settings.getAccessToken());
        }
        if (id == R.id.action_clear_queue) {
            queueCtrl.clearQueue();
            updateActionBar(false, false);
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

    @Override
    public void updateActionBar(boolean search, boolean clear) {
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        clearSearch();
        if (landscape) {
            ENABLE_CLEAR = clear;
            ENABLE_REFRESH = false;
            ENABLE_SEARCH = search;
        } else {
            ENABLE_CLEAR = clear;
            ENABLE_REFRESH = false;
            ENABLE_SEARCH = search;
        }
        // TODO: setTitle(PlayService.playlistName);
        mDrawerToggle.syncState();
        supportInvalidateOptionsMenu();
    }

    private static String makeFragmentName(int index) {
        return "android:switcher:" + R.id.pager + ":" + index;
    }

    private void checkOrientation() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            landscape = true;
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            landscape = false;
        }
    }

    private void populateDrawer(String[] list) {
        String path = getAplicationPath().toString();
        Bitmap profilepicture = loadImageFromStorage(path);
        CustomListAdapterDrawer mAdapter = new CustomListAdapterDrawer(this, list, settings.getUserID(), settings.getProduct(), profilepicture);
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

    private void startService() {
        if (!playCtrl.hasInstance()) {
            serviceIntent = new Intent(this, PlayService.class);
            this.startService(serviceIntent);
            bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
        }
    }

    private void selectItem(int position) {
        if (position >= 0) {
            mDrawerList.setSelection(position);
            mDrawerList.setSelected(true);
            //setTitle(PlayService.playlistName);
            PageAdapter viewPagerAdapter = new PageAdapter(getSupportFragmentManager());
            mViewPager.setAdapter(viewPagerAdapter);
            String playlistID = settings.getPlaylists().get(position).get(Params.playlist_id);
            String userID = settings.getPlaylists().get(position).get(Params.playlist_user_id);
            viewCtrl.loadTracks(userID, playlistID);
            updateActionBar(false, false);
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

    private void setPictureinDrawer(String pictureinDrawer) {
        final String str = pictureinDrawer;
        new Handler().post(new Runnable() {
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
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (pos > 0) {
                    TextView tv;
                    if (oldPosition != -1 && !confChanged) {
                        getViewByPosition(oldPosition, mDrawerList).setBackgroundColor(Color.WHITE);
                        tv = (TextView) getViewByPosition(oldPosition, mDrawerList).findViewById(R.id.tvPlaylist);
                        //TODO: tv.setTextColor(getResources().getColor(R.color.darkgrey));
                    }
                    tv = (TextView) getViewByPosition(pos, mDrawerList).findViewById(R.id.tvPlaylist);
                    tv.setTextColor(Color.WHITE);
                    getViewByPosition(pos, mDrawerList).setBackgroundColor(getResources().getColor(R.color.colorSecondary));
                    oldPosition = pos;
                }
            }
        });

    }

    /*
    Spotify network related requests
     */

    @Override
    public void onProfilePictureReceived(Bitmap image) {
        if (image!=null) {
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
        if (playlists.size() > 0) {
            settings.setPlayLists(playlists);
            populateDrawer(settings.getPlaylistsNames());
            openDrawer();
        }
    }

    @Override
    public void onRandomArtistPictureURLReceived(String artistPictureURL) {
        setPictureinDrawer(artistPictureURL);
        randomArtistPictureURL = artistPictureURL;
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
