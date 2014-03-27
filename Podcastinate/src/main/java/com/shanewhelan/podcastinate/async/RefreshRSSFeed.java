package com.shanewhelan.podcastinate.async;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.shanewhelan.podcastinate.ParseRSS;
import com.shanewhelan.podcastinate.Podcast;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.exceptions.HTTPConnectionException;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RefreshRSSFeed extends AsyncTask<HashMap<String, String>, Void, String> {
    public Context context;
    private MenuItem refreshAction;
    private ProgressBar progressBar;
    private int newEpisodeCount;

    public RefreshRSSFeed(Context context, MenuItem refreshAction, ProgressBar progressBar) {
        this.context = context;
        this.refreshAction = refreshAction;
        this.progressBar = progressBar;
        newEpisodeCount = 0;
    }

    @Override
    protected void onPreExecute() {
        // Set Refresh icon to progress bar
        refreshAction.setActionView(R.layout.actionbar_indeterminate_progress);
        refreshAction.expandActionView();

        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected String doInBackground(HashMap<String, String>... urlList) {
        try {
            Set entrySet = urlList[0].entrySet();
            for (Object anEntrySet : entrySet) {
                Map.Entry mapEntry = (Map.Entry) anEntrySet;
                refreshRSSFeed(mapEntry.getValue().toString(),
                        Integer.parseInt(mapEntry.getKey().toString()));
            }
            return Integer.toString(Utilities.SUCCESS);
        } catch (HTTPConnectionException e) {
            Utilities.logException(e);
            return "Connection Error " + e.getResponseCode();
        } catch (MalformedURLException e) {
            Utilities.logException(e);
            return "URL is malformed";
        } catch (IOException e) {
            Utilities.logException(e);
            return "Connection Error";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        progressBar.setVisibility(View.GONE);

        // Replace the progress bar with the refresh button again
        refreshAction.collapseActionView();
        refreshAction.setActionView(null);

        if (context.getApplicationContext() != null) {
            if (newEpisodeCount > 1) {
                Toast.makeText(context.getApplicationContext(), newEpisodeCount + " new episodes", Toast.LENGTH_LONG).show();
            } else if (newEpisodeCount == 1) {
                Toast.makeText(context.getApplicationContext(), newEpisodeCount + " new episode", Toast.LENGTH_LONG).show();
            } else if (newEpisodeCount == 0) {
                Toast.makeText(context.getApplicationContext(), "No new episodes", Toast.LENGTH_LONG).show();
            } else if (result != null) {
                Toast.makeText(context.getApplicationContext(), result, Toast.LENGTH_LONG).show();
            }
        }

        context.sendBroadcast(new Intent(Utilities.ACTION_UPDATE_LIST));
    }

    @SuppressWarnings("DuplicateThrows")
    private int refreshRSSFeed(String url, int podcastID) throws IOException, HTTPConnectionException, MalformedURLException {
        InputStream inputStream = null;
        int response;
        try {
            URL feedURL = new URL(url);
            HttpURLConnection httpCon = (HttpURLConnection) feedURL.openConnection();
            httpCon.setReadTimeout(100000);
            httpCon.setConnectTimeout(150000);
            httpCon.setRequestMethod("GET");
            httpCon.setUseCaches(true);
            httpCon.addRequestProperty("Content-Type", "text/xml; charset=utf-8");
            httpCon.setDoInput(true);
            httpCon.connect();
            response = httpCon.getResponseCode();

            if (response == 200) {
                inputStream = httpCon.getInputStream();
            } else {
                throw new HTTPConnectionException(response);
            }

            ParseRSS parseRSS = new ParseRSS();
            XmlPullParser xmlPullParser = parseRSS.inputStreamToPullParser(inputStream);
            if (xmlPullParser != null) {
                PodcastDataSource pds = new PodcastDataSource(context.getApplicationContext());
                pds.openDbForReading();
                String enclosure = pds.getMostRecentEpisodeEnclosure(podcastID);
                String podcastTitle = pds.getPodcastTitle(podcastID);
                int currentCountNew = pds.getCountNew(podcastID);
                pds.closeDb();
                Podcast podcast = parseRSS.checkForNewEntries(xmlPullParser, enclosure, podcastTitle);
                // Number of new episodes for the notification
                newEpisodeCount = newEpisodeCount + podcast.getCountNew();
                podcast.setCountNew(currentCountNew + podcast.getCountNew());

                if (podcast != null) {
                    if (podcast.getEpisodeList().size() > 0) {
                        Utilities.savePodcastToDb(context.getApplicationContext(), podcast, url, false);
                        return Utilities.SUCCESS;
                    } else {
                        return Utilities.NO_NEW_EPISODES;
                    }
                } else {
                    // Won't be false unless parser threw exception, causing podcast to be null
                    return Utilities.FAILURE_TO_PARSE;
                }
            }

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Utilities.logException(e);
                }
            }
        }
        return Utilities.INVALID_URL;
    }
}