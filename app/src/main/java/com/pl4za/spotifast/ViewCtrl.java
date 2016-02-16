package com.pl4za.spotifast;

import android.util.Log;

import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.FragmentOptions;
import com.pl4za.interfaces.ViewPagerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public class ViewCtrl {

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

    public void showSnackBar(String text) {
        activityOptions.showSnackBar(text);
    }

    public void addFragmentView(FragmentOptions f) {
        fragmentsOptions.add(f);
    }

    public void updateActionBar(int position) {
        activityOptions.updateActionBar(position);
    }

    public void updateView() {
        for (FragmentOptions f : fragmentsOptions) {
            f.updateView();
        }
    }

    public void updateView(int viewPosition) {
        for (FragmentOptions f : fragmentsOptions) {
            if (viewPosition==0 && (f.getClass().getName().equals("com.pl4za.spotifast.FragmentTracks"))) {
                f.updateView();
            } else if (viewPosition==1 && (f.getClass().getName().equals("com.pl4za.spotifast.FragmentQueue"))) {
                f.updateView();
            } else if (viewPosition==2 && (f.getClass().getName().equals("com.pl4za.spotifast.FragmentPlayer"))) {
                f.updateView();
            }
        }
    }

    public void setAdapter() {
        if (viewPagerInstance != null) {
            viewPagerInstance.setAdapter();
        }
    }

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

    public boolean isLandscape() {
        return activityOptions.isLandscape();
    }

    public void clearSearch() {
        activityOptions.clearSearch();
    }

    public void updateFilter(String query) {
        for (FragmentOptions f : fragmentsOptions) {
            f.updateFilter(query);
        }
    }

    public void setList(List<Track> list) {
        // Not implemented
    }

    public void onSwipe(int position) {
        //Not implemented
    }

    public void onDoubleClick(int position) {
        //Not implemented
    }

    public void loadTracks(String userID, String playlistID) {
        for (FragmentOptions f : fragmentsOptions) {
            f.loadTracks(userID, playlistID);
        }
    }
}
