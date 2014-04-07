package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
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
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.async.DownloadRSSFeed;
import com.shanewhelan.podcastinate.async.LoadImageFromDisk;
import com.shanewhelan.podcastinate.async.LoadImageFromDiskNonDecode;
import com.shanewhelan.podcastinate.async.RefreshRSSFeed;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastContract.PodcastEntry;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.services.AudioPlayerService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;

import static android.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER;

public class MainActivity extends Activity {
    private PodcastDataSource dataSource;
    private ProgressBar progressBar;
    private Cursor podcastCursor;
    private static PodcastAdapter podcastAdapter;
    private static ListView listView;
    private static GridView gridView;
    private ListView drawerListView;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    // Variables for the audio service
    private AudioPlayerService audioService;
    private ServiceConnection serviceConnection;
    private ImageButton cpPlayButton;
    private ImageButton cpPauseButton;
    private ImageView cpPodcastArt;
    private TextView cpPodcastTitle;
    private TextView cpEpisodeTitle;
    private RelativeLayout controlPanel;
    boolean isGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) throws NullPointerException {
        super.onCreate(savedInstanceState);
        setTitle("Podcasts");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        isGridView = sharedPreferences.getBoolean("grid_view_enabled", false);
        if(isGridView) {
            setContentView(R.layout.activity_main_grid_view);
        } else {
            setContentView(R.layout.activity_main);
        }
        // Sets the defaults up without overriding user settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

/*
        PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
        pds.openDbForWriting();
        pds.upgradeDB();
        pds.closeDb();
*/
        /*
        try {
            copyAppDbToDownloadFolder();
        } catch (IOException e) {
            Utilities.logException(e);
        }
        */

        initialiseDrawer();


        if(isGridView) {
            gridView = (GridView) findViewById(R.id.listOfPodcasts);
            initialiseAdapter();
        } else {
            listView = (ListView) findViewById(R.id.listOfPodcasts);
            initialiseAdapter();
        }
        initialiseSelectionListeners();
        initialiseControlPanel();

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
                } else if (incomingIntent.getAction().equals(Intent.ACTION_VIEW)) {
                    if(getIntent().getData() != null) {
                        // Received URL to subscribe to now process it
                        if(Utilities.testNetwork(getApplicationContext())) {
                            DownloadRSSFeed downloadRSSFeed = new DownloadRSSFeed(getApplicationContext(), progressBar);
                            downloadRSSFeed.execute(getIntent().getData().toString());
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
                Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(settingsIntent);
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
                    RefreshRSSFeed refreshFeed = new RefreshRSSFeed(getApplicationContext(), item, progressBar);
                    refreshFeed.execute();
                    return true;
                } else {
                    return false;
                }
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

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                AudioPlayerService.AudioPlayerBinder binder = (AudioPlayerService.AudioPlayerBinder) service;
                audioService = binder.getService();
                syncControlPanel();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                audioService = null;
            }
        };

        Intent intent = new Intent(this, AudioPlayerService.class);
        // 3 parameter is 0 because this means "bind if exists"
        bindService(intent, serviceConnection, 0);

        registerReceiver(mainActivityReceiver, new IntentFilter(Utilities.ACTION_UPDATE_LIST));

        registerReceiver(broadcastReceiver, new IntentFilter(Utilities.ACTION_PLAY));
        registerReceiver(broadcastReceiver, new IntentFilter(Utilities.ACTION_PAUSE));
        registerReceiver(broadcastReceiver, new IntentFilter(Utilities.ACTION_FINISHED));
        syncControlPanel();
        updateListOfPodcasts();
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(serviceConnection);
        unregisterReceiver(mainActivityReceiver);
        unregisterReceiver(broadcastReceiver);
    }

    public void initialiseAdapter() {
        dataSource = new PodcastDataSource(getApplicationContext());
        dataSource.openDbForReading();
        // Get Podcast ID so we can get all episode names from DB
        podcastCursor = dataSource.getPodcastInfoForAdapter();
        podcastAdapter = new PodcastAdapter(getApplicationContext(), podcastCursor,
                FLAG_REGISTER_CONTENT_OBSERVER);
        if(isGridView) {
            gridView.setAdapter(podcastAdapter);
        } else {
            listView.setAdapter(podcastAdapter);
        }
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

    public class MultiChoiceModeListenerGrid implements GridView.MultiChoiceModeListener {
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
        if(isGridView) {
            gridView.setOnItemClickListener(itemCLickHandler);
            gridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
            gridView.setMultiChoiceModeListener(new MultiChoiceModeListenerGrid());
        } else {
            listView.setOnItemClickListener(itemCLickHandler);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            listView.setMultiChoiceModeListener(new MultiChoiceModeListener());
        }
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

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utilities.ACTION_PLAY.equals(intent.getAction())) {
                cpPlayButton.setVisibility(View.GONE);
                cpPauseButton.setVisibility(View.VISIBLE);
                // This call fixes an issue where we are overriding the audio from third party app
                // and the control panel doesn't appear because the service is already up.
                syncControlPanel();
            } else if (Utilities.ACTION_PAUSE.equals(intent.getAction())) {
                cpPauseButton.setVisibility(View.GONE);
                cpPlayButton.setVisibility(View.VISIBLE);
                syncControlPanel();
            } else if (Utilities.ACTION_FINISHED.equals(intent.getAction())) {
                syncControlPanel();
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

    @SuppressWarnings("UnusedDeclaration")
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
        SparseBooleanArray booleanArray;
        if(isGridView) {
            booleanArray = gridView.getCheckedItemPositions();
        } else {
            booleanArray = listView.getCheckedItemPositions();
        }
        if (booleanArray != null) {
            // Open connection to DB
            PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
            pds.openDbForWriting();
            // Loop through the SparseBooleanArray and delete directory from db and file from disk
            for (int i = 0; i < booleanArray.size(); i++) {
                if (booleanArray.valueAt(i)) {

                    if(isGridView) {
                        cursor = (SQLiteCursor) gridView.getItemAtPosition(booleanArray.keyAt(i));
                    } else {
                        cursor = (SQLiteCursor) listView.getItemAtPosition(booleanArray.keyAt(i));
                    }

                    if (cursor != null) {
                        String podcastDirectory = cursor.getString(cursor.getColumnIndex(PodcastEntry.DIRECTORY));
                        int podcastID = cursor.getInt(cursor.getColumnIndex("_id"));
                        try {
                            File directoryToDelete = new File(podcastDirectory);
                            if(directoryToDelete.exists()) {
                                String deleteCmd = "rm -r " + podcastDirectory;
                                Runtime runtime = Runtime.getRuntime();
                                runtime.exec(deleteCmd);
                                pds.deletePodcast(podcastID);

                                if(audioService != null) {
                                    if(audioService.getEpisode() != null) {
                                        if(audioService.getEpisode().getPodcastID() == podcastID) {
                                            // Stop Service as the deleted podcast is also currently playing
                                            audioService.stopService();
                                            syncControlPanel();
                                        }
                                    }
                                }
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

            if(isGridView) {
                podcastImage.setOnClickListener(this);
                LoadImageFromDiskNonDecode loadImage = new LoadImageFromDiskNonDecode(podcastImage);
                loadImage.execute(cursor.getString(cursor.getColumnIndex(PodcastEntry.IMAGE_DIRECTORY)));
            } else {
                LoadImageFromDisk loadImage = new LoadImageFromDisk(podcastImage);
                loadImage.execute(cursor.getString(cursor.getColumnIndex(PodcastEntry.IMAGE_DIRECTORY)));
            }

            // Load in Podcast Titles
            TextView podcastTitleView = (TextView) view.findViewById(R.id.podcastName);
            podcastTitleView.setText(podcastTitle);
            podcastTitleView.setContentDescription(podcastID);

            TextView numNewEpisodesText = (TextView) view.findViewById(R.id.numberOfNewEpisodesTextView);
            if(numNewEpisodesText != null) {
                numNewEpisodesText.setText(Integer.toString(cursor.getInt(cursor.getColumnIndex(PodcastEntry.COUNT_NEW))));
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            if(isGridView){
                return layoutInflater.inflate(R.layout.podcast_item_grid_view, parent, false);
            } else {
                return layoutInflater.inflate(R.layout.podcast_list_item, parent, false);
            }
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
            if(view.getId() == R.id.podcastArtImage) {
                if(view.getContentDescription() != null) {
                    // Check if audio service has been initialised and is playing
                    int podcastId = Integer.parseInt(view.getContentDescription().toString());
                    PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
                    pds.openDbForReading();
                    String podcastTitle = pds.getPodcastTitle(podcastId);
                    pds.closeDb();
                    viewPodcast(podcastTitle, podcastId);
                }
            }
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

    private void initialiseControlPanel() {
        controlPanel = (RelativeLayout) findViewById(R.id.controlPanel);
        cpPlayButton = (ImageButton) findViewById(R.id.cpPlayButton);
        cpPauseButton = (ImageButton) findViewById(R.id.cpPauseButton);
        cpPodcastTitle = (TextView) findViewById(R.id.cpPodcastTitle);
        cpEpisodeTitle = (TextView) findViewById(R.id.cpEpisodeTitle);
        cpPodcastArt = (ImageView) findViewById(R.id.cpPodcastArt);

        cpPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if audio service has been initialised and is playing
                if (audioService != null) {
                    audioService.resumeMedia();
                }
            }
        });

        cpPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if audio service has been initialised and is playing
                // Pause podcast in background service
                if (audioService.getPlayer().isPlaying()) {
                    audioService.pauseMedia(false);
                }
            }
        });

        controlPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent playerIntent = new Intent(getApplicationContext(), PlayerActivity.class);
                startActivity(playerIntent);
            }
        });
    }

    public void syncControlPanel() {
        if (audioService != null) {
            if (audioService.getPlayer() != null) {
                controlPanel.setVisibility(View.VISIBLE);
                cpEpisodeTitle.setText(audioService.getEpisode().getTitle());
                cpPodcastTitle.setText(audioService.getPodcastTitle());
                cpPodcastArt.setImageBitmap(audioService.getPodcastBitmapLarge());
                if (audioService.getPlayer().isPlaying()) {
                    cpPlayButton.setVisibility(View.GONE);
                    cpPauseButton.setVisibility(View.VISIBLE);
                } else {
                    cpPauseButton.setVisibility(View.GONE);
                    cpPlayButton.setVisibility(View.VISIBLE);
                }
            } else {
                controlPanel.setVisibility(View.GONE);
            }
        } else {
            controlPanel.setVisibility(View.GONE);
        }
    }
}
