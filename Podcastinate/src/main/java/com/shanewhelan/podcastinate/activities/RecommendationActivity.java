package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.SearchResult;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RecommendationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);
        QueryRecommendationAPI queryAPI = new QueryRecommendationAPI();
        queryAPI.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.recommendation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class QueryRecommendationAPI extends AsyncTask<String, Void, SearchResult[]> {
        @Override
        protected SearchResult[] doInBackground(String... urls) {
            PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
            pds.openDbForReading();
            JSONArray titlesJson = pds.getAllPodcastTitles();
            pds.closeDb();
            if(titlesJson != null) {
                try {
                    JSONObject jsonToSend = new JSONObject();
                    jsonToSend.put("recommend", titlesJson);

                    InputStream jsonInputStream;
                    HttpPost getRecommendations = new HttpPost("http://ec2-54-186-15-6.us-west-2.compute.amazonaws.com/API/recommend");

                    // Crucial to set the encoding to UTF-8 as that is the encoding of Play Framework
                    StringEntity se = new StringEntity(jsonToSend.toString(), "UTF-8");
                    getRecommendations.setEntity(se);

                    getRecommendations.setHeader("Content-type", "application/json");

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpResponse httpResponse = httpClient.execute(getRecommendations);

                    jsonInputStream = httpResponse.getEntity().getContent();

                    // Convert InputSteam to String and then store the JSon result in an Array
                    JSONObject jsonResult = new JSONObject(Utilities.convertInputStreamToStringV2(jsonInputStream));
                    JSONArray resultArray = jsonResult.getJSONArray("result");
                    SearchResult[] searchResults = new SearchResult[resultArray.length()];
                    for(int i = 0; i < resultArray.length(); i++) {
                        JSONObject currentJsonNode = resultArray.getJSONObject(i);
                        SearchResult currentSearchNode = new SearchResult();
                        currentSearchNode.setTitle(currentJsonNode.getString("podcastTitle"));
                        currentSearchNode.setImageLink(currentJsonNode.getString("imageLink"));
                        currentSearchNode.setLink(currentJsonNode.getString("link"));

                        /*
                        TODO Sort out this bug
                            JSONArray genreArray = currentJsonNode.getJSONArray("genres");
                            List<String> genresToSave = new ArrayList<String>(genreArray.length());
                            for(int j = 0; j < genreArray.length(); j++) {
                                genresToSave.add(genreArray.getString(j));
                            }

                            currentSearchNode.setGenres(genresToSave);
                        */
                        searchResults[i] = currentSearchNode;
                    }
                    return searchResults;
                } catch (JSONException e) {
                    Utilities.logException(e);
                } catch (ClientProtocolException e) {
                    Utilities.logException(e);
                } catch (IOException e) {
                    Utilities.logException(e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(SearchResult[] resultsArray) {
            for (SearchResult aResultsArray : resultsArray) {
                Log.d("sw9", "Result array " + aResultsArray.getTitle());
            }
        }
    }

}
