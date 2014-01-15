package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.database.PodcastContract.*;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public class PodcastViewerActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episode_list);
        Intent intent = getIntent();
        String podcast = intent.getStringExtra("userChoice");

        PodcastDataSource dataSource = new PodcastDataSource(this);
        dataSource.openDb();
        int podcastID = dataSource.getPodcastID(podcast);

        Cursor listOfEpisodes = dataSource.getAllEpisodeNames(podcastID);


        String[] fromColumns = new String[]{EpisodeEntry.COLUMN_NAME_TITLE};
        int[] toViews = new int[]{R.id.episodeName};
        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this,
                R.layout.episode_list_item, listOfEpisodes, fromColumns, toViews, 0);

        ListView listView = (ListView) findViewById(R.id.listOfEpisodes);
        listView.setAdapter(simpleCursorAdapter);
        dataSource.closeDb();

        OnItemClickListener itemCLickHandler = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //viewEpisode( ((TextView) view.findViewById(R.id.podcastName)).getText().toString());
            }
        };

        listView.setOnItemClickListener(itemCLickHandler);
    }
}
