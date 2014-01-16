package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.database.DatabaseHelper;
import com.shanewhelan.podcastinate.database.PodcastContract.PodcastEntry;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/*
TODO: Add Refresh button
TODO: Add picture beside podcast name
TODO: Add long press options (Maybe refresh individual feeds, mark done/new, add to playlist, sort options, force update of thumnail
TODO: Render ic_download properly


 */
public class MainActivity extends Activity {
    private PodcastDataSource dataSource;
    private SimpleCursorAdapter simpleCursorAdapter;
    private Cursor allPodcastNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) throws NullPointerException {
        super.onCreate(savedInstanceState);
        this.setTitle("Podcasts");
        setContentView(R.layout.activity_main);

        try {
            copyAppDbToDownloadFolder();
        } catch (IOException e) {
            e.printStackTrace();
        }

        dataSource = new PodcastDataSource(this);
        dataSource.openDb();
        allPodcastNames = dataSource.getAllPodcastNames();
        String[] fromColumns = new String[]{PodcastEntry.COLUMN_NAME_TITLE};
        int[] toViews = new int[]{R.id.podcastName};
        simpleCursorAdapter = new SimpleCursorAdapter(this, R.layout.podcast_list_item,
                allPodcastNames, fromColumns, toViews, 0);

        ListView listView = (ListView) findViewById(R.id.listOfPodcasts);
        listView.setAdapter(simpleCursorAdapter);
        dataSource.closeDb();

        OnItemClickListener itemCLickHandler = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = ((TextView) view.findViewById(R.id.podcastName));
                if (textView.getText() != null) {
                    viewEpisode(textView.getText().toString());
                }
            }
        };
        listView.setOnItemClickListener(itemCLickHandler);
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
                Intent intent = new Intent(this, SubscribeActivity.class);
                startActivityForResult(intent, 1);
                return true;
            case R.id.action_settings:

                return true;
            case R.id.action_wipe_db:
                wipeDb();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                updateListOfPodcasts();
            }
        }
    }

    public void updateListOfPodcasts() {
        dataSource.openDb();
        allPodcastNames = dataSource.getAllPodcastNames();
        simpleCursorAdapter.swapCursor(allPodcastNames);
        simpleCursorAdapter.notifyDataSetChanged();
        dataSource.closeDb();
    }

    public void wipeDb() {
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        databaseHelper.onUpgrade(databaseHelper.getWritableDatabase(), 1, 2);
    }

    public void viewEpisode(String podcastChosen) {
        Intent intent = new Intent(this, PodcastViewerActivity.class);
        intent.putExtra("userChoice", podcastChosen);
        startActivity(intent);
    }

    public void copyAppDbToDownloadFolder() throws IOException {
        File backupDB = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "backup.db"); // for example "my_data_backup.db"
        File currentDB = this.getDatabasePath("Podcastinate.db"); //databaseName=your current application database name, for example "my_data.db"
        if (currentDB.exists()) {
            FileChannel src = new FileInputStream(currentDB).getChannel();
            FileChannel dst = new FileOutputStream(backupDB).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            MediaScannerConnection.scanFile(this, new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/backup.db"}, null, null);
        }
    }
}
