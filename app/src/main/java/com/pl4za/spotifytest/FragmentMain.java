package com.pl4za.spotifytest;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pl4za.help.MyViewPager;
import com.pl4za.help.PageAdapter;
import com.pl4za.interfaces.ViewPagerOptions;

/**
 * Created by jasoncosta on 10/02/2016.
 */
public class FragmentMain extends Fragment implements ViewPagerOptions {

    private static MyViewPager viewPager;
    private PageAdapter viewPagerAdapter;

    // Delegators
    private ViewCtrl viewCtrl = ViewCtrl.getInstance();
    private SettingsManager settings = SettingsManager.getInstance();
    private TracklistCtrl tracklistCtrl = TracklistCtrl.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        viewPager = (MyViewPager) view.findViewById(R.id.pager);
        viewPager.setOnPageChangeListener(new ViewPagerScroll());
        viewPagerAdapter = new PageAdapter(getActivity().getSupportFragmentManager());
        viewPagerAdapter.setViewCtrl(viewCtrl);
        viewCtrl.setViewPagerOptions(this);
        viewPager.setAdapter(viewPagerAdapter);
        tracklistCtrl.clear();
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
    public void setViewPagerPosition(int position) {
        viewPager.setCurrentItem(position);
    }

    @Override
    public void updateView() {
        viewPager.setAdapter(viewPagerAdapter);
    }

    class ViewPagerScroll implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            settings.setLastPagerPosition(position);
            viewCtrl.clearSearch();
        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
}
