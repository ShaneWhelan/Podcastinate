package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.*;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.async.DownloadRSSFeed;
import com.shanewhelan.podcastinate.async.LoadImageFromDisk;
import com.shanewhelan.podcastinate.async.RefreshRSSFeed;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastContract.PodcastEntry;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;

import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;

import static android.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER;

/*
High Priority FEATURES:
TODO: Create Download Queue (Cancel, pause and start downloads)
TODO: Add long press options (Maybe refresh individual feeds, add to playlist, sort options, force update of thumnail)
TODO: Set back button to go to right activities

MAJOR FEATURES:
TODO: Integrate other APIs - EMAIL FEEDLY
TODO: Cloud backup
TODO: Car Mode
TODO: User Suggested Podcasts
TODO: User Settings - refresh interval
TODO: Statistics of user playback

Low Priority FEATURES:
TODO: Check Rotation on all feeds
TODO: Finalise Control Panel Design and put on every page (Podcast beside epsisode name)
TODO: Confirmation dialog box on subscribe
TODO: Lock screen widget
TODO: Sleep Timer
TODO: Mark new on resume podcast if manually made new

BUGS:
TODO: Download service: Sometimes fails to download and jumps straight to download complete notification
TODO: E/MediaPlayer﹕ Attempt to call getDuration without a valid mediaplayer when playing a new podcast overriding an old one
TODO: CNET ALL podcasts feed is broken
TODO: Restrict access to the player from the drawer
TODO: Fix for one feed a week/removing old feeds
TODO: On subscribe pictures dont load

TODO: Delete a subscription while player is playing
TODO: Demo refresh with player activity
TODO: DELETE while ANYTHING, or downloading

Server Bugs:
TODO: The server side gives back the same podcasts already subscribed to - limit searches and results

LOW PRIORITY:
TODO: Streaming: Must keep WIFI from sleeping
TODO: Help Section
TODO: Add Paging to podcast viewing activity

Test Case:
TODO: If you have no subscriptions and you look for recommendations
*/

