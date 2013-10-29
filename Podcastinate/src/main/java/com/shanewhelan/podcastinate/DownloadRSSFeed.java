package com.shanewhelan.podcastinate;

import android.os.AsyncTask;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public class DownloadRSSFeed extends AsyncTask<String, Void, InputStream> {
    private InputStream inputStream = null;

    @Override
    protected InputStream doInBackground(String... urls) {
        try {
            downloadRSSFeed(urls[0]);
            return getInStream();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(InputStream inputStream){

    }

    private void downloadRSSFeed(String url) throws IOException {
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

            if(response == 200){
                inputStream = httpCon.getInputStream();
            }else{
                throw new HTTPConnectionException(response);
            }
            ParseRSS parseRss = new ParseRSS(inputStream);
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
