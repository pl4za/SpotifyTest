package com.pl4za.spotifast;

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
            if (false) {
                f.updateView();
            } else if ((f.getClass().getName().equals("com.pl4za.spotifast.FragmentQueue"))) {
                f.updateView();
            } else if (false) {
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
        if (viewPagerInstance!=null) {
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
            f.loadTracks(userID, playlistID);
        }
    }
}
