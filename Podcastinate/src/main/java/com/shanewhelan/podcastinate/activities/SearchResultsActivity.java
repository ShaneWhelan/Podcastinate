package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.SearchResult;
import com.shanewhelan.podcastinate.Utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class SearchResultsActivity extends Activity {
    private Bitmap[] bitmapList;
    private SearchResult[] resultArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        // Retrieve search terms from previous page
        Intent searchResultsIntent = getIntent();
        Parcelable[] parcelableArray = searchResultsIntent.getParcelableArrayExtra(Utilities.SEARCH_RESULT);


        if (parcelableArray != null) {
            resultArray = Arrays.copyOf(parcelableArray, parcelableArray.length, SearchResult[].class);

            bitmapList = new Bitmap[resultArray.length];

            ListView searchList = (ListView) findViewById(R.id.listOfSearchResults);
            SearchResultAdapter searchResultAdapter = new SearchResultAdapter(getApplicationContext(), R.layout.search_result_item, R.id.searchResultPodcastTitle ,resultArray);
            searchList.setAdapter(searchResultAdapter);
            if(resultArray.length > 1) {
                setTitle(resultArray.length + " Podcasts Found");
            } else {
                setTitle(resultArray.length + " Podcast Found");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(bitmapList != null) {
            for (Bitmap aBitmapList : bitmapList) {
                if(aBitmapList != null) {
                    aBitmapList.recycle();
                }
            }
        }
        System.gc();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(getApplicationContext(), SearchResult.class);
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

    public class SearchResultAdapter extends ArrayAdapter<SearchResult> implements View.OnClickListener{

        public SearchResultAdapter(Context context, int resource, int textViewResource, SearchResult[] objects) {
            super(context, resource, textViewResource, objects);
        }

        private void bindView(int position, View view) {
            SearchResult searchResult = super.getItem(position);
            if (searchResult != null) {
                // Load image into thumbnail slot asynchronously
                ImageView thumbNail = (ImageView) view.findViewById(R.id.searchResultImage);
                thumbNail.setContentDescription("" + position);
                thumbNail.setOnClickListener(this);
                // Check if bitmap is stored already
                if(bitmapList.length > position) {
                    if(bitmapList[position] == null) {
                        DownloadImageAsyncTask downloadImage = new DownloadImageAsyncTask(thumbNail, position);
                        if(searchResult.getLink() != null) {
                            downloadImage.execute(searchResult.getImageLink());
                        }
                    } else {
                        thumbNail.setImageBitmap(bitmapList[position]);
                    }
                } else {
                    DownloadImageAsyncTask downloadImage = new DownloadImageAsyncTask(thumbNail, position);
                    if(searchResult.getLink() != null) {
                        downloadImage.execute(searchResult.getImageLink());
                    }
                }

                // Update text view for this result
                TextView podcastTitle = (TextView) view.findViewById(R.id.searchResultPodcastTitle);
                podcastTitle.setOnClickListener(this);
                if(podcastTitle != null) {
                    podcastTitle.setText(searchResult.getTitle());
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
                subscribe.putExtra(Utilities.PODCAST_LINK, resultArray[position].getLink());
                startActivity(subscribe);
            }
        }
    }

    private class DownloadImageAsyncTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView podcastImage;
        private int position;

        public DownloadImageAsyncTask(ImageView podcastImage, int position) {
            this.podcastImage = podcastImage;
            this.position = position;
        }

        protected Bitmap doInBackground(String... urls) {
            Bitmap podcastBitmap = null;
            InputStream inStream = null;
            try {
                inStream = new java.net.URL(urls[0]).openStream();
                //Decode with inSampleSize
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = 2;
                podcastBitmap = BitmapFactory.decodeStream(inStream, null, o2);
            } catch (Exception e) {
                // Utilities.logException(e);
            } finally {
                if(inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        Utilities.logException(e);
                    }
                }
            }
            return podcastBitmap;
        }

        protected void onPostExecute(Bitmap podcastBitmap) {
            bitmapList[position] = podcastBitmap;
            // Multi threading check to see if the image view has changed position
            if(podcastImage.getContentDescription() != null) {
                if(podcastImage.getContentDescription().toString().equals("" + position)) {
                    podcastImage.setImageBitmap(podcastBitmap);
                    podcastImage.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
