package com.pl4za.spotifytest;

import com.pl4za.interfaces.ActivityOptions;
import com.pl4za.interfaces.FragmentOptions;

import java.util.ArrayList;

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
    public void updateActionBar(boolean search, boolean clear) {
        activityOptions.updateActionBar(search, clear);
    }

    @Override
    public void updateView() {
        for (FragmentOptions f : fragmentsOptions) {
            f.updateView();
        }
    }

    @Override
    public void onSwipe(int position) {

    }

    @Override
    public void onDoubleClick(int position) {

    }
}
