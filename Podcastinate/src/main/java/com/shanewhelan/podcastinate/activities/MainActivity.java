package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.database.PodcastContract;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

public class MainActivity extends Activity {
    private PodcastDataSource dataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataSource = new PodcastDataSource(this);
        dataSource.openDb();
        Cursor allPodcastNames = dataSource.getAllPodcastNames();

        String[] fromColumns = new String[]{PodcastContract.PodcastEntry.COLUMN_NAME_TITLE};
        int[] toViews = new int[]{R.id.podcastName};
        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this, R.layout.podcast_list_item, allPodcastNames, fromColumns, toViews, 0);

        ListView listview = (ListView) findViewById(R.id.listOfPodcasts);
        listview.setAdapter(simpleCursorAdapter);
        dataSource.closeDb();
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.add_feed_action:
                Intent intent = new Intent(MainActivity.this, SubscribeActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_settings:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
