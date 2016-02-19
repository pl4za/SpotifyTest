package com.pl4za.help;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.pl4za.spotlight.FragmentQueue;
import com.pl4za.spotlight.FragmentTracks;


public class PageAdapter extends FragmentPagerAdapter {

    private boolean landscape;
    private Fragment fragmentTracks;
    private Fragment fragmentQueue;

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
