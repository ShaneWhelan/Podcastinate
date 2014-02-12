package com.shanewhelan.podcastinate;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Shane on 04/02/14. Podcastinate.
 */
public class Utilities {
    public static final String ACTION_PLAY = "com.shanewhelan.podcastinate.PLAY";
    public static final String ACTION_PAUSE = "com.shanewhelan.podcastinate.PAUSE";
    public static final String ACTION_DOWNLOADED = "com.shanewhelan.podcastinate.DOWNLOADED";
    public static final String ACTION_FINISHED = "com.shanewhelan.podcastinate.FINISHED";

    public static boolean testNetwork(Context context) {
        if(context.getApplicationContext() != null) {
            ConnectivityManager conMan = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = conMan.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            } else if (networkInfo == null) {
                // Alert the user that network connection methods are off
                Log.i("sw9", "WI-FI or Mobile Data turned off");
                int duration = Toast.LENGTH_LONG;
                Toast.makeText(context.getApplicationContext(), "WI-FI or Mobile Data turned off",
                        duration).show();

                return false;
            } else if (!networkInfo.isConnected()) {
                // Alert the user that network is not available.
                Log.i("sw9", "Connected but no internet access");
                int duration = Toast.LENGTH_LONG;
                Toast.makeText(context.getApplicationContext(), "Connected but no internet access",
                            duration).show();
                return false;
            }
        }
        return false;
    }
}
