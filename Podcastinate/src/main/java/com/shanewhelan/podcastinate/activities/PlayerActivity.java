package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.services.AudioPlayerService;

public class PlayerActivity extends Activity {
    private AudioPlayerService audioService;
    private ServiceConnection serviceConnection;
    private ImageButton playButton;
    private ImageButton pauseButton;
    private TextView elapsedText;
    private TextView remainingText;
    private SeekBar seekBar;
    private Handler timerHandler = new Handler();

    BroadcastReceiver audioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utilities.ACTION_PLAY.equals(intent.getAction())) {
                playButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                updatePlayerTimers();
            } else if (Utilities.ACTION_PAUSE.equals(intent.getAction())) {
                pauseButton.setVisibility(View.GONE);
                playButton.setVisibility(View.VISIBLE);
                timerHandler.removeCallbacks(updateTimers);
            } else if (Utilities.ACTION_FINISHED.equals(intent.getAction())) {
                timerHandler.removeCallbacks(updateTimers);
                returnToPodcastViewer(intent.getStringExtra(Utilities.PODCAST_TITLE));
            }
        }
    };

    private Runnable updateTimers = new Runnable() {
        @Override
        public void run() {
            if (audioService != null) {
                if (audioService.getPlayer() != null) {
                    int currentPos;

                    if (audioService.getPlayer().isPlaying()) {
                        // Current position while playing
                        currentPos = audioService.getPlayer().getCurrentPosition();
                    } else {
                        // Last position that podcast was at before pausing
                        currentPos = audioService.getLastPausedPosition();
                    }
                    //Update SeekBar too
                    seekBar.setProgress(currentPos);

                    int duration = audioService.getPlayer().getDuration();

                    int hours = currentPos / 1000 / 60 / 60;
                    int minutes = (currentPos / 1000 / 60) % 60;
                    int seconds = currentPos / 1000 % 60;

                    if (hours > 0 && hours < 10) {
                        elapsedText.setText(String.format("%01d:%02d:%02d", hours, minutes, seconds));
                    } else if (hours > 10) {
                        elapsedText.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    } else {
                        elapsedText.setText(String.format("%02d:%02d", minutes, seconds));
                    }

                    int remHours = (duration - currentPos) / 1000 / 60 / 60;
                    int remMinutes = ((duration - currentPos) / 1000 / 60) % 60;
                    int remSeconds = (duration - currentPos) / 1000 % 60;

                    if (remHours > 0 && remHours < 10) {
                        remainingText.setText(String.format("-%01d:%02d:%02d", remHours, remMinutes, remSeconds));
                    } else if (hours > 10) {
                        remainingText.setText(String.format("-%02d:%02d:%02d", remHours, remMinutes, remSeconds));
                    } else {
                        remainingText.setText(String.format("-%02d:%02d", remMinutes, remSeconds));
                    }


                    if(audioService.getPlayer().isPlaying()) {
                        // Call this thread again
                        timerHandler.postDelayed(this, 1000);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void initialiseLayout() {
        setContentView(R.layout.activity_player);

        playButton = (ImageButton) findViewById(R.id.playerPlayButton);
        pauseButton = (ImageButton) findViewById(R.id.playerPauseButton);
        seekBar = (SeekBar) findViewById(R.id.seekBarPlayer);
        elapsedText = (TextView) findViewById(R.id.timeElapsed);
        remainingText = (TextView) findViewById(R.id.timeRemaining);
        ImageButton skipBack = (ImageButton) findViewById(R.id.skipBack);
        ImageButton skipForward = (ImageButton) findViewById(R.id.skipForward);

        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if audio service has been initialised and is playing
                if (audioService != null) {
                    audioService.resumeMedia();
                }
            }
        });

        pauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pause podcast in background service
                audioService.pauseMedia(false);
            }
        });

        skipBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                audioService.skipBack(30000);
            }
        });

        skipForward.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                audioService.skipForward(30000);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    // This happens when the SeekBar is moved physically by the user#
                    int duration = audioService.getPlayer().getDuration();

                    int hours = progress / 1000 / 60 / 60;
                    int minutes = (progress / 1000 / 60) % 60;
                    int seconds = progress / 1000 % 60;

                    if (hours > 0 && hours < 10) {
                        elapsedText.setText(String.format("%01d:%02d:%02d", hours, minutes, seconds));
                    } else if (hours > 10) {
                        elapsedText.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    } else {
                        elapsedText.setText(String.format("%02d:%02d", minutes, seconds));
                    }

                    int remHours = (duration - progress) / 1000 / 60 / 60;
                    int remMinutes = ((duration - progress) / 1000 / 60) % 60;
                    int remSeconds = (duration - progress) / 1000 % 60;

                    if (remHours > 0 && remHours < 10) {
                        remainingText.setText(String.format("-%01d:%02d:%02d", remHours, remMinutes, remSeconds));
                    } else if (hours > 10) {
                        remainingText.setText(String.format("-%02d:%02d:%02d", remHours, remMinutes, remSeconds));
                    } else {
                        remainingText.setText(String.format("-%02d:%02d", remMinutes, remSeconds));
                    }
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Allow us to display potential seek to time by removing update of time.
                timerHandler.removeCallbacks(updateTimers);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Allow us to display potential seek to time by removing update of time.
                if (audioService != null) {
                    audioService.setProgress(seekBar.getProgress());
                    // Only if playing, continue updating the time
                    if(audioService.getPlayer().isPlaying()){
                        updatePlayerTimers();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                AudioPlayerService.AudioPlayerBinder b = (AudioPlayerService.AudioPlayerBinder) service;
                audioService = b.getService();
                initialiseLayout();
                syncUserInterface();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                audioService = null;
                syncUserInterface();
            }
        };

        Intent intent = new Intent(this, AudioPlayerService.class);
        intent.setAction(Utilities.ACTION_NEW_EPISODE);
        bindService(intent, serviceConnection, 0);

        registerReceiver(audioReceiver, new IntentFilter(Utilities.ACTION_PLAY));
        registerReceiver(audioReceiver, new IntentFilter(Utilities.ACTION_PAUSE));
        registerReceiver(audioReceiver, new IntentFilter(Utilities.ACTION_FINISHED));
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(serviceConnection);
        unregisterReceiver(audioReceiver);
    }

    public void syncUserInterface() {
        LinearLayout controlPanel = (LinearLayout) findViewById(R.id.playerControls);
        if (audioService != null) {
            if (audioService.getPlayer() != null) {
                // Set up ImageView for the Player
                ImageView podcastImage = (ImageView) findViewById(R.id.podcastImage);
                podcastImage.setImageBitmap(audioService.getPodcastBitmapLarge());

                // Change title of activity
                setTitle(audioService.getPodcastTitle());
                TextView playerEpisodeTitle = (TextView) findViewById(R.id.playerEpisodeTitle);
                playerEpisodeTitle.setText(audioService.getEpisode().getTitle());
                controlPanel.setVisibility(View.VISIBLE);

                if (audioService.getPlayer().isPlaying()) {
                    playButton.setVisibility(View.GONE);
                    pauseButton.setVisibility(View.VISIBLE);
                    seekBar.setProgress(audioService.getPlayer().getCurrentPosition());
                    seekBar.setMax(audioService.getPlayer().getDuration());
                    seekBar.setVisibility(View.VISIBLE);
                } else {
                    playButton.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(View.GONE);
                    seekBar.setProgress(audioService.getLastPausedPosition());
                    seekBar.setMax(audioService.getPlayer().getDuration());
                    seekBar.setVisibility(View.VISIBLE);
                    // timerHandler.removeCallbacks(updateTimers);
                }
                updatePlayerTimers();
            } else {
                returnToPodcastViewer(audioService.getPodcastTitle());
            }
        } else {
            Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(mainIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(getApplicationContext(), PodcastViewerActivity.class);
                upIntent.setAction(Utilities.VIEW_PODCAST);
                upIntent.putExtra(Utilities.PODCAST_TITLE, audioService.getPodcastTitle());
                upIntent.putExtra(Utilities.PODCAST_ID, audioService.getEpisode().getPodcastID());
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                                    // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updatePlayerTimers() {
        // Start the thread that updates the elapsed/remaining timers
        timerHandler.post(updateTimers);
    }

    public void returnToPodcastViewer(String podcastTitle) {
        // Start Podcast Viewer Activity and
        Intent backIntent = new Intent(getApplicationContext(), PodcastViewerActivity.class);
        backIntent.setAction(Utilities.VIEW_PODCAST);
        backIntent.putExtra(Utilities.PODCAST_TITLE, podcastTitle);
        backIntent.putExtra(Utilities.PODCAST_ID, audioService.getEpisode().getPodcastID());
        TaskStackBuilder.create(getApplicationContext())
                // Make sure that we return to PodcastViewerActivity and set the MainActivity as the back button action
                .addNextIntentWithParentStack(backIntent).startActivities();
    }
}
