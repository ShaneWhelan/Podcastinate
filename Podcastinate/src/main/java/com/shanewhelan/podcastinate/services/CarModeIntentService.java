package com.shanewhelan.podcastinate.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.shanewhelan.podcastinate.Utilities;
/**
 * Created by Shane on 01/04/2014. Podcastinate.
 */
public class CarModeIntentService extends IntentService {

    @SuppressWarnings("UnusedDeclaration")
    public CarModeIntentService() {
        super("");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    @SuppressWarnings("UnusedDeclaration")
    public CarModeIntentService(String name) {
        super(name);
    }

    /**
     * Called when a new activity detection update is available.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    protected void onHandleIntent(Intent intent) {
        // If the incoming intent contains an update
        if (ActivityRecognitionResult.hasResult(intent)) {
            // Get the update
            ActivityRecognitionResult result =
                    ActivityRecognitionResult.extractResult(intent);
            // Get the most probable activity
            DetectedActivity mostProbableActivity =
                    result.getMostProbableActivity();

            /*
             * Get an integer describing the type of activity
             */
            int activityType = mostProbableActivity.getType();

            String activityName = getNameFromType(activityType);
            Log.d("sw9", activityName);
            if(activityName.equals("tilting")) {
                sendBroadcast(new Intent(Utilities.ACTION_CAR_MODE_ON));
            }

            /*
             * At this point, you have retrieved all the information
             * for the current update. You can display this
             * information to the user in a notification, or
             * send it to an Activity or Service in a broadcast
             * Intent.
             */
        } else {

            /*
             * This implementation ignores intents that don't contain
             * an activity update. If you wish, you can report them as
             * errors.
             */
        }
    }

    /**
     * Map detected activity types to strings
     *@param activityType The detected activity type
     *@return A user-readable name for the type
     */
    private String getNameFromType(int activityType) {
        switch(activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.TILTING:
                return "tilting";
        }
        return "unknown";
    }
}
