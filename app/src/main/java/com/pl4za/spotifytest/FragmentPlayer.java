package com.pl4za.spotifytest;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.pl4za.interfaces.IplayerViewRefresh;
import com.pl4za.interfaces.IrefreshActionBar;
import com.pl4za.volley.AppController;

public class FragmentPlayer extends Fragment implements View.OnClickListener, IplayerViewRefresh {

    private static ImageLoader imageLoader = AppController.getInstance().getImageLoader();
    private View view;
    private ImageView ivNext;
    private ImageView ivPrevious;
    private ImageView ivPlayPause;
    private ImageView ivShuffle;
    private ImageView ivRepeat;
    private NetworkImageView ivAlbumArt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_player, container, false);
        IrefreshActionBar mRefreshActionBar = (IrefreshActionBar) getActivity();
        setButtonsListeners();
        if (Queue.TRACK_LIST != null && !Queue.TRACK_LIST.isEmpty()) {
            updateInfo();
        } else {
            ivPlayPause.setImageAlpha(30);
            ivAlbumArt.setImageAlpha(30);
            ivNext.setImageAlpha(30);
            ivPrevious.setImageAlpha(30);
            ivShuffle.setImageAlpha(30);
            ivRepeat.setImageAlpha(30);
            ivAlbumArt.setImageUrl(null, imageLoader);
            ivAlbumArt.setDefaultImageResId(R.drawable.no_image);
            ivAlbumArt.setErrorImageResId(R.drawable.no_image);
        }
        mRefreshActionBar.refreshActionBar(2);
        PlayService.playerRefreshListener(this);
        return view;
    }

    private void setButtonsListeners() {
        ivAlbumArt = (com.android.volley.toolbox.NetworkImageView) view.findViewById(R.id.albumArt);
        ivNext = (ImageView) view.findViewById(R.id.ivNext);
        ivPrevious = (ImageView) view.findViewById(R.id.ivPrevious);
        ivPlayPause = (ImageView) view.findViewById(R.id.ivPlayPause);
        ivShuffle = (ImageView) view.findViewById(R.id.ivShuffle);
        ivRepeat = (ImageView) view.findViewById(R.id.ivRepeat);
        //ImageView ivTracks = (ImageView) view.findViewById(R.id.ivTracks);
        //ImageView ivQueue = (ImageView) view.findViewById(R.id.ivQueue);
        ivAlbumArt.setOnClickListener(this);
        ivNext.setOnClickListener(this);
        ivPrevious.setOnClickListener(this);
        ivShuffle.setOnClickListener(this);
        ivRepeat.setOnClickListener(this);
        //ivQueue.setOnClickListener(this);
        //ivTracks.setOnClickListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new FragmentPlayer(), "FragmentPlayer")
                .commit();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.albumArt) {
            //Log.i("Player", "PlayPause Event");
            if (PlayService.PLAYING) {
                ivPlayPause.setImageResource(R.drawable.play_selector);
            } else {
                ivPlayPause.setImageResource(R.drawable.pause_selector);
            }
            PlayService.resumePause();
        } else if (v.getId() == R.id.ivNext) {
            //Log.i("Player", "NextTrack Event");
            PlayService.nextTrack();
        } else if (v.getId() == R.id.ivPrevious) {
            //Log.i("Player", "PreviousTrack Event");
            PlayService.prevTrack();
        } else if (v.getId() == R.id.ivShuffle) {
            //Log.i("Player", "Shuffle Event");
            PlayService.shuffle();
            checkOptions();
        } else if (v.getId() == R.id.ivRepeat) {
            //Log.i("Player", "Repeat Event");
            PlayService.repeat();
            checkOptions();
        }
    }

    @Override
    public void updateInfo() {
        //Log.i("FragmentPlayer", "Updating info: " + Queue.getPlayingTrack().getTrack());
        TextView tvTrackTitle = (TextView) view.findViewById(R.id.tvTrackTitle);
        TextView tvAlbum = (TextView) view.findViewById(R.id.tvAlbum);
        TextView tvArtist = (TextView) view.findViewById(R.id.tvArtist);
        tvTrackTitle.setText(Queue.playingTrack.getTrack());
        tvAlbum.setText(Queue.playingTrack.getTrack());
        tvArtist.setText(Queue.playingTrack.getSimpleArtist());
        checkOptions();
        if (Queue.TRACK_LIST.isEmpty()) {
            ivPlayPause.setImageResource(R.drawable.play_selector);
            ivPlayPause.setImageAlpha(30);
            ivNext.setEnabled(false);
            ivPrevious.setEnabled(false);
        } else {
            ivPlayPause.setImageAlpha(255);
            if ((Queue.trackNumber + 1 == Queue.TRACK_LIST.size()) && !PlayService.isShuffled()) {
                ivNext.setImageAlpha(30);
            } else {
                ivNext.setImageAlpha(255);
            }
            if (Queue.trackNumber == 0 && !PlayService.isShuffled()) {
                ivPrevious.setImageAlpha(30);
            } else {
                ivPrevious.setImageAlpha(255);
            }
        }
        if (PlayService.PLAYING) {
            ivPlayPause.setImageResource(R.drawable.pause_selector);
        } else {
            ivPlayPause.setImageResource(R.drawable.play_selector);
        }
        insertImage();
    }

    private void checkOptions() {
        if (PlayService.isShuffled()) {
            ivShuffle.setImageAlpha(255);
        } else {
            ivShuffle.setImageAlpha(30);
        }
        if (PlayService.isRepeating()) {
            ivRepeat.setImageAlpha(255);
        } else {
            ivRepeat.setImageAlpha(30);
        }
    }

    private void insertImage() {
        if (imageLoader == null)
            imageLoader = AppController.getInstance().getImageLoader();
        NetworkImageView albumArt = (NetworkImageView) view.findViewById(R.id.albumArt);
        albumArt.setImageUrl(Queue.playingTrack.getBigAlbumArt(), imageLoader);
        imageLoader.get(Queue.playingTrack.getBigAlbumArt(), ImageLoader.getImageListener(albumArt, R.drawable.no_image, R.drawable.no_image));
    }

}