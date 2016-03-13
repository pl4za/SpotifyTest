package com.pl4za.spotlight;

import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.FragmentOptions;
import com.pl4za.interfaces.ViewPagerOptions;

import java.util.ArrayList;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public class ViewCtrl {

    private final ArrayList<FragmentOptions> fragmentsOptions = new ArrayList<>();
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
        if (activityOptions != null) {
            activityOptions.showSnackBar(text);
        }
    }

    public void addFragmentView(FragmentOptions f) {
        String fragmentClass = f.getClass().getName();
        ArrayList<Integer> removeArr = new ArrayList<>();
        for (int i = 0; i < fragmentsOptions.size(); i++) {
            if (fragmentsOptions.get(i).getClass().getName().equals(fragmentClass)) {
                removeArr.add(i);
            }
        }
        for (int toRemove : removeArr) {
            fragmentsOptions.remove(toRemove);
        }
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
            if (viewPosition == 0 && (f.getClass().getName().equals("com.pl4za.spotlight.FragmentTracks"))) {
                f.updateView();
            } else if (viewPosition == 1 && (f.getClass().getName().equals("com.pl4za.spotlight.FragmentQueue"))) {
                f.updateView();
            } else if (viewPosition == 2 && (f.getClass().getName().equals("com.pl4za.spotlight.FragmentPlayer") || (f.getClass().getName().equals("com.pl4za.spotlight.FragmentQueue")))) {
                f.updateView();
            }
        }
    }

    public void hideFab(boolean hide) {
        for (FragmentOptions f : fragmentsOptions) {
            f.hideFab(hide);
        }
    }

    public void setViewPagerPosition(int position) {
        if (viewPagerInstance != null) {
            viewPagerInstance.setViewPagerPosition(position);
        }
    }

    public boolean isLandscape() {
        return activityOptions.isLandscape();
    }

    public void updateFilter(String query) {
        for (FragmentOptions f : fragmentsOptions) {
            f.updateFilter(query);
        }
    }

    public void loadTracks(String userID, String playlistID) {
        for (FragmentOptions f : fragmentsOptions) {
            if (playlistID.equals("stared")) {
                f.loadTracks("https://api.spotify.com/v1/me/tracks");
            } else {
                f.loadTracks("https://api.spotify.com/v1/users/" + userID + "/playlists/" + playlistID + "/tracks");
            }
        }
    }
}
