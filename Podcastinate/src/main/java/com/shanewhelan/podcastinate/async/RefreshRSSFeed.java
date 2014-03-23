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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RefreshRSSFeed extends AsyncTask<HashMap<String, String>, Void, HashMap<String, String>> {
    public Context context;
    private MenuItem refreshAction;
    private ProgressBar progressBar;

    public RefreshRSSFeed(Context context, MenuItem refreshAction, ProgressBar progressBar) {
        this.context = context;
        this.refreshAction = refreshAction;
        this.progressBar = progressBar;
    }

    @Override
    protected void onPreExecute() {
        // Set Refresh icon to progress bar
        refreshAction.setActionView(R.layout.actionbar_indeterminate_progress);
        refreshAction.expandActionView();

        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected HashMap<String, String> doInBackground(HashMap<String, String>... urlList) {
        HashMap<String, String> resultMap = new HashMap<String, String>(urlList[0].size());
        try {
            Set entrySet = urlList[0].entrySet();
            int result;
            for (Object anEntrySet : entrySet) {
                Map.Entry mapEntry = (Map.Entry) anEntrySet;
                result = refreshRSSFeed(mapEntry.getValue().toString(),
                        Integer.parseInt(mapEntry.getKey().toString()));
                resultMap.put(mapEntry.getValue().toString(), String.valueOf(result));
            }
            return resultMap;
        } catch (HTTPConnectionException e) {
            Utilities.logException(e);
            resultMap.put("error", "Connection Error " + e.getResponseCode());
            return resultMap;
        } catch (IOException e) {
            Utilities.logException(e);
            resultMap.put("error", "Fail on ic_download RSS Feed, ERROR DUMP: " + e.getMessage() + " " + e.getClass());
            return resultMap;
        }
    }

    @Override
    protected void onPostExecute(HashMap<String, String> resultMap) {
        progressBar.setVisibility(View.GONE);

        // Replace the progress bar with the refresh button again
        refreshAction.collapseActionView();
        refreshAction.setActionView(null);

        Set entrySet = resultMap.entrySet();
        int numNewEpisodes = 0;
        String error = null;
        for (Object anEntrySet : entrySet) {
            Map.Entry mapEntry = (Map.Entry) anEntrySet;
            if (mapEntry.getValue() == String.valueOf(Utilities.SUCCESS)) {
                numNewEpisodes++;
            } else if (mapEntry.getKey().toString().equals("error")) {
                error = mapEntry.getValue().toString();
            }
        }

        if (context.getApplicationContext() != null) {
            if (numNewEpisodes > 1) {
                Toast.makeText(context.getApplicationContext(), numNewEpisodes + " new episodes.", Toast.LENGTH_LONG).show();
            } else if (numNewEpisodes == 1) {
                Toast.makeText(context.getApplicationContext(), numNewEpisodes + " new episode.", Toast.LENGTH_LONG).show();
            } else if (numNewEpisodes == 0) {
                Toast.makeText(context.getApplicationContext(), "No new episodes", Toast.LENGTH_LONG).show();
            } else if (error != null) {
                Toast.makeText(context.getApplicationContext(), error, Toast.LENGTH_LONG).show();
            }
        }

        context.sendBroadcast(new Intent(Utilities.ACTION_UPDATE_LIST));
    }

    private int refreshRSSFeed(String url, int podcastID) throws IOException {
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