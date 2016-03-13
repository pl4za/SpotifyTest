package com.pl4za.spotlight;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pl4za.help.CustomViewPager;
import com.pl4za.help.PageAdapter;
import com.pl4za.help.Params;
import com.pl4za.interfaces.ViewPagerOptions;
import com.pl4za.volley.AppController;

/**
 * Created by jasoncosta on 10/02/2016.
 */
public class FragmentMain extends Fragment implements ViewPagerOptions {

    private static CustomViewPager viewPager;
    private PageAdapter viewPagerAdapter;

    // Delegators
    private final ViewCtrl viewCtrl = ViewCtrl.getInstance();
    private final SettingsManager settings = SettingsManager.getInstance();
    private final TracklistCtrl tracklistCtrl = TracklistCtrl.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        viewPager = (CustomViewPager) view.findViewById(R.id.pager);
        viewPager.setOnPageChangeListener(new ViewPagerScroll());
        viewPagerAdapter = new PageAdapter(getChildFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPagerAdapter.setOrientation(viewCtrl.isLandscape());
        viewCtrl.setViewPagerOptions(this);
        //TODO: tracklistCtrl.clear();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        int lastPagerPosition = settings.getLastPagerPosition();
        viewCtrl.updateActionBar(lastPagerPosition);
        viewPager.setCurrentItem(lastPagerPosition);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewPager.removeAllViews();
    }

    @Override
    public void setViewPagerPosition(int position) {
        viewPager.setCurrentItem(position);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AppController.getInstance().cancelPendingRequests(Params.TAG_getSelectedPlaylistTracks);
        viewPagerAdapter.setOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
        setAdapter();
    }

    @Override
    public void setAdapter() {
        viewPager.setAdapter(viewPagerAdapter);
    }

    private class ViewPagerScroll implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            settings.setLastPagerPosition(position);
            viewCtrl.updateActionBar(position);
        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
}
