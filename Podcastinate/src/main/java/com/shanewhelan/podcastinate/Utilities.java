package com.shanewhelan.podcastinate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.services.AudioPlayerService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Shane on 04/02/14. Podcastinate.
 */
public class Utilities {
    public static final String ACTION_PLAY = "com.shanewhelan.podcastinate.PLAY";
    public static final String ACTION_PAUSE = "com.shanewhelan.podcastinate.PAUSE";
    public static final String ACTION_DOWNLOADED = "com.shanewhelan.podcastinate.DOWNLOADED";
    public static final String ACTION_FINISHED = "com.shanewhelan.podcastinate.FINISHED";
    public static final String PODCAST_TITLE = "e_title";
    public static final String EPISODE_TITLE = "p_title";
    public static final String ENCLOSURE = "enclosure";
    public static final String DIRECTORY = "/Podcastinate";
    public static final int INVALID_URL = -1;
    public static final int FAILURE_TO_PARSE = 0;
    public static final int SUCCESS = 1;
    public static final int NO_NEW_EPISODES = 2;
    public static final String SEARCH_RESULT = "result";
    public static final String ACTION_SUBSCRIBE = "com.shanewhelan.podcastinate.SUBSCRIBE";
    public static final String PODCAST_LINK = "link";
    public static String PODCAST_ID = "id";

    public Utilities() {

    }

    public static boolean testNetwork(Context context) {
        if (context.getApplicationContext() != null) {
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
        dataSource.openDbForWriting();
        int podcastID;

        if (isNewFeed) {
            podcastID = (int) dataSource.insertPodcast(podcast.getTitle(), podcast.getDescription(),
                    podcast.getImageDirectory(), podcast.getDirectory(), podcast.getLink());
        } else {
            podcastID = dataSource.getPodcastIDWithLink(podcast.getLink());
        }

        // If podcast inserted correctly now insert episodes too
        if (podcastID != -1) {
            ArrayList<Episode> listOfEpisodes = podcast.getEpisodeList();
            for (Episode episode : listOfEpisodes) {
                dataSource.insertEpisode(podcastID, episode.getTitle(), episode.getDescription(),
                        episode.getPubDate(), episode.getGuid(), episode.getDuration(),
                        episode.getEnclosure());
            }
        } else {
            int duration = Toast.LENGTH_LONG;
            if (context.getApplicationContext() != null) {
                Toast.makeText(context.getApplicationContext(), "Already subscribed to podcast.", duration).show();
            }
        }
        dataSource.closeDb();
    }

    public static class DisconnectHeadphonesReceiver extends BroadcastReceiver {
        public DisconnectHeadphonesReceiver() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                    Intent pauseIntent = new Intent(context, AudioPlayerService.class);
                    pauseIntent.setAction(AudioPlayerService.ACTION_DISCONNECT);
                    context.startService(pauseIntent);
                }
            }
        }
    }

    public static void logException(Exception e) {
        Log.e(e.getClass().getName(), e.getMessage(), e);
    }

    /*
    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public static String convertInputStreamToString(InputStream inputStream, long contentLength) {
        // Broken method
        try {
            Reader reader = new InputStreamReader(inputStream, "UTF-8");
            char[] buffer = new char[Utilities.safeLongToInt(contentLength)];
            reader.read(buffer);
            return new String(buffer);
        } catch (UnsupportedEncodingException e) {
            Utilities.logException(e);
        } catch (IOException e) {
            Utilities.logException(e);
        }
        return "";
    }*/

    public static String convertInputStreamToStringV2(InputStream inputStream) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder(inputStream.available());
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            return total.toString();
        } catch (IOException e) {
            logException(e);
        }
        return "";
    }
}
