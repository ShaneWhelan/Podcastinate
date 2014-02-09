package com.shanewhelan.podcastinate.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastContract.EpisodeEntry;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.services.AudioPlayerService;

import static android.widget.CursorAdapter.*;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public class PodcastViewerActivity extends Activity {
    private static EpisodeAdapter episodeAdapter;
    private static PodcastDataSource dataSource;
    private Cursor episodeCursor;
    private int podcastID;
    private ImageButton playButton;
    private ImageButton pauseButton;
    private AudioPlayerService audioService;
    private ServiceConnection serviceConnection;

    BroadcastReceiver playReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Utilities.ACTION_PLAY.equals(intent.getAction())) {
                playButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                updateListOfPodcasts();
            }
        }
    };

    BroadcastReceiver pauseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Utilities.ACTION_PAUSE.equals(intent.getAction())) {
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                updateListOfPodcasts();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episode_list);
        Intent intent = getIntent();
        String podcastName = intent.getStringExtra("userChoice");

        dataSource = new PodcastDataSource(this);

        dataSource.openDb();

        podcastID = dataSource.getPodcastID(podcastName);
        episodeCursor = dataSource.getAllEpisodeNames(podcastID);
        episodeAdapter = new EpisodeAdapter(this, episodeCursor,
                FLAG_REGISTER_CONTENT_OBSERVER);
        episodeAdapter.setPodcastTitle(podcastName);
        ListView listView = (ListView) findViewById(R.id.listOfEpisodes);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(episodeAdapter);
        dataSource.closeDb();

        playButton = (ImageButton) findViewById(R.id.mainPlayButton);
        pauseButton = (ImageButton) findViewById(R.id.mainPauseButton);

        OnItemClickListener itemCLickHandler = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = (TextView) view.findViewById(R.id.episodeName);
                if (textView.getText() != null) {
                    Log.d("sw9", textView.getText().toString());
                }
            }
        };
        listView.setOnItemClickListener(itemCLickHandler);

        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if audio service has been initialised and is playing
                // TODO: Verify code
                if(audioService == null) {
                    // Play podcast in a background service
                    Intent intent = new Intent(getApplicationContext(), AudioPlayerService.class);
                    intent.putExtra(AudioPlayerService.DIRECTORY, v.getContentDescription());
                    //intent.putExtra(DownloadActivity.PODCAST_TITLE, podcastTitle);
                    intent.setAction(AudioPlayerService.ACTION_PLAY);
                    // Investigate Correct flag and compatibility
                    if(getApplicationContext() != null) {
                        getApplicationContext().startService(intent);
                        getApplicationContext().bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
                    }
                } else {
                    audioService.resumeMedia();

                    // Check this isn't called twice
                    updateListOfPodcasts();
                }
            }
        });

        pauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if audio service has been initialised and is playing
                // Pause podcast in background service
                audioService.pauseMedia();
                updateListOfPodcasts();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if(episodeAdapter.getAudioService() != null) {
            unbindService(serviceConnection);
        }
        unregisterReceiver(pauseReceiver);
        unregisterReceiver(playReceiver);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onResume() {
        super.onResume();
        // TODO: Check here for restarting service

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                AudioPlayerService.AudioPlayerBinder b = (AudioPlayerService.AudioPlayerBinder) service;
                audioService = b.getService();
                togglePauseButton();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                audioService = null;
            }
        };

        Intent intent = new Intent(this, AudioPlayerService.class);
        intent.setAction(AudioPlayerService.ACTION_PLAY);
        bindService(intent, serviceConnection, 0);

        registerReceiver(playReceiver, new IntentFilter(Utilities.ACTION_PLAY));
        registerReceiver(pauseReceiver, new IntentFilter(Utilities.ACTION_PAUSE));
    }

    public void togglePauseButton() {
        if(audioService != null) {
            if(audioService.getPlayer().isPlaying()) {
                playButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
            }
        }
    }

    public void updateListOfPodcasts() {
        dataSource.openDb();
        episodeCursor = dataSource.getAllEpisodeNames(podcastID);
        episodeAdapter.swapCursor(episodeCursor);
        episodeAdapter.notifyDataSetChanged();
        dataSource.closeDb();
    }









    public class EpisodeAdapter extends CursorAdapter implements View.OnClickListener {
        private final LayoutInflater layoutInflater;
        public Context context;
        private String podcastTitle;



        public EpisodeAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            layoutInflater = LayoutInflater.from(context);
            this.context = context;
        }

        public void setPodcastTitle(String podcastTitle) {
            this.podcastTitle = podcastTitle;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String episodeTitle = cursor.getString(cursor.getColumnIndex(EpisodeEntry.COLUMN_NAME_TITLE));
            TextView episodeNameView = (TextView) view.findViewById(R.id.episodeName);
            episodeNameView.setText(episodeTitle);

            ImageButton downloadButton = (ImageButton) view.findViewById(R.id.download_icon);
            ImageButton playButton = (ImageButton) view.findViewById(R.id.play_icon);
            ImageButton pauseButton = (ImageButton) view.findViewById(R.id.pause_icon);

            downloadButton.setContentDescription(episodeTitle);
            // Set up listeners or nothing will work
            downloadButton.setOnClickListener(this);
            playButton.setOnClickListener(this);
            pauseButton.setOnClickListener(this);

            String directory = cursor.getString(cursor.getColumnIndex(EpisodeEntry.COLUMN_NAME_DIRECTORY));

            if(directory != null && episodeTitle != null) { // Check if the file is downloaded
                if(audioService == null) { // Check if audio service is initialised
                    downloadButton.setVisibility(View.GONE);
                    pauseButton.setVisibility(View.GONE);
                    playButton.setVisibility(View.VISIBLE);
                    playButton.setContentDescription(directory);
                }else if(audioService.getPlayer().isPlaying() && episodeTitle.equals(audioService.getEpisode().getTitle())) {
                    downloadButton.setVisibility(View.GONE);
                    playButton.setVisibility(View.GONE);
                    pauseButton.setVisibility(View.VISIBLE);
                    pauseButton.setContentDescription(directory);
                }else {
                    downloadButton.setVisibility(View.GONE);
                    pauseButton.setVisibility(View.GONE);
                    playButton.setVisibility(View.VISIBLE);
                    playButton.setContentDescription(directory);
                }
            }else{
                playButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.GONE);
                downloadButton.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return layoutInflater.inflate(R.layout.episode_list_item, parent, false);
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Override
        public void onClick(View v) {
            int viewId = v.getId();

            if(viewId == R.id.download_icon) {
                // Download the podcast
                // TODO: Send more info to downloader service
                Intent intent = new Intent(context, DownloadActivity.class);
                intent.putExtra(DownloadActivity.EPISODE_TITLE, v.getContentDescription());
                intent.putExtra(DownloadActivity.PODCAST_TITLE, podcastTitle);
                context.startActivity(intent);
            } else if(viewId == R.id.play_icon) {
                if(audioService == null) {
                    // Play podcast in a background service
                    // TODO: Send more info to play service
                    Intent intent = new Intent(context, AudioPlayerService.class);
                    intent.putExtra(AudioPlayerService.DIRECTORY, v.getContentDescription());
                    intent.putExtra(DownloadActivity.PODCAST_TITLE, podcastTitle);
                    intent.setAction(AudioPlayerService.ACTION_PLAY);
                    // Investigate Correct flag and compatibility
                    context.startService(intent);
                    context.bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
                } else {
                    if(v.getContentDescription() != null) {
                        String directory = v.getContentDescription().toString();
                        if(audioService.getDirectory().equals(directory)) {
                            audioService.resumeMedia();
                            // TODO: Check this isn't called twice by the broadcast receiver
                            updateListOfPodcasts();
                        }else{
                            audioService.playNewEpisode(directory);
                        }
                    }
                }
            } else if(viewId == R.id.pause_icon) {
                // Pause podcast in background service
                audioService.pauseMedia();
                updateListOfPodcasts();
            }
        }

        public AudioPlayerService getAudioService() {
            return audioService;
        }

    }
}
