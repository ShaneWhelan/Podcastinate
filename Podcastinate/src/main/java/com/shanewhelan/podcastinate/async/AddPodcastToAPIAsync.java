package com.shanewhelan.podcastinate.async;

import android.os.AsyncTask;
import android.util.Log;

import com.shanewhelan.podcastinate.Podcast;
import com.shanewhelan.podcastinate.Utilities;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by Shane on 31/03/2014. Podcastinate.
 */
public class AddPodcastToAPIAsync extends AsyncTask<Podcast, Void, Void> {

    @Override
    protected Void doInBackground(Podcast... podcasts) {
        JSONObject jsonPodcast = new JSONObject();
        try {
            jsonPodcast.put("title", podcasts[0].getTitle());
            jsonPodcast.put("link", podcasts[0].getLink());
            jsonPodcast.put("imageLink", podcasts[0].getImageDirectory());
            Log.d("sw9", "Doing it!");
            HttpPost postPodcast = new HttpPost("http://ec2-54-186-15-6.us-west-2.compute.amazonaws.com/API/addPodcast");

            // Crucial to set the encoding to UTF-8 as that is the encoding of Play Framework
            StringEntity stringEntity = new StringEntity(jsonPodcast.toString(), "UTF-8");
            postPodcast.setEntity(stringEntity);
            postPodcast.setHeader("Content-type", "application/json");

            HttpClient httpClient = new DefaultHttpClient();
            httpClient.execute(postPodcast);
        } catch (JSONException e) {
            Utilities.logException(e);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
