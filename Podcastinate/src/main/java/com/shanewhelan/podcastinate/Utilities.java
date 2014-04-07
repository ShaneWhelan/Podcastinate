package com.shanewhelan.podcastinate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.shanewhelan.podcastinate.activities.DownloadActivity;
import com.shanewhelan.podcastinate.async.RefreshRSSFeed;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.services.AudioPlayerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Utilities {
    public static final String ACTION_NEW_EPISODE = "com.shanewhelan.podcastinate.NEW_EPISODE";
    public static final String ACTION_PLAY = "com.shanewhelan.podcastinate.PLAY";
    public static final String ACTION_PAUSE = "com.shanewhelan.podcastinate.PAUSE";
    public static final String ACTION_DOWNLOADED = "com.shanewhelan.podcastinate.DOWNLOADED";
    public static final String ACTION_FINISHED = "com.shanewhelan.podcastinate.FINISHED";
    public static final String ACTION_UPDATE_LIST = "update_list";
    public static final String ACTION_SUBSCRIBE = "com.shanewhelan.podcastinate.SUBSCRIBE";
    // Actions for notification
    public static final String ACTION_PLAY_NOTIFY = "com.shanewhelan.podcastinate.PLAY_N";
    public static final String ACTION_PAUSE_NOTIFY = "com.shanewhelan.podcastinate.PAUSE_N";
    public static final String ACTION_SKIP_BACK_NOTIFY = "com.shanewhelan.podcastinate.SKIP_BACK_NOTIFY";
    public static final String ACTION_SKIP_FORWARD_NOTIFY = "com.shanewhelan.podcastinate.SKIP_FORWARD_NOTIFY";

    public static final String PODCAST_TITLE = "e_title";
    public static final String DIRECTORY = "/Podcastinate";
    public static final int INVALID_URL = -1;
    public static final int FAILURE_TO_PARSE = 0;
    public static final int SUCCESS = 1;
    public static final int NO_NEW_EPISODES = 2;
    public static final String SEARCH_RESULT = "result";
    public static final String PODCAST_LINK = "link";
    public static final String ACTION_DOWNLOAD = "com.shanewhelan.podcastinate.DOWNLOAD";
    public static final String ACTION_CANCEL = "com.shanewhelan.podcastinate.CANCEL";
    public static final String ACTION_CANCEL_COMPLETE = "com.shanewhelan.podcastinate.CANCEL_FIN";
    public static final String ACTION_QUEUED = "com.shanewhelan.podcastinate.QUEUED";
    public static final String ACTION_CAR_MODE_ON = "com.shanewhelan.podcastinate.CM_ON";
    public static final String ACTION_CAR_MODE_OFF = "com.shanewhelan.podcastinate.CM_OFF";
    public static final String ACTION_REFRESH = "com.shanewhelan.podcastinate.REFRESH_FEEDS";
    public static String PODCAST_ID = "id";
    public static String VIEW_PODCAST = "view";
    public static String EPISODE_ID = "episode_id";

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
            } else //noinspection ConstantConditions
                if (!networkInfo.isConnected()) {
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

    public static void savePodcastToDb(Context context, Podcast podcast, String url, boolean isNewFeed) {
        PodcastDataSource pds = new PodcastDataSource(context.getApplicationContext());
        pds.openDbForWriting();
        int podcastID;

        if (isNewFeed) {
            podcastID = (int) pds.insertPodcast(podcast.getTitle(), podcast.getDescription(),
                    podcast.getImageDirectory(), podcast.getDirectory(), podcast.getLink(), podcast.getCountNew());
        } else {
            podcastID = pds.getPodcastIDWithLink(url);
            pds.updatePodcastCountNew(podcastID, podcast.getCountNew());
        }

        // If podcast inserted correctly now insert episodes too
        if (podcastID != -1) {
            ArrayList<Episode> listOfEpisodes = podcast.getEpisodeList();
            for (Episode episode : listOfEpisodes) {
                pds.insertEpisode(podcastID, episode.getTitle(), episode.getDescription(),
                        episode.getPubDate(), episode.getGuid(), episode.getDuration(),
                        episode.getEnclosure(), episode.isNew());

            }
        } else {
            int duration = Toast.LENGTH_LONG;
            if (context.getApplicationContext() != null) {
                Toast.makeText(context.getApplicationContext(), "Already subscribed to podcast.", duration).show();
            }
        }
        pds.closeDb();
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
                } else if(intent.getAction().equals(ACTION_PAUSE)) {
                    Intent pauseIntent = new Intent(context, AudioPlayerService.class);
                    pauseIntent.setAction(ACTION_PAUSE);
                    context.startService(pauseIntent);
                }
            }
        }
    }

    public static class RefreshFeeds extends BroadcastReceiver {

        public RefreshFeeds() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("sw9", "Refreshing feeds");
            if (intent.getAction() != null) {
                if(intent.getAction().equals(ACTION_REFRESH)) {
                    RefreshRSSFeed refreshRSSFeed = new RefreshRSSFeed(context, null, null);
                    refreshRSSFeed.execute();
                }
            }
        }
    }

    public static class NotificationClicked extends BroadcastReceiver {
        public NotificationClicked() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent notificationIntent = new Intent(context, DownloadActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(notificationIntent);
        }
    }

    public static void logException(Exception e) {
        Log.e(e.getClass().getName(), e.getMessage(), e);
    }

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

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public static Bitmap decodeFile(File f, int IMAGE_MAX_SIZE){
        Bitmap b = null;

        //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        FileInputStream fis;
        try {
            fis = new FileInputStream(f);
            BitmapFactory.decodeStream(fis, null, o);
            fis.close();

            int scale = 1;
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                scale = (int)Math.pow(2, (int) Math.ceil(Math.log(IMAGE_MAX_SIZE /
                        (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            fis = new FileInputStream(f);
            b = BitmapFactory.decodeStream(fis, null, o2);
            fis.close();
        } catch (FileNotFoundException e) {
            Utilities.logException(e);
        } catch (IOException e) {
            Utilities.logException(e);
        }
        return b;
    }


    @SuppressWarnings("UnusedDeclaration")
    public static Bitmap decodeStream(InputStream inputStream, int IMAGE_MAX_SIZE){
        Bitmap b;

        //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;


        BitmapFactory.decodeStream(inputStream, null, o);

        int scale = 1;
        if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
            scale = (int)Math.pow(2, (int) Math.ceil(Math.log(IMAGE_MAX_SIZE /
                    (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
        }

        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        b = BitmapFactory.decodeStream(inputStream, null, o);

        return b;
    }
}
