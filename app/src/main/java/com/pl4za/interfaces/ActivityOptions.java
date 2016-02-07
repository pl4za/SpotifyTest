package com.pl4za.interfaces;

/**
 * Created by jasoncosta on 2/2/2016.
 */
public interface ActivityOptions {

    void updateActionBar(int position);

    void showSnackBar(String text);

    void setViewPagerPosition(int position);

    boolean isLandscape();

}