public class MainActivity extends Activity {
    private PodcastDataSource dataSource;
    private ProgressBar progressBar;
    private Cursor podcastCursor;
    private static PodcastAdapter podcastAdapter;
    private static ListView listView;
    private ListView drawerListView;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) throws NullPointerException {
        super.onCreate(savedInstanceState);
        setTitle("Podcasts");
        setContentView(R.layout.activity_main);
/*
        PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
        pds.openDbForWriting();
        pds.upgradeDB();
        pds.closeDb();
*/
        // TODO: Dev only, take out for release
        try {
            copyAppDbToDownloadFolder();
        } catch (IOException e) {
            Utilities.logException(e);
        }

        initialiseDrawer();

        listView = (ListView) findViewById(R.id.listOfPodcasts);
        initialiseAdapter();
        initialiseSelectionListeners();

        progressBar = (ProgressBar) findViewById(R.id.smoothProgressBar);
        progressBar.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(getApplicationContext()).interpolator(new AccelerateInterpolator()).build());

        Intent incomingIntent = getIntent();
        if(incomingIntent != null) {
            if(incomingIntent.getAction() != null){
                if(incomingIntent.getAction().equals(Utilities.ACTION_SUBSCRIBE)) {
                    // Received URL to subscribe to now process it
                    if(Utilities.testNetwork(getApplicationContext())) {
                        DownloadRSSFeed downloadRSSFeed = new DownloadRSSFeed(getApplicationContext(), progressBar);
                        downloadRSSFeed.execute(incomingIntent.getStringExtra(Utilities.PODCAST_LINK));
                    }
                } else {
                    if (getIntent().getAction() != null) {
                        if(Utilities.testNetwork(getApplicationContext())) {
                            if (getIntent().getAction().equals(Intent.ACTION_VIEW)) {
                                if(getIntent().getData() != null) {
                                    DownloadRSSFeed downloadRSSFeed = new DownloadRSSFeed(getApplicationContext(), progressBar);
                                    downloadRSSFeed.execute(getIntent().getData().toString());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Update drawer icon now that we have created everything
        actionBarDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.add_feed_action:
                Intent intent = new Intent(getApplicationContext(), SubscribeActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_settings:
                return true;
            case R.id.action_remove_2_podcasts:
                PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
                pds.openDbForWriting();
                pds.removeTwoEpisodesFromEach();
                pds.closeDb();
                updateListOfPodcasts();
                return true;
            case R.id.action_refresh:
                if (Utilities.testNetwork(getApplicationContext())) {
                    PodcastDataSource dataSource = new PodcastDataSource(getApplicationContext());
                    dataSource.openDbForReading();
                    HashMap<String, String> podcastInfo = dataSource.getAllPodcastIDsLinks();
                    dataSource.closeDb();
                    RefreshRSSFeed refreshFeed = new RefreshRSSFeed(getApplicationContext(), item, progressBar);
                    if (podcastInfo != null) {
                        //noinspection unchecked
                        refreshFeed.execute(podcastInfo);
                    }
                    return true;
                } else {
                    return false;
                }
            case R.id.action_wipe_db:
                wipeDb();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mainActivityReceiver, new IntentFilter(Utilities.ACTION_UPDATE_LIST));
        updateListOfPodcasts();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mainActivityReceiver);
    }

    public void initialiseAdapter() {
        dataSource = new PodcastDataSource(getApplicationContext());
        dataSource.openDbForReading();
        // Get Podcast ID so we can get all episode names from DB
        podcastCursor = dataSource.getPodcastInfoForAdapter();
        podcastAdapter = new PodcastAdapter(getApplicationContext(), podcastCursor,
                FLAG_REGISTER_CONTENT_OBSERVER);
        listView.setAdapter(podcastAdapter);
        dataSource.closeDb();
    }

    public class MultiChoiceModeListener implements ListView.MultiChoiceModeListener {
        private int nr = 0;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            // Here you can do something when items are selected/de-selected,
            // such as update the title in the CAB
            if (checked) {
                nr++;
                podcastAdapter.setNewSelection(position, true);
            } else {
                nr--;
                podcastAdapter.removeSelection(position);
            }
            mode.setTitle(nr + " Selected");
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB
            MenuInflater inflater = mode.getMenuInflater();
            if (inflater != null) {
                inflater.inflate(R.menu.multi_select, menu);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Here you can perform updates to the CAB due to
            // an invalidate() request
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Respond to clicks on the actions in the CAB
            switch (item.getItemId()) {
                case R.id.delete_selection_action:
                    deleteSelectedItems();
                    updateListOfPodcasts();
                    nr = 0;
                    podcastAdapter.clearSelection();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Here you can make any necessary updates to the activity when
            // the CAB is removed. By default, selected items are deselected/unchecked.
            podcastAdapter.clearSelection();
            nr = 0;
        }
    }

    private void initialiseDrawer() {
        // Retrieve items from strings.xml - names of the drawer selections
        String[] drawerLisViewArray = getResources().getStringArray(R.array.nav_drawer_titles);

        drawerListView = (ListView) findViewById(R.id.left_drawer);

        // Initialise adapter for the drawer
        drawerListView.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_listview_item,
                drawerLisViewArray));


        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        // Set drawer icon
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer,
                R.string.openDrawerText, R.string.closeDrawerText);

        // Set actionBarDrawerToggle as the DrawerListener
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        if(getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // just styling option add shadow the right edge of the drawer
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        drawerListView.setOnItemClickListener(new DrawerListViewClickListener());
    }

    public void initialiseSelectionListeners() {
        OnItemClickListener itemCLickHandler = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = (TextView) view.findViewById(R.id.podcastName);
                if (textView.getText() != null) {
                    if(textView.getContentDescription() != null) {
                        viewPodcast(textView.getText().toString(), Integer.parseInt(textView.getContentDescription().toString()));
                    }
                }
            }
        };
        listView.setOnItemClickListener(itemCLickHandler);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new MultiChoiceModeListener());
    }

    public void updateListOfPodcasts() {
        dataSource.openDbForReading();
        podcastCursor = dataSource.getPodcastInfoForAdapter();
        podcastAdapter.swapCursor(podcastCursor);
        podcastAdapter.notifyDataSetChanged();
        dataSource.closeDb();
    }

    BroadcastReceiver mainActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utilities.ACTION_UPDATE_LIST.equals(intent.getAction())) {
                updateListOfPodcasts();
            }
        }
    };

    public void viewPodcast(String podcastTitle, int podcastID) {
        Intent intent = new Intent(this, PodcastViewerActivity.class);
        intent.setAction(Utilities.VIEW_PODCAST);
        intent.putExtra(Utilities.PODCAST_ID, podcastID);
        intent.putExtra(Utilities.PODCAST_TITLE, podcastTitle);
        startActivity(intent);
    }

    // Following two methods are debug only methods
    public void wipeDb() {
        // Only to be left in developer version - wipes db and external storage directory
        PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
        pds.openDbForWriting();
        pds.upgradeDB();
        pds.closeDb();
        Log.d("sw9", Environment.getExternalStorageDirectory().getAbsolutePath() + "/Podcastinate");
        File externalDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Podcastinate");

        if (externalDir.exists()) {
            String deleteCmd = "rm -r " + externalDir.getAbsolutePath();
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
                Log.d("sw9", "Directory deleted");
            } catch (IOException e) {
                Utilities.logException(e);
            }
        }
    }

    public void copyAppDbToDownloadFolder() throws IOException {
        File backupDB = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "backup.db"); // for example "my_data_backup.db"
        File currentDB = this.getDatabasePath("Podcastinate.db"); //databaseName=your current application database name, for example "my_data.db"
        if(currentDB.exists()) {
            FileChannel source = new FileInputStream(currentDB).getChannel();
            FileChannel destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            MediaScannerConnection.scanFile(this, new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/backup.db"}, null, null);
        }
    }

    private void deleteSelectedItems() {
        SQLiteCursor cursor;
        SparseBooleanArray booleanArray = listView.getCheckedItemPositions();
        if (booleanArray != null) {
            // Open connection to DB
            PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
            pds.openDbForWriting();
            // Loop through the SparseBooleanArray and delete directory from db and file from disk
            for (int i = 0; i < booleanArray.size(); i++) {
                if (booleanArray.valueAt(i)) {
                    cursor = (SQLiteCursor) listView.getItemAtPosition(booleanArray.keyAt(i));
                    if (cursor != null) {
                        String podcastDirectory = cursor.getString(cursor.getColumnIndex(PodcastEntry.DIRECTORY));
                        try {
                            File directoryToDelete = new File(podcastDirectory);
                            if(directoryToDelete.exists()) {
                                String deleteCmd = "rm -r " + podcastDirectory;
                                Runtime runtime = Runtime.getRuntime();
                                runtime.exec(deleteCmd);
                                pds.deletePodcast(cursor.getInt(cursor.getColumnIndex("_id")));
                                // TODO Control Panel integration
                                /*
                                if(audioService != null) {
                                    if(audioService.getEpisode() != null) {
                                        if(audioService.getEpisode().getEnclosure().equals(enclosure)) {
                                            // Stop Service as the deleted podcast is also currently playing
                                            audioService.stopService();
                                            syncControlPanel();
                                        }
                                    }
                                }
                                */
                            }

                        } catch(Exception e) {
                            Utilities.logException(e);
                        }
                    }
                }
            }
            pds.closeDb();
        }
    }

    public class PodcastAdapter extends CursorAdapter implements View.OnClickListener {
        private final LayoutInflater layoutInflater;
        private SparseBooleanArray sparseBArray = new SparseBooleanArray();

        public PodcastAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Get Title and ID for identification purposes
            String podcastTitle = cursor.getString(cursor.getColumnIndex(PodcastEntry.TITLE));
            String podcastID = Integer.toString(cursor.getInt(cursor.getColumnIndex("_id")));

            // Load images in background thread
            ImageButton podcastImage = (ImageButton) view.findViewById(R.id.podcastArtImage);
            podcastImage.setContentDescription(podcastID);

            LoadImageFromDisk loadImage = new LoadImageFromDisk(podcastImage);
            loadImage.execute(cursor.getString(cursor.getColumnIndex(PodcastEntry.IMAGE_DIRECTORY)));

            // Load in Podcast Titles
            TextView podcastTitleView = (TextView) view.findViewById(R.id.podcastName);
            podcastTitleView.setText(podcastTitle);
            podcastTitleView.setContentDescription(podcastID);

            TextView numNewEpisodesText = (TextView) view.findViewById(R.id.numberOfNewEpisodesTextView);
            numNewEpisodesText.setText(Integer.toString(cursor.getInt(cursor.getColumnIndex(PodcastEntry.COUNT_NEW))));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return layoutInflater.inflate(R.layout.podcast_list_item, parent, false);
        }

        public void setNewSelection(int position, boolean value) {
            sparseBArray.put(position, value);
            notifyDataSetChanged();
        }

        public void removeSelection(int position) {
            sparseBArray.delete(position);
            notifyDataSetChanged();
        }

        public void clearSelection() {
            sparseBArray = new SparseBooleanArray();
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Adapter handles setting up rows
            View v = super.getView(position, convertView, parent);
            if (v != null) {
                v.setBackgroundColor(getResources().getColor(android.R.color.background_light));
                if (sparseBArray.get(position)) {
                    v.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                }
            }
            return v;
        }

        @Override
        public void onClick(View view) {
            // Leave empty because whole row is clickable
        }
    }

    private class DrawerListViewClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            @SuppressWarnings("ConstantConditions")
            String selectedText = (((TextView)view).getText()).toString();
            if(selectedText.equals("Podcasts")){
                drawerLayout.closeDrawer(drawerListView);
            } else if(selectedText.equals("Find Similar")) {
                Intent recommendIntent = new Intent(getApplicationContext(), RecommendationActivity.class);
                startActivity(recommendIntent);
            } else if(selectedText.equals("Subscribe")) {
                Intent intent = new Intent(getApplicationContext(), SubscribeActivity.class);
                startActivity(intent);
            } else if(selectedText.equals("Player")) {
                Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
                startActivity(intent);
            }
            drawerLayout.closeDrawer(drawerListView);
        }
    }
}
