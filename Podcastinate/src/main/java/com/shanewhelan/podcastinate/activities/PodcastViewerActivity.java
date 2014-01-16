package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.*;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.shanewhelan.podcastinate.EpisodeAdapter;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.database.PodcastContract.EpisodeEntry;
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
        /*
        int podcastID = dataSource.getPodcastID(podcast);

        Cursor listOfEpisodes = dataSource.getAllEpisodeNames(podcastID);


        String[] fromColumns = new String[]{EpisodeEntry.COLUMN_NAME_TITLE};
        int[] toViews = new int[]{R.id.episodeName};
        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this,
                R.layout.episode_list_item, listOfEpisodes, fromColumns, toViews, 0);

        ListView listView = (ListView) findViewById(R.id.listOfEpisodes);
        listView.setAdapter(simpleCursorAdapter);
        */
        int podcastID = dataSource.getPodcastID(podcast);

        Cursor episodesCursor = dataSource.getAllEpisodeNames(podcastID);

        EpisodeAdapter cursorAdapter = new EpisodeAdapter(this, episodesCursor, 2);

        ListView listView = (ListView) findViewById(R.id.listOfEpisodes);
        //cursorAdapter.bindView(listView, this, episodesCursor);
        listView.setAdapter(cursorAdapter);
        dataSource.closeDb();

        OnItemClickListener itemCLickHandler = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("sw9", "Download button " + position);
                TextView textView = (TextView) view.findViewById(R.id.episodeName);
                if (textView.getText() != null) {
                    Log.d("sw9", textView.getText().toString());
                }
            }
        };
        listView.setOnItemClickListener(itemCLickHandler);
/*
        ImageButton imageButton = (ImageButton) findViewById(R.id.download_icon);
        OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton currentButton = (ImageButton) findViewById(R.id.download_icon);
                Log.d("sw9", currentButton.getContentDescription().toString());
            }
        };
        imageButton.setOnClickListener(onClickListener);
        */
    }
}
