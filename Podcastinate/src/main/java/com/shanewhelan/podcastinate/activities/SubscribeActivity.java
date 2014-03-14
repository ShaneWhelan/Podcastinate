package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.SearchResult;
import com.shanewhelan.podcastinate.Utilities;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Shane on 29/10/13. Podcastinate. Class to add a subscription.
 */
public class SubscribeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_activity);



        final TextView searchText = (TextView) findViewById(R.id.edit_text_search_arguments);
        searchText.setHint("Search Podcasts");
        Button searchButton = (Button) findViewById(R.id.button_search);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (Utilities.testNetwork(getApplicationContext())) {
                    searchAPI(searchText);
                }
            }
        });

        final TextView subscribeUrl = (TextView) findViewById(R.id.edit_text_feed_url);
        // Test Line
        subscribeUrl.setText("http://www.tested.com/podcast-xml/this-is-only-a-test/");

        Button subscribeButton = (Button) findViewById(R.id.button_subscribe);
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (Utilities.testNetwork(getApplicationContext())) {
                    if (subscribeUrl.getText() != null) {
                        Intent subscribeIntent = new Intent(getApplicationContext(), MainActivity.class);
                        subscribeIntent.setAction(Utilities.ACTION_SUBSCRIBE);
                        subscribeIntent.putExtra(Utilities.PODCAST_LINK, subscribeUrl.getText().toString());
                        startActivity(subscribeIntent);
                    }
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

    public void searchAPI(TextView searchText) {
        if(searchText.getText() != null) {
            // Query Podcast API for available podcasts and display them in a list.
            QueryPodcastAPI queryPodcastAPI = new QueryPodcastAPI();
            queryPodcastAPI.execute(searchText.getText().toString());
        }
    }

    public class QueryPodcastAPI extends AsyncTask<String, Void, SearchResult[]> {
        @Override
        protected SearchResult[] doInBackground(String... urls) {
            String apiURL = "http://ec2-54-186-15-6.us-west-2.compute.amazonaws.com/API/search?podcastTitle=" + urls[0].replace(" ", "+");
            InputStream jsonInputStream = null;
            try {
                HttpGet httpGet = new HttpGet(new URI(apiURL));
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse httpResponse = httpClient.execute(httpGet);
                jsonInputStream = httpResponse.getEntity().getContent();

                // Convert InputSteam to String and then store the JSon result in an Array
                JSONObject jsonResult = new JSONObject(Utilities.convertInputStreamToStringV2(jsonInputStream));
                JSONArray resultArray = jsonResult.getJSONArray("result");
                SearchResult[] searchResults = new SearchResult[resultArray.length()];
                for(int i = 0; i < resultArray.length(); i++) {
                    JSONObject currentJsonNode = resultArray.getJSONObject(i);
                    SearchResult currentSearchNode = new SearchResult();
                    currentSearchNode.setTitle(currentJsonNode.getString("title"));
                    currentSearchNode.setImageLink(currentJsonNode.getString("imageLink"));
                    currentSearchNode.setLink(currentJsonNode.getString("link"));
                    currentSearchNode.setDescription(currentJsonNode.getString("description"));
                    searchResults[i] = currentSearchNode;
                }
                return searchResults;
            } catch (URISyntaxException e) {
                Utilities.logException(e);
            } catch (ClientProtocolException e) {
                Utilities.logException(e);
            } catch (IOException e) {
                Utilities.logException(e);
            } catch (JSONException e) {
                Utilities.logException(e);
            } finally {
                if (jsonInputStream != null) {
                    try {
                        jsonInputStream.close();
                    } catch (IOException e) {
                        Utilities.logException(e);
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(SearchResult[] resultsArray) {
            Intent searchResultsIntent = new Intent(getApplicationContext(), SearchResultsActivity.class);
            searchResultsIntent.putExtra(Utilities.SEARCH_RESULT, resultsArray);
            startActivity(searchResultsIntent);
        }
    }
}

