package com.pl4za.spotifytest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.github.mrengineer13.snackbar.SnackBar;
import com.melnykov.fab.FloatingActionButton;
import com.pl4za.help.CustomListAdapter;
import com.pl4za.interfaces.ISwipeListener;
import com.pl4za.interfaces.IqueueRefresh;
import com.pl4za.interfaces.IspotifyPlayerOptions;

import java.util.List;

public class FragmentQueue extends Fragment implements IqueueRefresh, ISwipeListener {

    private static IspotifyPlayerOptions mInitializePlayer;
    private static List<Track> queue;
    public CustomListAdapter mAdapter;
    private ListView swipeListView;
    private Context context;
    private ProgressDialog pDialog;
    private int trackToRemove = 0;
    private boolean animate = true;
    private FloatingActionButton fabPlay;
    private FloatingActionButton fabTracks;

    public static void updateQueueList(List<Track> queueUpdated) {
        queue = queueUpdated;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        queue = Queue.queue;
        context = getActivity();
        View view = inflater.inflate(R.layout.fragment_queue, container, false);
        swipeListView = (ListView) view.findViewById(R.id.listview);
        fabPlay = (FloatingActionButton) view.findViewById(R.id.fab);
        fabTracks = (FloatingActionButton) view.findViewById(R.id.fabTracks);
        fabPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentPlayer playFrag = new FragmentPlayer();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, playFrag, "FragmentPlayer")
                        .addToBackStack(null)
                        .commit();
                ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                MainActivity.isHomeAsUpEnabled = true;
            }
        });
        fabTracks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mViewPager.setCurrentItem(0);
            }
        });
        swipeListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    fabPlay.show(true);
                    fabTracks.show(true);

                } else {
                    fabPlay.hide(true);
                    fabTracks.hide(true);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
        mAdapter = new CustomListAdapter(getActivity(), queue);
        mAdapter.setSwipeListener(this);
        mAdapter.setSwipeDirection("left");
        swipeListView.setAdapter(mAdapter);
        Queue.QueueRefreshListener(this);
        if (MainActivity.landscape) {
            fabTracks.setVisibility(View.INVISIBLE);
        } else {
            fabTracks.setVisibility(View.VISIBLE);
        }
        return view;
    }

    public static void playerInitializeListener(IspotifyPlayerOptions listener) {
        mInitializePlayer = listener;
    }

    @Override
    public void refreshList() {
        mAdapter.notifyDataSetChanged();
    }

    private void PlayOnSpotify(int position) {
        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
        String product = sharedPref.getString(getString(R.string.product), "");
        if (product.equals("premium")) {
            if (PlayService.mPlayer == null || PlayService.mPlayer.isShutdown()) {
                Toast.makeText(context, "Initializing player", Toast.LENGTH_SHORT).show();
                mInitializePlayer.initializePlayer();
                mInitializePlayer.startNotification();
            }
            Queue.trackNumber = position;
            PlayService.addToQueue(Queue.getQueue(Queue.queue), Queue.getPosition(queue.get(position).getTrackURI()));
        } else {
            try {
                Intent spotify = new Intent(Intent.ACTION_VIEW, Uri.parse(queue.get(position).getTrackURI()));
                context.startActivity(spotify);
            } catch (Exception e) {
                Intent play = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.spotify.music"));
                context.startActivity(play);
            }
        }
    }

    @Override
    public void onSwipe(int position) {
        Log.d("swipe", "Remove " + queue.get(trackToRemove).getTrack() + " from queue");
        if (animate) {
            fabPlay.hide(true);
            fabTracks.hide(true);
        }
        animate = false;
        new SnackBar.Builder(getActivity())
                .withMessage("Removed: " + Queue.queue.get(trackToRemove).getTrack())
                .withVisibilityChangeListener(new SnackBar.OnVisibilityChangeListener() {
                    @Override
                    public void onShow(int i) {

                    }

                    @Override
                    public void onHide(int i) {
                        if (!animate) {
                            fabPlay.show(true);
                            fabTracks.show(true);
                            animate = true;
                        }
                    }
                })
                .withDuration(SnackBar.SHORT_SNACK)
                .show();
        FragmentTracks.insertTrack(queue.get(trackToRemove), queue.get(trackToRemove).getPosition());
        Queue.removeFromQueue(trackToRemove);
        Queue.queueChanged = true;
        refreshList();
        if (!Queue.queue.isEmpty()) {
            MainActivity.ENABLE_SEARCH = true;
            MainActivity.ENABLE_CLEAR = true;
        } else {
            MainActivity.ENABLE_SEARCH = false;
            MainActivity.ENABLE_CLEAR = false;
        }
        ((MainActivity) getActivity()).refreshActionBar();
    }

    @Override
    public void onDoubleClick(int position) {
        Log.d("swipe", String.format("onClickFrontView %d", position));
        PlayOnSpotify(position);
    }
}