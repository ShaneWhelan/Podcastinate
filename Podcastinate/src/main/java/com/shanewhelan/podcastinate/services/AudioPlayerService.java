package com.shanewhelan.podcastinate.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

import com.shanewhelan.podcastinate.Episode;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

import java.io.IOException;

/**
 * Created by Shane on 03/02/14. Podcastinate.
 */

// TODO: Should have subtitle controller already set
// TODO: E/AudioSink﹕ received unknown event type: 1 inside CallbackWrapper !
public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private IBinder iBinder = new AudioPlayerBinder();
    private static MediaPlayer player;
    public static final String ACTION_PLAY = "com.shanewhelan.podcastinate.PLAY";
    public static final String DIRECTORY = "directory";
    public static final String ACTION_DISCONNECT = "1";
    private static String directory;
    private static Episode episode;

    private BroadcastReceiver disconnectJackR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_DISCONNECT.equals(intent.getAction())) {
                if (player != null) {
                    pauseMedia();
                }
            }
        }
    };

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (player == null) {
                    initialiseMediaPlayer();
                }
                player.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (player.isPlaying()) {
                    player.stop();
                    PodcastDataSource pds = new PodcastDataSource(this);
                    pds.openDb();
                    pds.updateCurrentTime(episode.getEpisodeID(), player.getCurrentPosition());
                    pds.closeDb();
                    player.release();
                    player = null;
                }

                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (player.isPlaying()) {
                    player.pause();
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

    private void initialiseMediaPlayer() {
        PodcastDataSource pds = new PodcastDataSource(this);
        pds.openDb();
        player = new MediaPlayer();
        player.reset();
        player.setLooping(false);
        player.seekTo(pds.getCurrentTime(episode.getEpisodeID()));
        player.prepareAsync();
        pds.closeDb();
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_PLAY.equals(intent.getAction())) {
            directory = intent.getStringExtra(DIRECTORY);
            playNewEpisode(directory, false);
        } else if (ACTION_DISCONNECT.equals(intent.getAction())) {
            if (player != null) {
                if (player.isPlaying()) {
                    pauseMedia();
                }
            } else {
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        player.start();
        Intent intent = new Intent();
        intent.setAction(Utilities.ACTION_PLAY);
        sendBroadcast(intent);
        registerReceiver(disconnectJackR, new IntentFilter(ACTION_DISCONNECT));
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void onCompletion(MediaPlayer player) {
        Intent finished = new Intent(Utilities.ACTION_FINISHED);
        sendBroadcast(finished);
        player.release();
        player = null;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        player.reset();
        return false;
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        episode = null;
        iBinder = null;
        directory = null;
        unregisterReceiver(disconnectJackR);
        super.onDestroy();
    }

    public void pauseMedia() {
        player.pause();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
        Intent intent = new Intent();
        intent.setAction(Utilities.ACTION_PAUSE);
        sendBroadcast(intent);
        // TODO: FIX BUG HERE not registered sometimes
        unregisterReceiver(disconnectJackR);
    }

    public void resumeMedia() {
        player.start();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        Intent intent = new Intent();
        intent.setAction(Utilities.ACTION_PLAY);
        sendBroadcast(intent);
        registerReceiver(disconnectJackR, new IntentFilter(ACTION_DISCONNECT));
    }

    public void setProgress(int progress) {
        if (progress < player.getDuration()) {
            player.seekTo(progress);
        } else {
            // TODO: Broadcasting intent before finished
            Intent finished = new Intent(Utilities.ACTION_FINISHED);
            sendBroadcast(finished);
            stopSelf();
        }
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    public Episode getEpisode() {
        return episode;
    }

    public void playNewEpisode(String directory, boolean isPlaying) {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                PodcastDataSource pds = new PodcastDataSource(this);
                pds.openDb();
                episode = pds.getEpisodeMetaData(directory);
                pds.closeDb();

                if (isPlaying) {
                    AudioPlayerService.directory = directory;
                } else {
                    player = new MediaPlayer();
                }

                player.reset();
                player.setDataSource(directory);
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

    public String getDirectory() {
        return directory;
    }
}
