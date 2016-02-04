package com.pl4za.spotifytest;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mrengineer13.snackbar.SnackBar;
import com.melnykov.fab.FloatingActionButton;
import com.pl4za.help.CustomListAdapter;
import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.FragmentOptions;

import java.util.List;

public class FragmentQueue extends Fragment implements FragmentOptions {
    private static final String TAG = "FragmentQueue";
    private static final int SCROLL_STATE_IDLE = 0;
    public static CustomListAdapter mAdapter;
    private static RecyclerView recyclerView;
    private static boolean animate = true;
    private static FloatingActionButton fabPlay;
    private static FloatingActionButton fabTracks;
    private static List<Track> filteredList;
    // interfaces
    private QueueCtrl queueCtrl = QueueCtrl.getInstance();
    private ViewCtrl viewCtrl = ViewCtrl.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_queue, container, false);
        viewCtrl.setActivityView((ActivityOptions) getActivity());
        viewCtrl.addFragmentView(this);
        recyclerView = (RecyclerView) view.findViewById(R.id.listview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        fabPlay = (FloatingActionButton) view.findViewById(R.id.fabPlay);
        fabTracks = (FloatingActionButton) view.findViewById(R.id.fabTracks);
        // Custom class listener for FAB
        FabClickListener fcl = new FabClickListener();
        fabPlay.setOnClickListener(fcl);
        fabTracks.setOnClickListener(fcl);
        recyclerView.addOnScrollListener(new ListViewScrollListener());
        mAdapter = new CustomListAdapter(queueCtrl.getTrackList());
        mAdapter.setSwipeListener(this);
        mAdapter.setSwipeDirection("left");
        recyclerView.setAdapter(mAdapter);
        if (MainActivity.landscape) {
            fabTracks.setVisibility(View.INVISIBLE);
        } else {
            fabTracks.setVisibility(View.VISIBLE);
        }
        return view;
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
        filteredList = list;
    }

    @Override
    public void onSwipe(int position) {
        //TODO: Play service not synced with queue?
        if (animate) {
            fabPlay.hide(true);
            fabTracks.hide(true);
        }
        animate = false;
        new SnackBar.Builder(getActivity())
                .withMessage("Removed: " + queueCtrl.getCurrentTrack().getTrack())
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
        queueCtrl.removeFromList(position);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDoubleClick(int position) {
        queueCtrl.addTrackList(queueCtrl.getTrackList(), position);
    }

    @Override
    public void loadTracks(String userID, String playlistID) {
        //Not implemented
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

    private class ListViewScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == SCROLL_STATE_IDLE) {
                fabPlay.show(true);
                fabTracks.show(true);

            } else {
                fabPlay.hide(true);
                fabTracks.hide(true);
            }
        }
    }

}