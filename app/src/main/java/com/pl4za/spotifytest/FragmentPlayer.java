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
import com.pl4za.interfaces.FragmentOptions;
import com.pl4za.volley.AppController;

import java.util.List;

public class FragmentPlayer extends Fragment implements View.OnClickListener, FragmentOptions {

    private static ImageLoader imageLoader = AppController.getInstance().getImageLoader();
    private View view;
    private ImageView ivNext;
    private ImageView ivPrevious;
    private ImageView ivPlayPause;
    private ImageView ivShuffle;
    private ImageView ivRepeat;
    private NetworkImageView ivAlbumArt;

    // Interfaces
    private PlayCtrl playCtrl = PlayCtrl.getInstance();
    private ViewCtrl viewCtrl = ViewCtrl.getInstance();
    private QueueCtrl queueCtrl = QueueCtrl.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewCtrl.addFragmentView(this);
        view = inflater.inflate(R.layout.fragment_player, container, false);
        viewCtrl.updateActionBar(false, false);
        setButtonsListeners();
        if (queueCtrl.hasTracks()) {
            updateView();
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
            playCtrl.resumePause();
        } else if (v.getId() == R.id.ivNext) {
            //Log.i("Player", "NextTrack Event");
            playCtrl.nextTrack();
        } else if (v.getId() == R.id.ivPrevious) {
            //Log.i("Player", "PreviousTrack Event");
            playCtrl.prevTrack();
        } else if (v.getId() == R.id.ivShuffle) {
            //Log.i("Player", "Shuffle Event");
            playCtrl.shuffle();
            checkOptions();
        } else if (v.getId() == R.id.ivRepeat) {
            //Log.i("Player", "Repeat Event");
            playCtrl.repeat();
            checkOptions();
        }
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
        albumArt.setImageUrl(queueCtrl.getCurrentTrack().getBigAlbumArt(), imageLoader);
        imageLoader.get(queueCtrl.getCurrentTrack().getBigAlbumArt(), ImageLoader.getImageListener(albumArt, R.drawable.no_image, R.drawable.no_image));
    }

    @Override
    public void updateView() {
        //Log.i("FragmentPlayer", "Updating info: " + Queue.getPlayingTrack().getTrack());
        TextView tvTrackTitle = (TextView) view.findViewById(R.id.tvTrackTitle);
        TextView tvAlbum = (TextView) view.findViewById(R.id.tvAlbum);
        TextView tvArtist = (TextView) view.findViewById(R.id.tvArtist);
        tvTrackTitle.setText(queueCtrl.getCurrentTrack().getTrack());
        tvAlbum.setText(queueCtrl.getCurrentTrack().getTrack());
        tvArtist.setText(queueCtrl.getCurrentTrack().getSimpleArtist());
        checkOptions();
        if (!queueCtrl.hasTracks()) {
            ivPlayPause.setImageResource(R.drawable.play_selector);
            ivPlayPause.setImageAlpha(30);
            ivNext.setEnabled(false);
            ivPrevious.setEnabled(false);
        } else {
            ivPlayPause.setImageAlpha(255);
            if ((!queueCtrl.hasNext()) && !PlayService.isShuffled()) {
                ivNext.setImageAlpha(30);
            } else {
                ivNext.setImageAlpha(255);
            }
            if (!queueCtrl.hasPrevious() && !PlayService.isShuffled()) {
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

    @Override
    public void updateFilter(String query) {
        // Not implemented
    }

    @Override
    public void setList(List<Track> list) {
        // Not implemented
    }

    @Override
    public void onSwipe(int position) {
        // Not implemented
    }

    @Override
    public void onDoubleClick(int position) {
        // Not implemented
    }

    @Override
    public void loadTracks(String userID, String playlistID) {
        // Not implemented
    }
}