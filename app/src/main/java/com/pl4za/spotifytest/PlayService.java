package com.pl4za.spotifytest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.pl4za.interfaces.SpotifyPlayerOptions;
import com.pl4za.interfaces.ServiceOptions;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.PlayConfig;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import java.util.List;

/**
 * Created by Admin on 16/02/2015.
 */
public class PlayService extends Service implements PlayerNotificationCallback, ConnectionStateCallback, SpotifyPlayerOptions, ServiceOptions {

    private static final String TAG = "PlayService";
    public static String playlistName = "Spotlite";
    public static NotificationManager mNotificationManager;
    public static boolean notificationActive = false;
    public static Player mPlayer;
    public static boolean SKIP_NEXT = true;
    public static boolean TRACK_END = true;
    public static boolean PLAYING = false;
    private static boolean SHUFFLE = false;
    private static boolean REPEAT = false;

    private final IBinder mBinder = new LocalBinder();
    private RemoteViews contentView;
    private Notification note;
    private SwitchButtonListener switchButtonListener;
    private String access_token;

    // interfaces
    private QueueCtrl queueCtrl = QueueCtrl.getInstance();
    private ViewCtrl viewCtrl = ViewCtrl.getInstance();

    @Override
    public void addToQueue(String trackUri) {
        mPlayer.queue(trackUri);
    }

    @Override
    public void addToQueue(List<String> queue, int listStart) {
        PlayService.clearQueue();
        if (queue.size() == 1) {
            mPlayer.play(queue);
        } else {
            //Log.i("PlayService)", "Adding to TRACK_LIST: " + TRACK_LIST.get(0) + " - " + index);
            mPlayer.play(PlayConfig.createFor(queue).withTrackIndex(listStart));
        }
        TRACK_END = false;
        SKIP_NEXT = false;
    }

    @Override
    public boolean isActive() {
        return mPlayer.isLoggedIn();
    }

    public static boolean isShuffled() {
        return SHUFFLE;
    }

    public static boolean isRepeating() {
        return REPEAT;
    }

    public static void clearQueue() {
        if (mPlayer.isInitialized()) {
            mPlayer.pause();
            mPlayer.clearQueue();
        }
    }

    public void resumePause() {
        if (mPlayer != null) {
            if (PLAYING) {
                mPlayer.pause();
                PLAYING = false;
            } else {
                mPlayer.resume();
                PLAYING = true;
            }
        }
    }

    public void nextTrack() {
            mPlayer.skipToNext();
    }

    public void prevTrack() {
            mPlayer.skipToPrevious();
    }

    public void shuffle() {
        if (mPlayer != null) {
            if (SHUFFLE) {
                mPlayer.setShuffle(false);
                SHUFFLE = false;
            } else {
                mPlayer.setShuffle(true);
                SHUFFLE = true;
            }
        }
    }

    public void repeat() {
        if (mPlayer != null) {
            if (REPEAT) {
                mPlayer.setRepeat(false);
                REPEAT = false;
            } else {
                mPlayer.setRepeat(true);
                REPEAT = true;
            }
        }
    }

