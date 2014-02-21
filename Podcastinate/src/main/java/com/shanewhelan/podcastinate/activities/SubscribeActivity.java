package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.shanewhelan.podcastinate.DuplicatePodcastException;
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

/**
 * Created by Shane on 29/10/13. Podcastinate. Class to add a subscription.
 */
public class SubscribeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_activity);

        final TextView subscribeUrl = (TextView) findViewById(R.id.edit_text_feed_url);
        // Test Line
        subscribeUrl.setText("http://www.tested.com/podcast-xml/this-is-only-a-test/");

        final Button button = (Button) findViewById(R.id.button_subscribe);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (Utilities.testNetwork(getApplicationContext())) {
                    button.setClickable(false);
                    button.setVisibility(View.INVISIBLE);
                    subscribeToFeed(subscribeUrl);
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

    public void subscribeToFeed(TextView subscribeUrl) {
        DownloadRSSFeed downFeed = new DownloadRSSFeed();
        if (subscribeUrl.getText() != null) {
            downFeed.execute(subscribeUrl.getText().toString());
        }
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
                Utilities.logException(e);
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
                    String[] listOfLinks = getPodcastLinks();
                    Podcast podcast = parseRSS.parseRSSFeed(xmlPullParser, listOfLinks);

                    if (podcast != null) {
                        Utilities.savePodcastToDb(getApplicationContext(), podcast, true);
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
    }
}

