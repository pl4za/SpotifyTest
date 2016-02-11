package com.pl4za.help;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.View;

import com.pl4za.spotifytest.FragmentQueue;
import com.pl4za.spotifytest.FragmentTracks;
import com.pl4za.spotifytest.MainActivity;
import com.pl4za.spotifytest.ViewCtrl;


public class PageAdapter extends FragmentPagerAdapter {

    boolean landscape;
    Fragment fragmentTracks, fragmentQueue;

    public PageAdapter(FragmentManager fm) {
        super(fm);
    }

    public void setOrientation(boolean landscape) {
        this.landscape = landscape;
    }

    @Override
    public Fragment getItem(int position) {
        if (fragmentTracks==null) {
            fragmentTracks = new FragmentTracks();
        } if (fragmentQueue==null) {
            fragmentQueue = new FragmentQueue();
        }
        if (position == 0) {
            return fragmentTracks;
        } else if (position == 1)
            return fragmentQueue;
        else {
            return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override public float getPageWidth(int position) {
        if (landscape) {
            return (0.5f);
        }
        else {
            return (1);
        }
    }
}
