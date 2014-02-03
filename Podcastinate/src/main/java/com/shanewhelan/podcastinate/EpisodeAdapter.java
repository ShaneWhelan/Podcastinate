package com.shanewhelan.podcastinate;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.*;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.shanewhelan.podcastinate.activities.DownloadActivity;
import com.shanewhelan.podcastinate.database.PodcastContract.*;
import com.shanewhelan.podcastinate.services.AudioPlayerService;

/**
 * Created by Shane on 16/01/14. Podcastinate.
 */
public class EpisodeAdapter extends CursorAdapter implements OnClickListener {
    private final LayoutInflater layoutInflater;
    public Context context;
    private String podcastTitle;
    private AudioPlayerService audioService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioPlayerService.AudioPlayerBinder b = (AudioPlayerService.AudioPlayerBinder) service;
            audioService = b.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            audioService = null;
        }
    };

    public EpisodeAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        layoutInflater = LayoutInflater.from(context);
        this.context = context;
    }

    public void setEpisodeName(String episodeName) {
        this.podcastTitle = episodeName;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String episodeName = cursor.getString(cursor.getColumnIndex(EpisodeEntry.COLUMN_NAME_TITLE));
        TextView episodeNameView = (TextView) view.findViewById(R.id.episodeName);
        episodeNameView.setText(episodeName);

        ImageButton downloadButton = (ImageButton) view.findViewById(R.id.download_icon);
        ImageButton playButton = (ImageButton) view.findViewById(R.id.play_icon);
        ImageButton pauseButton = (ImageButton) view.findViewById(R.id.pause_icon);

        downloadButton.setContentDescription(episodeName);
        // Set up listeners or nothing will work
        downloadButton.setOnClickListener(this);
        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);

        String directory = cursor.getString(cursor.getColumnIndex(EpisodeEntry.COLUMN_NAME_DIRECTORY));

        if(directory != null) {
            if(audioService == null) {
                downloadButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.GONE);
                playButton.setVisibility(View.VISIBLE);
                playButton.setContentDescription(directory);
            }else{
                downloadButton.setVisibility(View.GONE);
                playButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setContentDescription(directory);
            }
        }else{
            playButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.GONE);
            downloadButton.setVisibility(View.VISIBLE);
        } // Other else clause if playing
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return layoutInflater.inflate(R.layout.episode_list_item, parent, false);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if(viewId == R.id.download_icon) {
            // Download the podcast
            Intent intent = new Intent(context, DownloadActivity.class);
            intent.putExtra(DownloadActivity.EPISODE_TITLE, v.getContentDescription());
            intent.putExtra(DownloadActivity.PODCAST_TITLE, podcastTitle);
            context.startActivity(intent);
        } else if(viewId == R.id.play_icon) {
            if(audioService == null) {
                // Play podcast in a background service
                Intent intent = new Intent(context, AudioPlayerService.class);
                intent.putExtra(AudioPlayerService.DIRECTORY, v.getContentDescription());
                intent.putExtra(DownloadActivity.PODCAST_TITLE, podcastTitle);
                intent.setAction(AudioPlayerService.ACTION_PLAY);
                // Investigate Correct flag and compatibility
                context.startService(intent);
                context.bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
            } else {
                audioService.resumeMedia();
            }
        } else if(viewId == R.id.pause_icon) {
            // Pause podcast in background service
            audioService.pauseMedia();
        }
    }

    public AudioPlayerService getAudioService() {
        return audioService;
    }

    public ServiceConnection getServiceConnection() {
        return serviceConnection;
    }

}
