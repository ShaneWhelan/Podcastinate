package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.shanewhelan.podcastinate.DuplicatePodcastException;
import com.shanewhelan.podcastinate.Episode;
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
import java.util.ArrayList;

/**
 * Created by Shane on 29/10/13. Podcastinate. Class to add a subscription.
 */
public class SubscribeActivity extends Activity {
    private TextView subscribeUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_activity);

        subscribeUrl = (TextView) findViewById(R.id.edit_text_feed_url);
        // Test Line
        subscribeUrl.setText("http://nerdist.libsyn.com/rss");

        final Button button = (Button) findViewById(R.id.button_subscribe);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (Utilities.testNetwork(getApplicationContext())) {
                    button.setClickable(false);
                    button.setVisibility(View.INVISIBLE);
                    subscribeToFeed();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.subscribe, menu);
        return true;
    }

    public void subscribeToFeed() {
        DownloadRSSFeed downFeed = new DownloadRSSFeed();
        if (subscribeUrl.getText() != null) {
            downFeed.execute(subscribeUrl.getText().toString());
        }
    }

    public void savePodcastToDb(Podcast podcast) {
        PodcastDataSource dataSource = new PodcastDataSource(this);
        dataSource.openDb();
        int podcastID = (int) dataSource.insertPodcast(podcast.getTitle(), podcast.getDescription(),
                podcast.getImageDirectory(), podcast.getLink());

        Log.d("sw9", "Podcast ID: " + podcastID);

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
            if (getApplicationContext() != null) {
                Toast.makeText(getApplicationContext(), "Already subscribed to podcast.", duration).show();
            }
        }
        dataSource.closeDb();
    }

    public String[] getPodcastLinks() {
        PodcastDataSource dataSource = new PodcastDataSource(this);
        dataSource.openDb();
        String[] listOfLinks = dataSource.getAllPodcastLinks();
        dataSource.closeDb();
        return listOfLinks;
    }

    public void successfulSubscription() {
        Intent intent = new Intent(this, MainActivity.class);
        setResult(RESULT_OK, intent);
        finish();
    }

    public class DownloadRSSFeed extends AsyncTask<String, Void, String> {
        private InputStream inputStream = null;

        @Override
        protected String doInBackground(String... urls) {
            try {
                int result = downloadRSSFeed(urls[0]);
                if (result == 1) {
                    return "subscribed";
                } else if (result == -1) {
                    return "URL Invalid";
                } else if (result == 0) {
                    return "Not Valid Podcast Feed";
                }
            } catch (DuplicatePodcastException e) {
                e.printStackTrace();
                return "Already subscribed to podcast";
            } catch (HTTPConnectionException httpException) {
                Log.e("sw9", "HTTP Response Error Number: " + httpException.getResponseCode() +
                        " caused by URL");
                return "Connection Error " + httpException.getResponseCode();
            } catch (IOException e) {
                Log.e("sw9", "Fail on ic_download RSS Feed, ERROR DUMP: " + e.getMessage() + " " + e.getClass());
                return "Exception: " + e.getClass();
            }
            return "Error";
        }

        @Override
        protected void onPostExecute(String subscribed) {
            Button button = (Button) findViewById(R.id.button_subscribe);
            button.setClickable(true);
            button.setVisibility(View.VISIBLE);

            int duration = Toast.LENGTH_LONG;
            if (subscribed.equals("subscribed")) {
                // Send out a toast displaying success
                // May be able to get this toast to the user faster
                if (getApplicationContext() != null) {
                    Toast.makeText(getApplicationContext(), "Subscribed", duration).show();
                }
                successfulSubscription();
            } else {
                if (getApplicationContext() != null) {
                    Toast.makeText(getApplicationContext(), subscribed, duration).show();
                }
            }
            // Implement the observer design pattern here to move to feeds page.

        }

        private int downloadRSSFeed(String url) throws DuplicatePodcastException, IOException {
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
                    String[] listOfLinks = getPodcastLinks();
                    Podcast podcast = parseRSS.parseRSSFeed(xmlPullParser, listOfLinks);

                    if (podcast != null) {
                        savePodcastToDb(podcast);
                        return 1;
                    } else {
                        // Won't be false unless parser threw exception
                        return 0;
                    }
                }

            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e("sw9", e.getMessage());
                    }
                }
            }
            return -1;
        }
    }
}

