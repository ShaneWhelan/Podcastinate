package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.shanewhelan.podcastinate.EpisodeAdapter;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

import static android.widget.CursorAdapter.*;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public class PodcastViewerActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episode_list);
        Intent intent = getIntent();
        String podcastName = intent.getStringExtra("userChoice");

        PodcastDataSource dataSource = new PodcastDataSource(this);

        dataSource.openDb();

        int podcastID = dataSource.getPodcastID(podcastName);
        Cursor episodesCursor = dataSource.getAllEpisodeNames(podcastID);
        EpisodeAdapter cursorAdapter = new EpisodeAdapter(this, episodesCursor,
                FLAG_REGISTER_CONTENT_OBSERVER);
        cursorAdapter.setEpisodeName(podcastName);
        ListView listView = (ListView) findViewById(R.id.listOfEpisodes);
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
    }
}
