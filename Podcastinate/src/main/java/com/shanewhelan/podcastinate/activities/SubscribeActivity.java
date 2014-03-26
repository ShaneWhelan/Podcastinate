package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;

public class SubscribeActivity extends Activity {
    private TextView subscribeUrl;
    private TextView searchText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);

        searchText = (TextView) findViewById(R.id.edit_text_search_arguments);
        subscribeUrl = (TextView) findViewById(R.id.edit_text_feed_url);

        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(EditorInfo.IME_ACTION_SEARCH == actionId){
                    if(Utilities.testNetwork(getApplicationContext())) {
                        searchAPI(searchText);
                    }
                }
                return false;
            }
        });

        subscribeUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(EditorInfo.IME_ACTION_GO == actionId) {
                    if(Utilities.testNetwork(getApplicationContext())) {
                        if(subscribeUrl.getText() != null) {
                            if(subscribeUrl.getText().length() > 0) {
                                Intent subscribeIntent = new Intent(getApplicationContext(), MainActivity.class);
                                subscribeIntent.setAction(Utilities.ACTION_SUBSCRIBE);
                                subscribeIntent.putExtra(Utilities.PODCAST_LINK, subscribeUrl.getText().toString());
                                startActivity(subscribeIntent);
                            }
                        }
                    }
                }
                return false;
            }
        });

        Button searchButton = (Button) findViewById(R.id.button_search);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if(Utilities.testNetwork(getApplicationContext())) {
                    if(searchText.getText() != null) {
                        if(searchText.getText().length() > 0) {
                            searchAPI(searchText);
                        }
                    }
                }
            }
        });

        Button subscribeButton = (Button) findViewById(R.id.button_subscribe);
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if(Utilities.testNetwork(getApplicationContext())) {
                    if(subscribeUrl.getText() != null) {
                        if(subscribeUrl.getText().length() > 0) {
                            Intent subscribeIntent = new Intent(getApplicationContext(), MainActivity.class);
                            subscribeIntent.setAction(Utilities.ACTION_SUBSCRIBE);
                            subscribeIntent.putExtra(Utilities.PODCAST_LINK, subscribeUrl.getText().toString());
                            startActivity(subscribeIntent);
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(getApplicationContext(), MainActivity.class);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                                    // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.subscribe, menu);
        return true;
    }

    public void searchAPI(TextView searchText) {
        if(searchText.getText() != null) {
            if(searchText.getText().length() > 0) {
                // Query Podcast API for available podcasts and display them in a list.
                QueryPodcastAPI queryPodcastAPI = new QueryPodcastAPI();
                queryPodcastAPI.execute(searchText.getText().toString());
            }
        }
    }

    public class QueryPodcastAPI extends AsyncTask<String, Void, SearchResult[]> {
        private ProgressBar progressBar;

        @Override
        protected void onPreExecute() {
            progressBar = (ProgressBar) findViewById(R.id.searchProgressBar);
            progressBar.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(getApplicationContext()).interpolator(new AccelerateInterpolator()).build());
            progressBar.setVisibility(View.VISIBLE);
        }

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

                // Check for no results first before parsing
                if(jsonResult.getInt("resultCount") > 0) {
                    // We have at least one result so parse the JSON
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
                }
                return null;
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
            progressBar.setVisibility(View.GONE);

            if(resultsArray != null) {
                Intent searchResultsIntent = new Intent(getApplicationContext(), SearchResultsActivity.class);
                searchResultsIntent.putExtra(Utilities.SEARCH_RESULT, resultsArray);
                startActivity(searchResultsIntent);
            } else {
                if (getApplicationContext() != null) {
                    Toast.makeText(getApplicationContext(), "No Results", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}

