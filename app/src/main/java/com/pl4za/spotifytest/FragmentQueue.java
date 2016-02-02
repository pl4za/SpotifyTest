package com.pl4za.spotifytest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.github.mrengineer13.snackbar.SnackBar;
import com.melnykov.fab.FloatingActionButton;
import com.pl4za.help.CustomListAdapter;
import com.pl4za.interfaces.ISwipeListener;
import com.pl4za.interfaces.IqueueRefresh;

import java.util.List;

public class FragmentQueue extends Fragment implements IqueueRefresh, ISwipeListener {

    public static CustomListAdapter mAdapter;
    private static ListView swipeListView;
    private static Context context;
    private static boolean animate = true;
    private static FloatingActionButton fabPlay;
    private static FloatingActionButton fabTracks;
    private static List<Track> filteredList;

    public static void setFilteredList(List<Track> results) {
        filteredList = results;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_queue, container, false);
        swipeListView = (ListView) view.findViewById(R.id.listview);
        fabPlay = (FloatingActionButton) view.findViewById(R.id.fabPlay);
        fabTracks = (FloatingActionButton) view.findViewById(R.id.fabTracks);
        // Custom class listener for FAB
        FabClickListener fcl = new FabClickListener();
        fabPlay.setOnClickListener(fcl);
        fabTracks.setOnClickListener(fcl);
        swipeListView.setOnScrollListener(new ListViewScrollListener());
        context = getActivity();
        filteredList = Queue.TRACK_LIST;
        mAdapter = new CustomListAdapter(getActivity(), filteredList);
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

    @Override
    public void refreshList() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSwipe(int position) {
        if (animate) {
            fabPlay.hide(true);
            fabTracks.hide(true);
        }
        animate = false;
        new SnackBar.Builder(getActivity())
                .withMessage("Removed: " + Queue.TRACK_LIST.get(position).getTrack())
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
        FragmentTracks.insertTrack(Queue.TRACK_LIST.get(position), position);
        Queue.removeFromQueue(position);
        mAdapter.notifyDataSetChanged();
        ((MainActivity) getActivity()).refreshActionBar(1);
    }

    @Override
    public void onDoubleClick(int position) {
        playTrack(position);
    }

    private void playTrack(int position) {
        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
        String product = sharedPref.getString(getString(R.string.product), "");
        if (product.equals("premium")) {
            PlayService.addToQueue(Queue.getQueueURIList(filteredList), position);
        } else {
            try {
                Intent spotify = new Intent(Intent.ACTION_VIEW, Uri.parse(filteredList.get(position).getTrackURI()));
                context.startActivity(spotify);
            } catch (Exception e) {
                Intent play = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.spotify.music"));
                context.startActivity(play);
            }
        }
    }

    private class FabClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.fabTracks) {
                MainActivity.mViewPager.setCurrentItem(0);
            } else if (v.getId() == R.id.fabPlay) {
                FragmentPlayer playFrag = new FragmentPlayer();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, playFrag, "FragmentPlayer")
                        .addToBackStack(null)
                        .commit();
                ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                MainActivity.isHomeAsUpEnabled = true;
            }
        }
    }

    private class ListViewScrollListener implements AbsListView.OnScrollListener {

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
    }

}