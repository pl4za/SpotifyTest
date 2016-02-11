package com.pl4za.spotifytest;

import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.FragmentOptions;
import com.pl4za.interfaces.ViewPagerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public class ViewCtrl implements ActivityOptions, FragmentOptions, ViewPagerOptions {

    private ArrayList<FragmentOptions> fragmentsOptions = new ArrayList<>();
    private ActivityOptions activityOptions;
    private ViewPagerOptions viewPagerInstance;

    private static final ViewCtrl INSTANCE = new ViewCtrl();

    public static ViewCtrl getInstance() {
        return INSTANCE;
    }

    private ViewCtrl() {
    }

    public void setActivityView(ActivityOptions a) {
        this.activityOptions = a;
    }

    public void setViewPagerOptions(ViewPagerOptions v) {
        this.viewPagerInstance = v;
    }

    @Override
    public void showSnackBar(String text) {
        activityOptions.showSnackBar(text);
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
    public void setAdapter() {
        if (viewPagerInstance != null) {
            viewPagerInstance.setAdapter();
        }
    }

    @Override
    public void hideFab(boolean hide) {
        for (FragmentOptions f : fragmentsOptions) {
            f.hideFab(hide);
        }
    }

    public void setViewPagerPosition(int position) {
        if (viewPagerInstance!=null) {
            viewPagerInstance.setViewPagerPosition(position);
        }
    }

    @Override
    public boolean isLandscape() {
        return activityOptions.isLandscape();
    }

    @Override
    public void clearSearch() {
        activityOptions.clearSearch();
    }

    @Override
    public void updateFilter(String query) {
        for (FragmentOptions f : fragmentsOptions) {
            f.updateFilter(query);
        }
    }

    @Override
    public void setList(List<Track> list) {
        // Not implemented
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
