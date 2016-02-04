package com.pl4za.spotifytest;

import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.FragmentOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public class ViewCtrl implements ActivityOptions, FragmentOptions {

    private ArrayList<FragmentOptions> fragmentsOptions = new ArrayList<>();
    private ActivityOptions activityOptions;

    private static final ViewCtrl INSTANCE = new ViewCtrl();

    public static ViewCtrl getInstance() {
        return INSTANCE;
    }

    private ViewCtrl() {
    }

    public void setActivityView(ActivityOptions a) {
        this.activityOptions = a;
    }

    public void addFragmentView(FragmentOptions f) {
        fragmentsOptions.add(f);
    }

    @Override
    public void updateActionBar(int position) {
        activityOptions.updateActionBar(position);
    }

    @Override
    public void updateView() {
        for (FragmentOptions f : fragmentsOptions) {
            f.updateView();
        }
    }

    @Override
    public void updateFilter(String query) {
        for (FragmentOptions f : fragmentsOptions) {
            f.updateFilter(query);
        }
    }

    @Override
    public void setList(List<Track> list) {
        for (FragmentOptions f : fragmentsOptions) {
            f.setList(list);
        }
    }

    @Override
    public void onSwipe(int position) {
        //Not implemented
    }

    @Override
    public void onDoubleClick(int position) {
        //Not implemented
    }

    @Override
    public void loadTracks(String userID, String playlistID) {
        for (FragmentOptions f : fragmentsOptions) {
            f.loadTracks(userID, playlistID);
        }
    }
}
