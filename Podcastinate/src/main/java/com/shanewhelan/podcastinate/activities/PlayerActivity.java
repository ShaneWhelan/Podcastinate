package com.shanewhelan.podcastinate.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
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
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                timerHandler.removeCallbacks(updateTimers);
            } else if (Utilities.ACTION_FINISHED.equals(intent.getAction())) {

                // Verify this is the right thing to do
                timerHandler.removeCallbacks(updateTimers);
                Intent episodesIntent = new Intent(getApplicationContext(), PodcastViewerActivity.class);
                episodesIntent.putExtra(Utilities.PODCAST_TITLE, intent.getStringExtra(Utilities.PODCAST_TITLE));
                startActivity(episodesIntent);
            }
        }
    };

    private Runnable updateTimers = new Runnable() {
        @Override
        public void run() {
            if (audioService != null) {
                if (audioService.getPlayer() != null) {
                    if (audioService.getPlayer().isPlaying()) {
                        int currentPos = audioService.getPlayer().getCurrentPosition();
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
        setContentView(R.layout.activity_player);

        initialiseButtons();
    }

    private void initialiseButtons() {
        playButton = (ImageButton) findViewById(R.id.mainPlayButton);
        pauseButton = (ImageButton) findViewById(R.id.mainPauseButton);
        seekBar = (SeekBar) findViewById(R.id.seekBarPlayer);
        elapsedText = (TextView) findViewById(R.id.timeElapsed);
        remainingText = (TextView) findViewById(R.id.timeRemaining);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if audio service has been initialised and is playing
                if (audioService == null) {
                    // Play podcast in a background service
                    Intent intent = new Intent(getApplicationContext(), AudioPlayerService.class);
                    intent.putExtra(AudioPlayerService.DIRECTORY, v.getContentDescription());
                    //intent.putExtra(DownloadActivity.PODCAST_TITLE, podcastTitle);
                    intent.setAction(AudioPlayerService.ACTION_PLAY);
                    // Investigate Correct flag and compatibility
                    if (getApplicationContext() != null) {
                        getApplicationContext().startService(intent);
                        getApplicationContext().bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
                    }
                } else {
                    audioService.resumeMedia();
                }
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pause podcast in background service
                audioService.pauseMedia();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

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
                }
                updatePlayerTimers();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onResume() {
        super.onResume();

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                AudioPlayerService.AudioPlayerBinder b = (AudioPlayerService.AudioPlayerBinder) service;
                audioService = b.getService();
                syncUserInterface();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                audioService = null;
                syncUserInterface();

            }
        };

        Intent intent = new Intent(this, AudioPlayerService.class);
        intent.setAction(AudioPlayerService.ACTION_PLAY);
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
        LinearLayout controlPanel = (LinearLayout) findViewById(R.id.controlPanel);
        if (audioService != null) {
            if (audioService.getPlayer() != null) {
                controlPanel.setVisibility(View.VISIBLE);
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                // Set up image for podcast
                                PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
                                pds.openDb();
                                final String imageDirectory = pds.getPodcastImage(audioService.getEpisode().getPodcastID());
                                pds.closeDb();

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ImageView podcastImage = (ImageView) findViewById(R.id.podcastImage);
                                        podcastImage.setImageBitmap(BitmapFactory.decodeFile(imageDirectory));
                                    }
                                });
                            }
                        }
                ).start();

                if (audioService.getPlayer().isPlaying()) {
                    playButton.setVisibility(View.GONE);
                    pauseButton.setVisibility(View.VISIBLE);
                    seekBar.setMax(audioService.getPlayer().getDuration());
                    seekBar.setVisibility(View.VISIBLE);
                    updatePlayerTimers();
                }
            }
        } else {
            RelativeLayout playerWindow = (RelativeLayout) findViewById(R.id.playerWindow);
            playerWindow.setVisibility(View.GONE);
            controlPanel.setVisibility(View.GONE);
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
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void updatePlayerTimers() {
        // Start the thread that updates the elapsed/remaining timers
        timerHandler.post(updateTimers);
    }
}
