package com.pl4za.spotifytest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.github.mrengineer13.snackbar.SnackBar;
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
public class PlayService extends Service implements PlayerNotificationCallback, ConnectionStateCallback, ServiceOptions {

    private static final String TAG = "PlayService";
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
    private Notification notification;
    private SwitchButtonListener switchButtonListener;

    // interfaces
    private QueueCtrl queueCtrl = QueueCtrl.getInstance();
    private ViewCtrl viewCtrl = ViewCtrl.getInstance();
    private SettingsManager settings = SettingsManager.getInstance();

    @Override
    public void addToQueue(String trackUri) {
        mPlayer.queue(trackUri);
    }

    @Override
    public void addToQueue(List<String> queue, int listStart) {
        clearQueue();
        if (queue.size() == 1) {
            mPlayer.play(queue);
        } else {
            mPlayer.play(PlayConfig.createFor(queue).withTrackIndex(listStart));
        }
        TRACK_END = false;
        SKIP_NEXT = false;
    }

    @Override
    public boolean isActive() {
        return mPlayer.isLoggedIn();
    }

    @Override
    public void onDestroy() {
        if (switchButtonListener != null)
            unregisterReceiver(switchButtonListener);
        destroyPlayer();
        Log.i(TAG, "Service destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started..");
        initializePlayer();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service bind..");
        return mBinder;
    }

    @Override
    public void onConnectionMessage(String arg0) {
        Log.i(TAG, arg0);
    }

    @Override
    public void onLoggedIn() {
        Log.i(TAG, "Logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.i(TAG, "Logged out!");
    }

    @Override
    public void onLoginFailed(Throwable arg0) {
        Log.e(TAG, arg0.getMessage());
        if (arg0.getMessage().equals("Temporary connection error occurred")) {
            viewCtrl.showSnackBar("No connection");
        }
        Toast.makeText(getApplicationContext(), arg0.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTemporaryError() {
        Log.e(TAG, "Something ocurred!");
    }

    @Override
    public void onPlaybackEvent(EventType arg0, PlayerState arg1) {
        String msg = arg0.toString();
        Log.i(TAG, "Evento: " + arg0 + " playing: " + PLAYING);
        if (msg.equals("PLAY") || msg.equals("PAUSE")) {
            PLAYING=true;
            if (notification != null) {
                updateNotificationButtons();
            }
        }
        if (msg.equals("TRACK_END")) {
            PLAYING = false;
            if (queueCtrl.isQueueChanged() && queueCtrl.hasNext()) {
                Log.i(TAG, "Queue changed: clearing and re-ading TRACK_LIST");
                mPlayer.clearQueue();
                addToQueue(queueCtrl.getTrackURIList(queueCtrl.getTrackList()), queueCtrl.getQueuePosition() + queueCtrl.getTrackNumberUpdate());
                queueCtrl.setQueueChanged(false);
            }
            TRACK_END = true;
            viewCtrl.updateView();

        } else if (msg.equals("TRACK_END") && TRACK_END) {
            Log.i(TAG, "SKIPPING TO NEXT AUTOMATICALLY");
            TRACK_END = true;
            SKIP_NEXT = false;
            mPlayer.skipToNext();
        } else {
            if ((msg.equals("TRACK_START") && SHUFFLE)) {
                Log.i(TAG, "SKIPPING TO NEXT SHUFFLE");
            } else if (msg.equals("SKIP_NEXT") && SKIP_NEXT) {
                Log.i(TAG, "SKIPPING TO NEXT VIA PRESS");
                TRACK_END = false;
                SKIP_NEXT = false;
            } else if (msg.equals("SKIP_PREV")) {
                Log.i(TAG, "SKIPPING TO PREVIOUS");
                TRACK_END = false;
            }
        }
        if (msg.equals("TRACK_START") && queueCtrl.hasTracks()) {
            PLAYING = true;
            TRACK_END = true;
            SKIP_NEXT = true;
            queueCtrl.updateTrackNumberAndPlayingTrack(arg1.trackUri);
            if (mNotificationManager == null || notification == null) {
                startNotification();
            }
            updateNotificationInfo();
            viewCtrl.updateView();

        }
    }

    @Override
    public void onPlaybackError(PlayerNotificationCallback.ErrorType arg0, String arg1) {
        Log.e("Playback", arg1 + " " + arg0.toString());
        if (arg0.toString().equals("TRACK_UNAVAILABLE")) {
            Toast.makeText(getApplicationContext(), "Track unavailable", Toast.LENGTH_SHORT).show();
            //nextTrack();
        } else if (arg0.toString().equals("ERROR_PLAYBACK")) {
            Toast.makeText(getApplicationContext(), "Playback error", Toast.LENGTH_SHORT).show();
            //nextTrack();
        }
    }

    public void startNotification() {
        if (mNotificationManager == null || notification == null) {
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
            notification = mBuilder.build();
            notification.flags = Notification.FLAG_ONGOING_EVENT;

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
            mNotificationManager.notify(1, notification);
            notificationActive = true;
        }
    }

    public void initializePlayer() {
        if ((mPlayer == null || mPlayer.isShutdown() || !mPlayer.isLoggedIn())) {
            if (settings.getProduct().equals("premium")) {
                Log.i(TAG, "Initializing player");
                Config playerConfig = new Config(this, settings.getAccessToken(), CLIENT_ID);
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(PlayService.this);
                        mPlayer.addPlayerNotificationCallback(PlayService.this);
                        Log.i(TAG, "Player initialized");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, "Could not initialize player: " + throwable.getMessage());
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "Spotify premium is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static boolean isShuffled() {
        return SHUFFLE;
    }

    public boolean isPlaying() {
        return PLAYING;
    }

    public static boolean isRepeating() {
        return REPEAT;
    }

    public void clearQueue() {
        if (mPlayer.isInitialized()) {
            mPlayer.pause();
            mPlayer.clearQueue();
        }
        PLAYING = false;
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
        if (queueCtrl.hasNext()) {
            mPlayer.skipToNext();
        }
    }

    public void prevTrack() {
        if (queueCtrl.hasPrevious()) {
            mPlayer.skipToPrevious();
        }
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

    public void destroyPlayer() {
        Spotify.destroyPlayer(PlayService.mPlayer);
        if (notificationActive) {
            mNotificationManager.cancel(1);
        }
        PLAYING=false;
        stopSelf();
    }

    private void updateNotificationInfo() {
        Log.i(TAG, "Updating notification");
        contentView.setImageViewResource(R.id.image, R.drawable.no_image);
        contentView.setTextViewText(R.id.tvTrackTitle, queueCtrl.getCurrentTrack().getTrack());
        contentView.setTextViewText(R.id.tvArtistAndAlbum, queueCtrl.getCurrentTrack().getSimpleArtist() + " - " + queueCtrl.getCurrentTrack().getAlbum());
        mNotificationManager.notify(1, notification);
    }

    private void updateNotificationButtons() {
        Log.i(TAG, "Updating notification");
        if (!PLAYING) {
            contentView.setImageViewResource(R.id.ivPlayPause_2, R.drawable.play_selector);
        } else {
            contentView.setImageViewResource(R.id.ivPlayPause_2, R.drawable.pause_selector);
        }
        mNotificationManager.notify(1, notification);
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
            Log.i(TAG, intent.getAction());
            if (queueCtrl.hasTracks()) {
                if (intent.getAction().equals(actionPlayPause)) {
                    resumePause();
                } else if (intent.getAction().equals(actionNext)) {
                    nextTrack();
                }
            }
            if (intent.getAction().equals(actionDismiss)) {
                destroyPlayer();
            }
        }

    }
}
