package com.pl4za.help;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.pl4za.spotifytest.FragmentQueue;
import com.pl4za.spotifytest.FragmentTracks;
import com.pl4za.spotifytest.MainActivity;


public class PageAdapter extends FragmentPagerAdapter {

    public PageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        //Log.i("Adapter", "Position: " + position);
        if (position == 0) {
            return new FragmentTracks();
        } else if (position == 1)
            return new FragmentQueue();
        /*
        else if (position == 2)
            return new FragmentPlayer();
            */
        else {
            return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override public float getPageWidth(int position) {
        if (MainActivity.landscape) {
            return (0.5f);
        }
        else {
            return (1);
        }
    }
}
