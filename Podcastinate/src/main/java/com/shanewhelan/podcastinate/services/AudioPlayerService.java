package com.shanewhelan.podcastinate.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import android.os.PowerManager;

import com.shanewhelan.podcastinate.Utilities;

import java.io.IOException;

/**
 * Created by Shane on 03/02/14. Podcastinate.
 */
public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener{
    private IBinder iBinder = new AudioPlayerBinder();
    private MediaPlayer player;
    public static final String ACTION_PLAY = "com.shanewhelan.podcastinate.PLAY";
    public static final String DIRECTORY = "directory";

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
        if(ACTION_PLAY.equals(intent.getAction())) {
            try {
                player = new MediaPlayer();
                player.reset();
                player.setDataSource(intent.getStringExtra(DIRECTORY));
                player.setLooping(false);
                player.setOnPreparedListener(this);
                player.setOnErrorListener(this);
                // Keeps CPU from sleeping
                // TODO: must keep WIFI from sleeping if streaming
                player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                player.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
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
        }
    }

    public void pauseMedia() {
        player.pause();
        Intent intent = new Intent();
        intent.setAction(Utilities.ACTION_PAUSE);
        sendBroadcast(intent);
    }

    public void resumeMedia() {
        player.start();
        Intent intent = new Intent();
        intent.setAction(Utilities.ACTION_PLAY);
        sendBroadcast(intent);
    }


    public MediaPlayer getPlayer() {
        return player;
    }
}
