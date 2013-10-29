package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import com.shanewhelan.podcastinate.R;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.shanewhelan.podcastinate.*;
import com.shanewhelan.podcastinate.database.DatabaseHelper;
import com.shanewhelan.podcastinate.database.PodcastContract.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Shane on 29/10/13. Podcastinate. Class to add a subscription.
 */
public class SubscribeActivity extends Activity {
    private TextView subscribeUrl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_activity);

        subscribeUrl = (TextView) findViewById(R.string.feedToSubscribeTo);
        subscribeUrl.setText("http://nerdist.libsyn.com/rss");

        final Button button = (Button) findViewById(R.string.testButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if(testNetwork()){
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

    public boolean testNetwork() {
        ConnectivityManager conMan = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conMan.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else if(networkInfo == null) {
            // Alert the user that network connection methods are off
            Log.i("sw9", "WI-FI or Mobile Data turned off");
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, "WI-FI or Mobile Data turned off", duration);
            toast.show();
            return false;
        } else if(networkInfo.isConnected() == false) {
            // Alert the user that network is not available.
            Log.i("sw9", "Connected but no internet access");
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, "Connected but no internet access", duration);
            toast.show();
            return false;
        }

        return false;
    }

    public void subscribeToFeed() {
        DownloadRSSFeed downFeed = new DownloadRSSFeed();
        downFeed.execute(subscribeUrl.getText().toString());
    }

    public void savePodcastToDb(Podcast podcast){
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.insertPodcast(podcast.getTitle(), podcast.getDescription(),
                podcast.getImageDirectory(), podcast.getLink());
    }

    public class DownloadRSSFeed extends AsyncTask<String, Void, Podcast> {
        private InputStream inputStream = null;

        @Override
        protected Podcast doInBackground(String... urls) {
            try {
                return downloadRSSFeed(urls[0]);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Podcast podcast){
            // Send out a toast of completion
            Context context = getApplicationContext();
            CharSequence text = "Subscribed to " + podcast.getTitle();
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            savePodcastToDb(podcast);
            // Implement the observer design pattern here to move to feeds page.
        }

        private Podcast downloadRSSFeed(String url) throws IOException {
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

                if(response == 200) {
                    inputStream = httpCon.getInputStream();
                }else{
                    throw new HTTPConnectionException(response);
                }
                ParseRSS parseRSS = new ParseRSS(inputStream);
                return parseRSS.getPodcast();
            } catch(HTTPConnectionException httpException) {
                Log.d("sw9", "HTTP Response Error Number: " + httpException.getResponseCode() +
                        " caused by URL:" + url);
            } catch (IOException e) {
                Log.d("sw9", "Fail on Download RSS Feed, ERROR DUMP: " + e.getLocalizedMessage());
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            return null;
        }

        public InputStream getInStream() {
            return inputStream;
        }

        class HTTPConnectionException extends IOException {
            private int responseCode;

            public HTTPConnectionException(int responseCode) {
                super();
                this.responseCode = responseCode;
            }

            public int getResponseCode() {
                return responseCode;
            }
        }
    }

}

