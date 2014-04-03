package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.services.AudioPlayerService;
import com.shanewhelan.podcastinate.services.CarModeIntentService;

public class PlayerActivity extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener{
    private AudioPlayerService audioService;
    private ServiceConnection serviceConnection;
    private ImageButton playButton;
    private ImageButton pauseButton;
    private TextView elapsedText;
    private TextView remainingText;
    private SeekBar seekBar;
    private Handler timerHandler = new Handler();
    private boolean isInCarMode;
    private ImageButton skipBack;
    private ImageButton skipForward;

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
                returnToPodcastViewer();
            }
        }
    };

    BroadcastReceiver carModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Utilities.ACTION_CAR_MODE_ON.equals(intent.getAction())) {
                if(getApplicationContext() != null) {
                    SharedPreferences sharedPreferences = PreferenceManager.
                            getDefaultSharedPreferences(getApplicationContext());
                    if (sharedPreferences.getBoolean("car_mode_enabled", true)) {
                        activateCarMode();
                    }
                }
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

        if(getApplicationContext() != null) {
            // Start with the request flag set to false
            mInProgress = false;
            /*
            * Instantiate a new activity recognition client. Since the
            * parent Activity implements the connection listener and
            * connection failure listener, the constructor uses "this"
            * to specify the values of those parameters.
            */
            mActivityRecognitionClient =
                    new ActivityRecognitionClient(getApplicationContext(), this, this);
            /*
            * Create the PendingIntent that Location Services uses
            * to send activity recognition updates back to this app.
            */
            Intent intent = new Intent(
                    getApplicationContext(), CarModeIntentService.class);
            /*
            * Return a PendingIntent that starts the IntentService.
            */
            mActivityRecognitionPendingIntent =
                    PendingIntent.getService(getApplicationContext(), 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    private void initialiseLayout() {
        isInCarMode = false;
        setContentView(R.layout.activity_player);
        ImageView podcastImage = (ImageView) findViewById(R.id.podcastImage);
        podcastImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView description = (TextView) findViewById(R.id.description_player);
                if(description.getVisibility() == View.GONE) {
                    // Remove HTML and CDATA tags
                    try {
                        description.setText(audioService.getEpisode().getDescription()
                                .replace("<![CDATA[", "").replace("]]>", "").replaceAll("<[^>]*>", " ")
                                .trim());
                    } catch (Exception e) {
                        Utilities.logException(e);
                    }
                    description.setVisibility(View.VISIBLE);
                } else {
                    description.setVisibility(View.GONE);
                }
            }
        });

        playButton = (ImageButton) findViewById(R.id.playerPlayButton);
        pauseButton = (ImageButton) findViewById(R.id.playerPauseButton);
        seekBar = (SeekBar) findViewById(R.id.seekBarPlayer);
        elapsedText = (TextView) findViewById(R.id.timeElapsed);
        remainingText = (TextView) findViewById(R.id.timeRemaining);
        skipBack = (ImageButton) findViewById(R.id.skipBack);
        skipForward = (ImageButton) findViewById(R.id.skipForward);

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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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
                startUpdates();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                audioService = null;
                isInCarMode = false;
                syncUserInterface();
                stopUpdates();
            }
        };

        Intent intent = new Intent(this, AudioPlayerService.class);
        intent.setAction(Utilities.ACTION_NEW_EPISODE);
        bindService(intent, serviceConnection, 0);

        registerReceiver(audioReceiver, new IntentFilter(Utilities.ACTION_PLAY));
        registerReceiver(audioReceiver, new IntentFilter(Utilities.ACTION_PAUSE));
        registerReceiver(audioReceiver, new IntentFilter(Utilities.ACTION_FINISHED));


        if(intent.getAction() != null) {
            if (Utilities.ACTION_CAR_MODE_OFF.equals(intent.getAction())) {
                carModeReceiver = null;
            } else {
                registerReceiver(carModeReceiver, new IntentFilter(Utilities.ACTION_CAR_MODE_ON));
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(serviceConnection);
        unregisterReceiver(audioReceiver);
        if(carModeReceiver != null) {
            unregisterReceiver(carModeReceiver);
        }
        stopUpdates();
        isInCarMode = false;
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(10);
    }

    public void syncUserInterface() {
        if (audioService != null) {
            if (audioService.getPlayer() != null) {
                // Change title of activity
                setTitle(audioService.getPodcastTitle());

                if(!isInCarMode) {
                    // Set up ImageView for the Player
                    ImageView podcastImage = (ImageView) findViewById(R.id.podcastImage);
                    podcastImage.setImageBitmap(audioService.getPodcastBitmapLarge());

                    TextView playerEpisodeTitle = (TextView) findViewById(R.id.playerEpisodeTitle);
                    playerEpisodeTitle.setText(audioService.getEpisode().getTitle());

                    LinearLayout controlPanel = (LinearLayout) findViewById(R.id.playerControls);
                    controlPanel.setVisibility(View.VISIBLE);
                }

                if (audioService.getPlayer().isPlaying()) {
                    playButton.setVisibility(View.GONE);
                    pauseButton.setVisibility(View.VISIBLE);
                    if(!isInCarMode) {
                        seekBar.setProgress(audioService.getPlayer().getCurrentPosition());
                        seekBar.setMax(audioService.getPlayer().getDuration());
                        seekBar.setVisibility(View.VISIBLE);
                    }
                } else {
                    playButton.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(View.GONE);
                    if(!isInCarMode) {
                        seekBar.setProgress(audioService.getLastPausedPosition());
                        seekBar.setMax(audioService.getPlayer().getDuration());
                        seekBar.setVisibility(View.VISIBLE);
                        // timerHandler.removeCallbacks(updateTimers);
                    }
                }
                if(!isInCarMode) {
                    updatePlayerTimers();
                }
            } else {
                returnToPodcastViewer();
            }
        } else {
            Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(mainIntent);
        }
    }

    private void activateCarMode() {
        isInCarMode = true;
        setContentView(R.layout.activity_player_car_mode);
        initialiseCarModeLayout();
        syncUserInterface();

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.setAction(Utilities.ACTION_CAR_MODE_OFF);
        // Start PlayerActivity in the future
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Create Notification using Notification Compatibility
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification_running)
                .setContentTitle("Car Mode Activated")
                .setContentText("Touch to Go to Normal Player")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Build Notification with Notification Manager
        notificationManager.notify(10, builder.build());
    }

    private void initialiseCarModeLayout() {
        playButton = (ImageButton) findViewById(R.id.playerPlayButton_car_mode);
        pauseButton = (ImageButton) findViewById(R.id.playerPauseButton_car_mode);
        skipBack = (ImageButton) findViewById(R.id.skipBack_car_mode);
        skipForward = (ImageButton) findViewById(R.id.skipForward_car_mode);

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
                Intent upIntent;
                if(audioService != null) {
                    if(audioService.getPlayer() != null) {
                        upIntent = new Intent(getApplicationContext(), PodcastViewerActivity.class);
                        upIntent.setAction(Utilities.VIEW_PODCAST);
                        upIntent.putExtra(Utilities.PODCAST_TITLE, audioService.getPodcastTitle());
                        upIntent.putExtra(Utilities.PODCAST_ID, audioService.getEpisode().getPodcastID());
                    } else {
                        upIntent = new Intent(getApplicationContext(), MainActivity.class);
                    }
                } else {
                    upIntent = new Intent(getApplicationContext(), MainActivity.class);
                }

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
                Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updatePlayerTimers() {
        // Start the thread that updates the elapsed/remaining timers
        timerHandler.post(updateTimers);
    }

    public void returnToPodcastViewer() {
        // Start Podcast Viewer Activity and
        Intent backIntent;
        if(audioService != null) {
            if(audioService.getPlayer() != null) {
                backIntent = new Intent(getApplicationContext(), PodcastViewerActivity.class);
                backIntent.setAction(Utilities.VIEW_PODCAST);
                backIntent.putExtra(Utilities.PODCAST_TITLE, audioService.getPodcastTitle());
                backIntent.putExtra(Utilities.PODCAST_ID, audioService.getEpisode().getPodcastID());
            } else {
                backIntent = new Intent(getApplicationContext(), MainActivity.class);
            }
        } else {
            backIntent = new Intent(getApplicationContext(), MainActivity.class);
        }
        TaskStackBuilder.create(getApplicationContext())
                // Make sure that we return to PodcastViewerActivity and set the MainActivity as the back button action
                .addNextIntentWithParentStack(backIntent).startActivities();
    }

    // Global constants
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 10;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;
    // Flag that indicates if a request is underway.
    private boolean mInProgress;
    public enum REQUEST_TYPE {START, STOP}
    private REQUEST_TYPE mRequestType;

    /*
     * Store the PendingIntent used to send activity recognition events
     * back to the app
     */
    private PendingIntent mActivityRecognitionPendingIntent;
    // Store the current activity recognition client
    private ActivityRecognitionClient mActivityRecognitionClient;

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    /*
     * Handle results returned to the FragmentActivity
     * by Google Play services
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {

            case CONNECTION_FAILURE_RESOLUTION_REQUEST :
        /*
         * If the result code is Activity.RESULT_OK, try
         * to connect again
         */
                switch (resultCode) {
                    case Activity.RESULT_OK :
                /*
                 * Try the request again
                 */
                        break;
                }
        }
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("sw9", "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getSupportFragmentManager(), "Activity Recognition");
            }
            return false;
        }
    }


    /*
    * Called by Location Services once the location client is connected.
    *
    * Continue by requesting activity updates.
    */
    @Override
    public void onConnected(Bundle bundle) {
        switch (mRequestType) {
            case START :
                /*
                * Request activity recognition updates using the
                * preset detection interval and PendingIntent.
                * This call is synchronous.
                */
                mActivityRecognitionClient.requestActivityUpdates(
                        DETECTION_INTERVAL_MILLISECONDS,
                        mActivityRecognitionPendingIntent);

                /*
                * Since the preceding call is synchronous, turn off the
                * in progress flag and disconnect the client
                */
                mInProgress = false;
                mActivityRecognitionClient.disconnect();
                break;
            case STOP :
                mActivityRecognitionClient.removeActivityUpdates(
                        mActivityRecognitionPendingIntent);
                break;
                /*
                 * An enum was added to the definition of REQUEST_TYPE,
                 * but it doesn't match a known case. Throw an exception.
                 */
            default :
                try {
                    throw new Exception("Unknown request type in onConnected().");
                } catch (Exception e) {
                    Utilities.logException(e);
                }
                break;
        }


    }


    /*
    * Called by Location Services once the activity recognition
    * client is disconnected.
    */
    @Override
    public void onDisconnected() {
        // Turn off the request flag
        mInProgress = false;
        // Delete the client
        mActivityRecognitionClient = null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Turn off the request flag
        mInProgress = false;
        /*
         * If the error has a resolution, start a Google Play services
         * activity to resolve it.
         */
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                Utilities.logException(e);
            }
            // If no resolution is available, display an error dialog
        } else {
            // Get the error code
            int errorCode = connectionResult.getErrorCode();
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    errorCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);
            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(
                        getSupportFragmentManager(),
                        "Activity Recognition");
            }
        }
    }

    /**
     * Request activity recognition updates based on the current
     * detection interval.
     *
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public void startUpdates() {
        // Set the request type to START
        mRequestType = REQUEST_TYPE.START;

        /*
        * Test for Google Play services after setting the request type.
        * If Google Play services isn't present, the proper request type
        * can be restarted.
        */
        if (!servicesConnected()) {
            return;
        }
        // If a request is not already underway
        if (!mInProgress) {
            // Indicate that a request is in progress
            mInProgress = true;
            // Request a connection to Location Services
            mActivityRecognitionClient.connect();
            //
        } else {
            /*
             * A request is already underway. You can handle
             * this situation by disconnecting the client,
             * re-setting the flag, and then re-trying the
             * request.
             */
        }
    }


    /**
     * Turn off activity recognition updates
     *
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public void stopUpdates() {
        // Set the request type to STOP
        mRequestType = REQUEST_TYPE.STOP;
        /*
         * Test for Google Play services after setting the request type.
         * If Google Play services isn't present, the request can be
         * restarted.
         */
        if (!servicesConnected()) {
            return;
        }
        // If a request is not already underway
        if (!mInProgress) {
            // Indicate that a request is in progress
            mInProgress = true;
            // Request a connection to Location Services
            mActivityRecognitionClient.connect();
            //
        } else {
            /*
             * A request is already underway. You can handle
             * this situation by disconnecting the client,
             * re-setting the flag, and then re-trying the
             * request.
             */
        }
    }
}
