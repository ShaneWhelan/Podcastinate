package com.shanewhelan.podcastinate;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.shanewhelan.podcastinate.database.PodcastDataSource;

import java.util.ArrayList;

/**
 * Created by Shane on 04/02/14. Podcastinate.
 */
public class Utilities {
    public static final String ACTION_PLAY = "com.shanewhelan.podcastinate.PLAY";
    public static final String ACTION_PAUSE = "com.shanewhelan.podcastinate.PAUSE";
    public static final String ACTION_DOWNLOADED = "com.shanewhelan.podcastinate.DOWNLOADED";
    public static final String ACTION_FINISHED = "com.shanewhelan.podcastinate.FINISHED";
    public static final int INVALID_URL = -1;
    public static final int FAILURE_TO_PARSE = 0;
    public static final int SUCCESS = 1;
    public static final int NO_NEW_EPISODES = 2;

    public static boolean testNetwork(Context context) {
        if(context.getApplicationContext() != null) {
            ConnectivityManager conMan = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = conMan.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                // We have a network connection
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

    public static void savePodcastToDb(Context context, Podcast podcast, boolean isNewFeed) {
        PodcastDataSource dataSource = new PodcastDataSource(context.getApplicationContext());
        dataSource.openDb();
        int podcastID;

        if(isNewFeed){
            podcastID = (int) dataSource.insertPodcast(podcast.getTitle(), podcast.getDescription(),
                podcast.getImageDirectory(), podcast.getLink());
        } else {
            podcastID = dataSource.getPodcastID(podcast.getTitle());
        }

        // If podcast inserted correctly now insert episodes too
        if (podcastID != -1) {
            ArrayList<Episode> listOfEpisodes = podcast.getEpisodeList();
            for (Episode episode : listOfEpisodes) {
                dataSource.insertEpisode(podcastID, episode.getTitle(), episode.getLink(),
                        episode.getDescription(), episode.getPubDate(), episode.getGuid(),
                        episode.getDuration(), episode.getEpisodeImage(), episode.getEnclosure());
            }
        } else {
            int duration = Toast.LENGTH_LONG;
            if (context.getApplicationContext() != null) {
                Toast.makeText(context.getApplicationContext(), "Already subscribed to podcast.", duration).show();
            }
        }
        dataSource.closeDb();
    }
}