    @Override
    public void onDestroy() {
        if (switchButtonListener != null)
            unregisterReceiver(switchButtonListener);
        destroyPlayer();
        Log.i("PlayService", "Service destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("PlayService", "Service started..");
        if (intent != null && (mPlayer == null || mPlayer.isShutdown())) {
            Bundle b = intent.getExtras();
            access_token = b.getString("acess_token");
            Log.i("PlayService", access_token);
            initializePlayer();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("PlayService", "Service bind..");
        return mBinder;
    }

    @Override
    public void onConnectionMessage(String arg0) {
    }

    @Override
    public void onLoggedIn() {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
        String user_id = sharedPref.getString(getString(R.string.user_id), "");
        Log.i("PlayService", "Logged in as " + user_id);
    }

    @Override
    public void onLoggedOut() {
    }

    @Override
    public void onLoginFailed(Throwable arg0) {
        Log.e("PlayService", arg0.getMessage());
        Toast.makeText(getApplicationContext(), arg0.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTemporaryError() {
    }

    @Override
    public void onPlaybackEvent(EventType arg0, PlayerState arg1) {
        String msg = arg0.toString();
        Log.i("PlayService", "Evento: " + arg0 + " playing: " + PLAYING);
        if (msg.equals("PLAY") || msg.equals("PAUSE")) {
            if (note != null) {
                updateNotificationButtons();
            }
        }
        if (msg.equals("TRACK_END")) {
            PLAYING = false;
            if (queueCtrl.isQueueChanged() && queueCtrl.hasNext()) { //Queue.TRACK_LIST.size() > 1
                Log.i("PlayService", "Queue changed: clearing and re-ading TRACK_LIST in position " + queueCtrl.getQueuePosition());
                mPlayer.clearQueue();
                addToQueue(queueCtrl.getQueueURIList(queueCtrl.getQueue()), queueCtrl.getQueuePosition());
                queueCtrl.setQueueChanged(false);
            }
            TRACK_END = true;
        } else if (msg.equals("TRACK_END") && TRACK_END) {
            Log.i("PlayService", "SKIPPING TO NEXT AUTOMATICALLY");
            TRACK_END = true;
            SKIP_NEXT = false;
            mPlayer.skipToNext();
        } else {
            if ((msg.equals("TRACK_START") && SHUFFLE)) {
                Log.i("PlayService", "SKIPPING TO NEXT SHUFFLE");
            } else if (msg.equals("SKIP_NEXT") && SKIP_NEXT) {
                Log.i("PlayService", "SKIPPING TO NEXT VIA PRESS");
                TRACK_END = false;
                SKIP_NEXT = false;
            } else if (msg.equals("SKIP_PREV")) {
                Log.i("PlayService", "SKIPPING TO PREVIOUS");
                TRACK_END = false;
            }
        }
        if (msg.equals("TRACK_START") && queueCtrl.hasTracks()) {
            PLAYING = true;
            TRACK_END = true;
            SKIP_NEXT = true;
            queueCtrl.updateTrackNumberAndPlayingTrack(arg1.trackUri);
            if (mNotificationManager == null || note == null) {
                startNotification();
            }
            updateNotificationInfo();
            viewCtrl.updateView();
        }
    }

    @Override
    public void onPlaybackError(PlayerNotificationCallback.ErrorType arg0, String arg1) {
        Log.e("Playback", arg1 + " " + arg0.toString());
        //Toast.makeText(getApplicationContext(), arg0.toString(), Toast.LENGTH_SHORT).show();
        if (arg0.toString().equals("TRACK_UNAVAILABLE")) {
            Toast.makeText(getApplicationContext(), "Track unavailable", Toast.LENGTH_SHORT).show();
            nextTrack();
        } else if (arg0.toString().equals("ERROR_PLAYBACK")) {
            Toast.makeText(getApplicationContext(), "Playback error", Toast.LENGTH_SHORT).show();
            nextTrack();
        }
    }

    @Override
    public void initializePlayer() {
        if ((mPlayer == null || mPlayer.isShutdown())) {
            SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.SpotifyPreferences), Context.MODE_PRIVATE);
            String product = sharedPref.getString("product", "Please login");
            if (product.equals("premium")) {
                Log.i("PlayService", "Initializing player");
                Config playerConfig = new Config(this, access_token, CLIENT_ID);
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(PlayService.this);
                        mPlayer.addPlayerNotificationCallback(PlayService.this);
                        Log.i("PlayService", "Player initialized");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("PlayService", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "Spotify premium is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void destroyPlayer() {
        Spotify.destroyPlayer(PlayService.mPlayer);
        if (notificationActive) {
            mNotificationManager.cancel(1);
        }
    }

    private void updateNotificationInfo() {
        Log.i("PlayService", "Updating notification");
        contentView.setImageViewResource(R.id.image, R.drawable.no_image);
        contentView.setTextViewText(R.id.tvTrackTitle, queueCtrl.getCurrentTrack().getTrack());
        contentView.setTextViewText(R.id.tvArtistAndAlbum, queueCtrl.getCurrentTrack().getSimpleArtist() + " - " + queueCtrl.getCurrentTrack().getAlbum());
        mNotificationManager.notify(1, note);
    }

    private void updateNotificationButtons() {
        Log.i("PlayService", "Updating notification");
        if (!PLAYING) {
            contentView.setImageViewResource(R.id.ivPlayPause_2, R.drawable.play_selector);
        } else {
            contentView.setImageViewResource(R.id.ivPlayPause_2, R.drawable.pause_selector);
        }
        mNotificationManager.notify(1, note);
    }

    @Override
    public void startNotification() {
        if (mNotificationManager == null || note == null) {
            switchButtonListener = new SwitchButtonListener();
            IntentFilter iFilter = new IntentFilter(actionPlayPause);
            iFilter.addAction(actionNext);
            iFilter.addAction(actionDismiss);
            registerReceiver(switchButtonListener, iFilter);

            Intent intentAction = new Intent(this, MainActivity.class);
            PendingIntent pendingIntentAction = PendingIntent.getActivity(this, 0, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);

            contentView = new RemoteViews(getPackageName(), R.layout.playing_notification);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_action_play_over_video)
                            .setContentIntent(pendingIntentAction)
                                    //.setDeleteIntent(pendingIntentDismiss)
                            .setContent(contentView);
            note = mBuilder.build();
            note.flags = Notification.FLAG_ONGOING_EVENT;

            //Play
            Intent intentPlayPause = new Intent(actionPlayPause);
            PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(this, 0, intentPlayPause, 0);
            contentView.setOnClickPendingIntent(R.id.ivPlayPause_2, pendingIntentPlayPause);
            //Next
            Intent intentNext = new Intent(actionNext);
            PendingIntent pendingIntentNext = PendingIntent.getBroadcast(this, 0, intentNext, 0);
            contentView.setOnClickPendingIntent(R.id.ivNext, pendingIntentNext);
            //Next
            Intent intentClose = new Intent(actionDismiss);
            PendingIntent pendingIntentClose = PendingIntent.getBroadcast(this, 0, intentClose, 0);
            contentView.setOnClickPendingIntent(R.id.ivClose, pendingIntentClose);
            //
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1, note);
            notificationActive = true;
        }
    }

    public class LocalBinder extends Binder {
        PlayService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayService.this;
        }
    }

    public class SwitchButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("PlayService", intent.getAction());
            if (queueCtrl.hasTracks()) {
                if (intent.getAction().equals(actionPlayPause)) {
                    resumePause();
                } else if (intent.getAction().equals(actionNext)) {
                    nextTrack();
                }
            }
            if (intent.getAction().equals(actionDismiss)) {
                destroyPlayer();
                stopSelf();
                mNotificationManager.cancel(1);
            }
        }

    }
}
