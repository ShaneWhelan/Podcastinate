package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.shanewhelan.podcastinate.async.DownloadImagesAsyncTask;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.RecommendResult;
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

import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;

public class RecommendationActivity extends Activity {
    private RecommendResult[] recommendResults;
    private Bitmap[] bitmapList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Loading...");
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
                Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class QueryRecommendationAPI extends AsyncTask<String, Void, RecommendResult[]> {
        private ProgressBar progressBar;

        @Override
        protected void onPreExecute() {
            progressBar = (ProgressBar) findViewById(R.id.recommendationProgressBar);
            progressBar.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(getApplicationContext()).interpolator(new AccelerateInterpolator()).build());

            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected RecommendResult[] doInBackground(String... urls) {
            PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
            pds.openDbForReading();
            JSONArray titlesJson = pds.getAllPodcastLinksJson();
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
                    recommendResults = new RecommendResult[resultArray.length()];
                    for(int i = 0; i < resultArray.length(); i++) {
                        JSONObject currentJsonNode = resultArray.getJSONObject(i);
                        RecommendResult currentRecommendNode = new RecommendResult();
                        currentRecommendNode.setTitle(currentJsonNode.getString("podcastTitle"));
                        currentRecommendNode.setImageLink(currentJsonNode.getString("imageLink"));
                        currentRecommendNode.setLink(currentJsonNode.getString("link"));
                        // Now add genres to object
                        JSONArray genreArray = currentJsonNode.getJSONArray("genres");
                        List<String> genresToSave = new ArrayList<String>(genreArray.length());
                        for(int j = 0; j < genreArray.length(); j++) {
                            genresToSave.add(genreArray.getString(j));
                        }
                        currentRecommendNode.setGenres(genresToSave);

                        recommendResults[i] = currentRecommendNode;
                    }
                    return recommendResults;
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
        protected void onPostExecute(RecommendResult[] resultsArray) {
            progressBar.setVisibility(View.GONE);

            if(resultsArray != null) {
                bitmapList = new Bitmap[resultsArray.length];

                ListView recommendationList = (ListView) findViewById(R.id.listOfRecommendations);
                RecommendationAdapter searchResultAdapter = new RecommendationAdapter(getApplicationContext(), R.layout.recommendation_item, R.id.recommendationPodcastTitle, resultsArray);
                recommendationList.setAdapter(searchResultAdapter);
                if(resultsArray.length > 1) {
                    setTitle(resultsArray.length + " Podcasts Found");
                } else {
                    setTitle(resultsArray.length + " Podcast Found");
                }

            }
        }
    }

    public class RecommendationAdapter extends ArrayAdapter<RecommendResult> implements View.OnClickListener{

        public RecommendationAdapter(Context context, int resource, int textViewResource, RecommendResult[] objects) {
            super(context, resource, textViewResource, objects);
        }

        private void bindView(int position, View view) {
            RecommendResult recommendResult = super.getItem(position);
            if (recommendResult != null) {
                // Load image into thumbnail slot asynchronously
                ImageView thumbNail = (ImageView) view.findViewById(R.id.recommendationImage);
                thumbNail.setContentDescription("" + position);
                thumbNail.setOnClickListener(this);
                // Check if bitmap is stored already
                if(bitmapList.length > position) {
                    if(bitmapList[position] == null) {
                        DownloadImagesAsyncTask downloadImage = new DownloadImagesAsyncTask(thumbNail, position, bitmapList);
                        downloadImage.execute(recommendResult.getImageLink());
                    } else {
                        thumbNail.setImageBitmap(bitmapList[position]);
                    }
                } else {
                    DownloadImagesAsyncTask downloadImage = new DownloadImagesAsyncTask(thumbNail, position, bitmapList);
                    downloadImage.execute(recommendResult.getImageLink());
                }

                // Update text view for this result
                TextView podcastTitle = (TextView) view.findViewById(R.id.recommendationPodcastTitle);
                podcastTitle.setOnClickListener(this);
                if(podcastTitle != null) {
                    podcastTitle.setText(recommendResult.getTitle());
                    podcastTitle.setContentDescription("" + position);
                }
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //convertView = layoutInflater.inflate(R.layout.activity_search_results, parent, false);
            // Adapter handles setting up rows
            View view = super.getView(position, convertView, parent);
            bindView(position, view);
            if (view != null) {
                view.setBackgroundColor(getResources().getColor(android.R.color.background_light));
            }
            return view;
        }

        @Override
        public void onClick(View v) {
            if(v.getContentDescription() != null){
                int position = Integer.parseInt(v.getContentDescription().toString());
                // Start New Subscribe Activity
                Intent subscribe = new Intent(getApplicationContext(), MainActivity.class);
                subscribe.setAction(Utilities.ACTION_SUBSCRIBE);
                subscribe.putExtra(Utilities.PODCAST_LINK, recommendResults[position].getLink());
                startActivity(subscribe);
            }
        }
    }

}
