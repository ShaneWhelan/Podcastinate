package com.shanewhelan.podcastinate.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.shanewhelan.podcastinate.Episode;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.activities.PlayerActivity;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

import java.io.IOException;

public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private IBinder iBinder = new AudioPlayerBinder();
    private static MediaPlayer player;
    public static final String ACTION_DISCONNECT = "1";
    // Episode info - essential that it is updated
    private static String podcastTitle;
    private static String podcastImage;
    private static String episodeID;
    private static Episode episode;
    private static int lastPausedPosition;
    private static Bitmap podcastBitmap;
    private static Bitmap podcastBitmapLarge;

    private BroadcastReceiver disconnectJackR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_DISCONNECT.equals(intent.getAction())) {
                if (player != null) {
                    pauseMedia(false);
                }
            }
        }
    };

    private BroadcastReceiver notificationBrodRec = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utilities.ACTION_PAUSE_NOTIFY.equals(intent.getAction())) {
                if(player.isPlaying()) {
                    pauseMedia(false);
                }
            } else if (Utilities.ACTION_PLAY_NOTIFY.equals(intent.getAction())) {
                resumeMedia();
            } else if (Utilities.ACTION_SKIP_BACK_NOTIFY.equals(intent.getAction())) {
                if(player.isPlaying()) {
                    skipBack(30000);
                }
            } else if (Utilities.ACTION_SKIP_FORWARD_NOTIFY.equals(intent.getAction())) {
                if(player.isPlaying()) {
                    skipForward(30000);
                }
            }
        }
    };

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (player != null) {
                    if(player.isPlaying()) {
                        player.setVolume(1.0f, 1.0f);
                    } else {
                        resumeMedia();
                    }
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (player.isPlaying()) {
                    saveEpisodeTimer(false);
                    pauseMedia(false);
                    /*
                    player.stop();
                    player.release();
                    player = null;
                    */
                    //sendBroadcast(new Intent (Utilities.ACTION_PAUSE));
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (player.isPlaying()) {
                    pauseMedia(true);
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (player.isPlaying()) {
                    player.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Utilities.ACTION_NEW_EPISODE.equals(intent.getAction())) {
            episodeID = intent.getStringExtra(Utilities.EPISODE_ID);
            podcastTitle = intent.getStringExtra(Utilities.PODCAST_TITLE);
            playNewEpisode(episodeID, false, podcastTitle);
        } else if (ACTION_DISCONNECT.equals(intent.getAction())) {
            if (player != null) {
                if (player.isPlaying()) {
                    pauseMedia(false);
                }
            } else {
                stopSelf();
            }
        }
        return START_STICKY;
    }

    public void playNewEpisode(String episodeID, boolean isDifferentEpisode, String podcastTitle) {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                if (isDifferentEpisode) {
                    saveEpisodeTimer(false);
                    AudioPlayerService.podcastTitle = podcastTitle;
                } else {
                    player = new MediaPlayer();
                }
                // Retrieve episode information from database
                PodcastDataSource pds = new PodcastDataSource(this);
                pds.openDbForWriting();
                episode = pds.getEpisodeMetaDataForPlay(episodeID);
                if(episode.isNew()) {
                    // While we are at it update the isNew fields in DB, DB instance is only opened once this way
                    pds.updateEpisodeIsNew(episode.getEpisodeID(), 0);
                    int countNew = pds.getCountNew(episode.getPodcastID());
                    if (countNew > 0) {
                        pds.updatePodcastCountNew(episode.getPodcastID(), countNew - 1);
                    }
                }
                podcastImage = pds.getPodcastImage(episode.getPodcastID());
                pds.closeDb();

                player.reset();
                player.setDataSource(episode.getDirectory());
                player.setLooping(false);
                player.setOnPreparedListener(this);
                player.setOnErrorListener(this);
                player.setOnCompletionListener(this);
                // Keeps CPU from sleeping
                player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                player.prepareAsync();
            }
        } catch (IOException e) {
            Utilities.logException(e);
        }
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        player.start();
        // Resume podcast if partially listened
        if(episode.getCurrentTime() > 0) {
            player.seekTo(episode.getCurrentTime());
        }
        registerReceiver(disconnectJackR, new IntentFilter(ACTION_DISCONNECT));
        sendBroadcast(new Intent (Utilities.ACTION_PLAY));

        // The rest of this method is concerned with scaffolding for the notification
        registerReceiver(notificationBrodRec, new IntentFilter(Utilities.ACTION_PAUSE_NOTIFY));
        registerReceiver(notificationBrodRec, new IntentFilter(Utilities.ACTION_PLAY_NOTIFY));
        registerReceiver(notificationBrodRec, new IntentFilter(Utilities.ACTION_SKIP_BACK_NOTIFY));
        registerReceiver(notificationBrodRec, new IntentFilter(Utilities.ACTION_SKIP_FORWARD_NOTIFY));

        BitmapDrawable podcastImageDrawable = (BitmapDrawable) BitmapDrawable.createFromPath(podcastImage);
        podcastBitmapLarge = podcastImageDrawable.getBitmap();
        int height = (int) getResources().getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) getResources().getDimension(android.R.dimen.notification_large_icon_width);

        podcastBitmap = Bitmap.createScaledBitmap(podcastBitmapLarge, width, height, false);

        buildNotification();
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        // So that we don't keep listening for audio changes
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
        try {
            unregisterReceiver(notificationBrodRec);
        } catch (Exception e) {
            Utilities.logException(e);
        }

        try {
            unregisterReceiver(disconnectJackR);
            disconnectJackR = null;
        } catch (Exception e) {
            Utilities.logException(e);
        }

        Intent finished = new Intent(Utilities.ACTION_FINISHED);
        finished.putExtra(Utilities.PODCAST_TITLE, podcastTitle);
        sendBroadcast(finished);
        AudioPlayerService.player.release();
        AudioPlayerService.player = null;
        saveEpisodeTimer(true);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e("sw9", "Media Player Error " + what + " " + extra);
        player.reset();
        return false;
    }

    @Override
    public void onDestroy() {
        // So that we don't keep listening for audio changes
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Build Notification with Notification Manager
        notificationManager.cancel("sw9", 0);

        if(disconnectJackR != null && player != null) {
            if(player.isPlaying()) {
                try {
                    unregisterReceiver(disconnectJackR);
                    disconnectJackR = null;
                } catch (Exception e) {
                    Utilities.logException(e);
                }
            }
        }

        if(notificationBrodRec != null) {
            try {
                unregisterReceiver(notificationBrodRec);
                notificationBrodRec = null;
            } catch (Exception e) {
                Utilities.logException(e);
            }
        }
        if (player != null) {
            player.release();
            player = null;
        }
        episode = null;
        iBinder = null;
        episodeID = null;

        super.onDestroy();
    }

    public class AudioPlayerBinder extends Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    public void pauseMedia(boolean isTransient) {
        lastPausedPosition = player.getCurrentPosition();
        player.pause();
        // Tell Application about pause
        sendBroadcast(new Intent(Utilities.ACTION_PAUSE));
        saveEpisodeTimer(false);

        if(!isTransient) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.abandonAudioFocus(this);
        }

        // Sometimes receiver is not registered this try/catch fixes that
        try {
            unregisterReceiver(disconnectJackR);
        } catch (Exception e) {
            Utilities.logException(e);
        }
        buildNotification();
    }

    public void resumeMedia() {
        // Get audio focus form the system
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        // If we received audio focus then resume the episode
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            player.start();
            // Register the headphone jack receiver
            registerReceiver(disconnectJackR, new IntentFilter(ACTION_DISCONNECT));
            sendBroadcast(new Intent(Utilities.ACTION_PLAY));

            registerReceiver(notificationBrodRec, new IntentFilter(Utilities.ACTION_PAUSE_NOTIFY));
            registerReceiver(notificationBrodRec, new IntentFilter(Utilities.ACTION_PLAY_NOTIFY));
            registerReceiver(notificationBrodRec, new IntentFilter(Utilities.ACTION_SKIP_BACK_NOTIFY));
            registerReceiver(notificationBrodRec, new IntentFilter(Utilities.ACTION_SKIP_FORWARD_NOTIFY));

            buildNotification();
        }
    }

    public void skipBack(int millisToSkip) {
        if((player.getCurrentPosition() - millisToSkip) > 0) {
            player.seekTo(player.getCurrentPosition() - millisToSkip);
        } else {
            player.seekTo(0);
        }
    }

    public void skipForward(int millisToSkip) {
        if((player.getCurrentPosition() + millisToSkip) < player.getDuration()) {
            player.seekTo(player.getCurrentPosition() + millisToSkip);
        } else {
            Intent finished = new Intent(Utilities.ACTION_FINISHED);
            finished.putExtra(Utilities.PODCAST_TITLE, podcastTitle);
            sendBroadcast(finished);
            saveEpisodeTimer(true);
            stopSelf();
        }
    }

    public void setProgress(int progress) {
        if (progress < player.getDuration()) {
            player.seekTo(progress);
            saveEpisodeTimer(false);
        } else {
            Intent finished = new Intent(Utilities.ACTION_FINISHED);
            finished.putExtra(Utilities.PODCAST_TITLE, podcastTitle);
            sendBroadcast(finished);
            saveEpisodeTimer(true);
            stopSelf();
        }
    }

    public void stopService() {
        // Only called on podcast delete, kind of hack to make sure the control panel goes away
        if(player != null) {
            if(player.isPlaying()) {
                pauseMedia(false);
            }
            player.release();
            player = null;
        }

        stopSelf();
    }

    public void saveEpisodeTimer(boolean isFinished) {
        PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
        pds.openDbForWriting();
        if(isFinished) {
            episode.setCurrentTime(0);
            pds.updateCurrentTime(episode.getEpisodeID(), 0);
        } else {
            episode.setCurrentTime(player.getCurrentPosition());
            pds.updateCurrentTime(episode.getEpisodeID(), player.getCurrentPosition());
        }
        pds.closeDb();
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    public Episode getEpisode() {
        return episode;
    }

    public String getPodcastTitle() {
        return podcastTitle;
    }

    public int getLastPausedPosition() {
        return lastPausedPosition;
    }

    public Bitmap getPodcastBitmapLarge() {
        return podcastBitmapLarge;
    }

    public void buildNotification() {
        if(getApplicationContext() != null) {
            SharedPreferences sharedPreferences = PreferenceManager.
                    getDefaultSharedPreferences(getApplicationContext());
            if (sharedPreferences.getBoolean("audio_notification_enabled", true)) {

                Intent viewPodcastIntent = new Intent(this, PlayerActivity.class);
                // Start PlayerActivity in the future
                PendingIntent viewPodcastPendIntent = PendingIntent.getActivity(this, 0, viewPodcastIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                PendingIntent skipBackPendIntent = PendingIntent.getBroadcast(this, 1,
                        new Intent(Utilities.ACTION_SKIP_BACK_NOTIFY), PendingIntent.FLAG_UPDATE_CURRENT);

                PendingIntent skipForwardPendIntent = PendingIntent.getBroadcast(this, 3,
                        new Intent(Utilities.ACTION_SKIP_FORWARD_NOTIFY), PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder builder;

                if (player.isPlaying()) {
                    PendingIntent pausePendIntent = PendingIntent.getBroadcast(this, 2,
                            new Intent(Utilities.ACTION_PAUSE_NOTIFY), PendingIntent.FLAG_UPDATE_CURRENT);

                    // Create Notification using Notification Compatibility
                    builder = new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_notification_running)
                            .setLargeIcon(podcastBitmap)
                            .setContentTitle(episode.getTitle())
                            .setContentText(podcastTitle)
                            .addAction(R.drawable.ic_notification_skip_back, "-30s", skipBackPendIntent)
                            .addAction(R.drawable.ic_notification_pause, "Pause", pausePendIntent)
                            .addAction(R.drawable.ic_notification_skip_forward, "+30s", skipForwardPendIntent)
                            .setContentIntent(viewPodcastPendIntent)
                            .setAutoCancel(false)
                            .setOngoing(true);
                } else {
                    PendingIntent playPendIntent = PendingIntent.getBroadcast(this, 2,
                            new Intent(Utilities.ACTION_PLAY_NOTIFY), PendingIntent.FLAG_UPDATE_CURRENT);

                    // Create Notification using Notification Compatibility
                    builder = new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_notification_running)
                            .setLargeIcon(podcastBitmap)
                            .setContentTitle(episode.getTitle())
                            .setContentText(podcastTitle)
                            .addAction(R.drawable.ic_notification_skip_back, "-30s", skipBackPendIntent)
                            .addAction(R.drawable.ic_notification_play, "Play", playPendIntent)
                            .addAction(R.drawable.ic_notification_skip_forward, "+30s", skipForwardPendIntent)
                            .setContentIntent(viewPodcastPendIntent)
                            .setAutoCancel(false);
                }
                // Create Notification Manager
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                // Build Notification with Notification Manager
                notificationManager.notify("sw9", 0, builder.build());
            }
        }
    }

}
