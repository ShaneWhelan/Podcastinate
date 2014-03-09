package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
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

import java.util.Arrays;

public class SearchResultsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        // Retrieve search terms from previous page
        Intent searchResultsIntent = getIntent();
        Parcelable[] parcelableArray = searchResultsIntent.getParcelableArrayExtra(Utilities.SEARCH_RESULT);

        SearchResult[] resultArray;
        if (parcelableArray != null) {
            resultArray = Arrays.copyOf(parcelableArray, parcelableArray.length, SearchResult[].class);
            Log.d("sw9", "Result Array length: " + resultArray.length);
            ListView searchList = (ListView) findViewById(R.id.listOfSearchResults);
            SearchResultAdapter searchResultAdapter = new SearchResultAdapter(getApplicationContext(), R.layout.search_result_item, R.id.searchResultPodcastTitle ,resultArray);
            searchList.setAdapter(searchResultAdapter);
        }
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
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class SearchResultAdapter extends ArrayAdapter<SearchResult> {
        private LayoutInflater layoutInflater;
        public Context context;

        public SearchResultAdapter(Context context, int resource, int textViewResource, SearchResult[] objects) {
            super(context, resource, textViewResource, objects);
            layoutInflater = LayoutInflater.from(context);
            this.context = context;
        }

        private void bindView(int position, View view) {
            SearchResult searchResult = super.getItem(position);
            if (searchResult != null) {
                ImageView thumbNail = (ImageView) view.findViewById(R.id.searchResultImage);

                TextView podcastTitle = (TextView) view.findViewById(R.id.searchResultPodcastTitle);
                if(podcastTitle != null) {
                    podcastTitle.setText(searchResult.getTitle());
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
    }
}
