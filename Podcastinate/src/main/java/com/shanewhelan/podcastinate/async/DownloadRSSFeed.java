package com.shanewhelan.podcastinate.async;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.shanewhelan.podcastinate.DuplicatePodcastException;
import com.shanewhelan.podcastinate.ParseRSS;
import com.shanewhelan.podcastinate.Podcast;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.exceptions.HTTPConnectionException;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadRSSFeed extends AsyncTask<String, Void, String> {
    private Context context;
    private ProgressBar progressBar;

    public DownloadRSSFeed(Context context, ProgressBar progressBar) {
        this.context = context;
        this.progressBar = progressBar;
    }

    @Override
    protected void onPreExecute() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            int result = downloadRSSFeed(urls[0]);
            if (result == Utilities.SUCCESS) {
                return "subscribed";
            } else if (result == Utilities.INVALID_URL) {
                return "URL Invalid";
            } else if (result == Utilities.FAILURE_TO_PARSE) {
                return "Not Valid Podcast Feed";
            }
        } catch (DuplicatePodcastException e) {
            return "Already subscribed to podcast";
        } catch (HTTPConnectionException e) {
            Utilities.logException(e);
            return "Connection Error " + e.getResponseCode();
        } catch (IOException e) {
            Utilities.logException(e);
            return "Exception: " + e.getClass();
        }
        return "Error";
    }

    @Override
    protected void onPostExecute(String subscribed) {
        progressBar.setVisibility(View.GONE);

        int duration = Toast.LENGTH_LONG;
        if (subscribed.equals("subscribed")) {
            // Send out a toast displaying success
            // May be able to get this toast to the user faster
            if (context.getApplicationContext() != null) {
                Toast.makeText(context.getApplicationContext(), "Subscribed", duration).show();
            }
            context.sendBroadcast(new Intent(Utilities.ACTION_UPDATE_LIST));
        } else {
            if (context.getApplicationContext() != null) {
                Toast.makeText(context.getApplicationContext(), subscribed, duration).show();
            }
        }
    }

    private int downloadRSSFeed(String url) throws DuplicatePodcastException, IOException {
        // Check for existing podcast
        String[] listOfLinks = getPodcastLinks();
        if (!isLinkUnique(listOfLinks, url)) {
            throw new DuplicatePodcastException("Podcast Already in Database");
        }

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

                Podcast podcast = parseRSS.parseRSSFeed(xmlPullParser, url);

                if(podcast.getEpisodeList().size() == 1) {
                    podcast.setCountNew(1);
                } else if(podcast.getEpisodeList().size() > 1){
                    podcast.setCountNew(2);
                }

                if (podcast != null) {
                    Utilities.savePodcastToDb(context.getApplicationContext(), podcast, "", true);
                    return Utilities.SUCCESS;
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

    public String[] getPodcastLinks() {
        PodcastDataSource dataSource = new PodcastDataSource(context.getApplicationContext());
        dataSource.openDbForReading();
        String[] listOfLinks = dataSource.getAllPodcastLinks();
        dataSource.closeDb();
        return listOfLinks;
    }

    public boolean isLinkUnique(String[] listOfLinks, String link) {
        boolean linkUnique = true;
        for (String currentLink : listOfLinks) {
            if (link.equals(currentLink)) {
                linkUnique = false;
            }
        }
        return linkUnique;
    }
}