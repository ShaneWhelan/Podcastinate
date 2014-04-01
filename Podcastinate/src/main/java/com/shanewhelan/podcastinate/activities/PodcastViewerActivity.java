package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastContract.EpisodeEntry;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.services.AudioPlayerService;

import java.io.File;

import static android.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER;

public class PodcastViewerActivity extends Activity {
    private static EpisodeAdapter episodeAdapter;
    private static PodcastDataSource dataSource;
    private Cursor episodeCursor;
    private int podcastID;
    private ListView listView;
    private String podcastTitle;


    // Variables for audio service
    private AudioPlayerService audioService;
    private ServiceConnection serviceConnection;
    private ImageButton cpPlayButton;
    private ImageButton cpPauseButton;
    private ImageView cpPodcastArt;
    private TextView cpPodcastTitle;
    private TextView cpEpisodeTitle;
    private RelativeLayout controlPanel;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utilities.ACTION_PLAY.equals(intent.getAction())) {
                cpPlayButton.setVisibility(View.GONE);
                cpPauseButton.setVisibility(View.VISIBLE);
                updateListOfPodcasts();

                // This call fixes an issue where we are overriding the audio from third party app
                // and the control panel doesn't appear because the service is already up.
                syncControlPanel();
            } else if (Utilities.ACTION_PAUSE.equals(intent.getAction())) {
                cpPauseButton.setVisibility(View.GONE);
                cpPlayButton.setVisibility(View.VISIBLE);
                updateListOfPodcasts();
                syncControlPanel();
            } else if (Utilities.ACTION_DOWNLOADED.equals(intent.getAction())) {
                updateListOfPodcasts();
            } else if (Utilities.ACTION_FINISHED.equals(intent.getAction())) {
                // Verify this is the right thing to do
                updateListOfPodcasts();
                syncControlPanel();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        podcastTitle = intent.getStringExtra(Utilities.PODCAST_TITLE);
        podcastID = intent.getIntExtra(Utilities.PODCAST_ID, -1);

        // Set title of current activity to Podcast Name
        setTitle(podcastTitle);
        setContentView(R.layout.activity_podcast_viewer);

        listView = (ListView) findViewById(R.id.listOfEpisodes);
        initialiseAdapter();
        initialiseMultiSelect();
        initialiseControlPanel();

        OnItemClickListener itemCLickHandler = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = (TextView) view.findViewById(R.id.episodeName);
                if(textView.getText() != null) {
                    Log.d("sw9", textView.getText().toString());
                }
            }
        };
        listView.setOnItemClickListener(itemCLickHandler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.podcast_viewer, menu);
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
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void initialiseAdapter() {
        dataSource = new PodcastDataSource(getApplicationContext());
        dataSource.openDbForReading();
        // Get Podcast ID so we can get all episode names from DB
        episodeCursor = dataSource.getAllEpisodeInfoForAdapter(podcastID);
        episodeAdapter = new EpisodeAdapter(getApplicationContext(), episodeCursor,
                FLAG_REGISTER_CONTENT_OBSERVER);
        listView.setAdapter(episodeAdapter);
        dataSource.closeDb();
    }

    public void initialiseMultiSelect() {
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

            private int nr = 0;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position,
                                                  long id, boolean checked) {
                // Here you can do something when items are selected/de-selected,
                // such as update the title in the CAB
                if (checked) {
                    nr++;
                    episodeAdapter.setNewSelection(position, true);
                } else {
                    nr--;
                    episodeAdapter.removeSelection(position);
                }
                mode.setTitle(nr + " Selected");
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                // Respond to clicks on the actions in the CAB
                switch (item.getItemId()) {
                    case R.id.delete_selection_action:
                        deleteSelectedItems();
                        updateListOfPodcasts();
                        nr = 0;
                        episodeAdapter.clearSelection();
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.mark_as_played:
                        setSelectedItemsNewState(0);
                        updateListOfPodcasts();
                        nr = 0;
                        episodeAdapter.clearSelection();
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.mark_as_new:
                        setSelectedItemsNewState(1);
                        updateListOfPodcasts();
                        nr = 0;
                        episodeAdapter.clearSelection();
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Inflate the menu for the CAB
                MenuInflater inflater = mode.getMenuInflater();
                if (inflater != null) {
                    inflater.inflate(R.menu.multi_select_episode, menu);
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // Here you can make any necessary updates to the activity when
                // the CAB is removed. By default, selected items are deselected/unchecked.
                episodeAdapter.clearSelection();
                nr = 0;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // Here you can perform updates to the CAB due to
                // an invalidate() request
                return false;
            }
        });
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
                        String enclosure = cursor.getString(cursor.getColumnIndex(EpisodeEntry.ENCLOSURE));
                        try {
                            File fileToDelete = new File(cursor.getString(cursor.getColumnIndex(EpisodeEntry.DIRECTORY)));
                            boolean isFileDeleted = fileToDelete.delete();
                            if (isFileDeleted) {
                                int episodeId = cursor.getInt(cursor.getColumnIndex("_id"));
                                pds.updateEpisodeDirectory(episodeId, null);
                                pds.updateCurrentTime(episodeId, 0);
                                if(audioService != null) {
                                    if(audioService.getEpisode() != null) {
                                        if(audioService.getEpisode().getEnclosure().equals(enclosure)) {
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
                        cursor.close();
                    }
                }
            }
            pds.closeDb();
        }
    }

    private void setSelectedItemsNewState(int newState) {
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
                        int episode_id = cursor.getInt(cursor.getColumnIndex("_id"));
                        int isEpisodeNew = cursor.getInt(cursor.getColumnIndex(EpisodeEntry.NEW_EPISODE));
                        try {
                            // Check if user erroneously change a new episode to the new state
                            if (isEpisodeNew != newState) {
                                // User made the correct choice now make sure that we don't go below
                                // 0 new episodes in the database
                                int currentCountNew = pds.getCountNew(podcastID);
                                if (newState == 0) {
                                    if (currentCountNew > 0) {
                                        pds.updatePodcastCountNew(podcastID, currentCountNew - 1);
                                    }
                                } else {
                                    pds.updatePodcastCountNew(podcastID, currentCountNew + 1);
                                }
                                pds.updateEpisodeIsNew(episode_id, newState);
                            }
                        } catch(Exception e) {
                            Utilities.logException(e);
                        }
                        cursor.close();
                    }
                }
            }
            pds.closeDb();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(serviceConnection);
        unregisterReceiver(broadcastReceiver);
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
        //TODO is this a bug I wonder?
        intent.setAction(Utilities.ACTION_NEW_EPISODE);
        // 3 parameter is 0 because this means "bind if exists"
        bindService(intent, serviceConnection, 0);

        registerReceiver(broadcastReceiver, new IntentFilter(Utilities.ACTION_PLAY));
        registerReceiver(broadcastReceiver, new IntentFilter(Utilities.ACTION_PAUSE));
        registerReceiver(broadcastReceiver, new IntentFilter(Utilities.ACTION_DOWNLOADED));
        registerReceiver(broadcastReceiver, new IntentFilter(Utilities.ACTION_FINISHED));

        syncControlPanel();
        updateListOfPodcasts();
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

    public void updateListOfPodcasts() {
        dataSource.openDbForReading();
        episodeCursor = dataSource.getAllEpisodeInfoForAdapter(podcastID);
        episodeAdapter.swapCursor(episodeCursor);
        episodeAdapter.notifyDataSetChanged();
        dataSource.closeDb();
    }

    public class EpisodeAdapter extends CursorAdapter implements View.OnClickListener {
        private final LayoutInflater layoutInflater;
        private SparseBooleanArray sparseBArray = new SparseBooleanArray();

        public EpisodeAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Set background colour of whole row
            if(cursor.getInt(cursor.getColumnIndex(EpisodeEntry.NEW_EPISODE)) == 1 ) {
                view.setBackgroundColor(getResources().getColor(R.color.background_color_new));
            } else {
                view.setBackgroundColor(getResources().getColor(R.color.background_color_old));
            }

            String episodeTitle = cursor.getString(cursor.getColumnIndex(EpisodeEntry.TITLE));
            String episodeID = Integer.toString(cursor.getInt(cursor.getColumnIndex("_id")));

            TextView episodeNameView = (TextView) view.findViewById(R.id.episodeName);
            episodeNameView.setText(episodeTitle);

            ImageButton downloadButton = (ImageButton) view.findViewById(R.id.download_icon);
            ImageButton playButton = (ImageButton) view.findViewById(R.id.play_icon);
            ImageButton pauseButton = (ImageButton) view.findViewById(R.id.pause_icon);

            downloadButton.setContentDescription(episodeID);
            // Set up listeners or nothing will work
            downloadButton.setOnClickListener(this);
            playButton.setOnClickListener(this);
            pauseButton.setOnClickListener(this);

            // Check if the file is downloaded
            if (cursor.getString(cursor.getColumnIndex(EpisodeEntry.DIRECTORY)) != null
                    && episodeID != null) {
                // Check if audio service is initialised
                if (audioService == null) {
                    // No episode is currently playing or paused
                    downloadButton.setVisibility(View.GONE);
                    playButton.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(View.GONE);
                    playButton.setContentDescription(episodeID);
                } else if (audioService.getPlayer() != null) {
                    if(audioService.getPlayer().isPlaying() && episodeID.equals(
                            Integer.toString(audioService.getEpisode().getEpisodeID()))) {
                        // Playing the same episode that is in this same row
                        downloadButton.setVisibility(View.GONE);
                        playButton.setVisibility(View.GONE);
                        pauseButton.setVisibility(View.VISIBLE);
                        pauseButton.setContentDescription(episodeID);
                    } else {
                        // Audio Service is up but playing a different episode
                        downloadButton.setVisibility(View.GONE);
                        playButton.setVisibility(View.VISIBLE);
                        pauseButton.setVisibility(View.GONE);
                        playButton.setContentDescription(episodeID);
                    }
                } else {
                    // Audio service is up but not playing at all
                    downloadButton.setVisibility(View.GONE);
                    playButton.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(View.GONE);
                    playButton.setContentDescription(episodeID);
                }
            } else {
                // This makes sure that when views are recycled that they reset state to
                // not downloaded
                downloadButton.setVisibility(View.VISIBLE);
                playButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.GONE);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return layoutInflater.inflate(R.layout.episode_list_item, parent, false);
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
                if (sparseBArray.get(position)) {
                    v.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                }
            }
            return v;
        }

        @Override
        public void onClick(View v) {
            int viewId = v.getId();
            if(getApplicationContext() != null) {
                if (viewId == R.id.download_icon) {
                    // Download the podcast
                    if(Utilities.testNetwork(getApplicationContext())) {
                        Intent intent = new Intent(getApplicationContext(), DownloadActivity.class);
                        intent.setAction(Utilities.ACTION_DOWNLOAD);
                        intent.putExtra(Utilities.EPISODE_ID, v.getContentDescription());
                        intent.putExtra(Utilities.PODCAST_TITLE, podcastTitle);
                        intent.putExtra(Utilities.PODCAST_ID, podcastID);
                        startActivity(intent);
                    }
                } else if (viewId == R.id.play_icon) {
                    if (audioService == null) {
                        // Play podcast in a background service
                        Intent intent = new Intent(getApplicationContext(), AudioPlayerService.class);
                        intent.putExtra(Utilities.EPISODE_ID, v.getContentDescription());
                        intent.putExtra(Utilities.PODCAST_TITLE, podcastTitle);
                        intent.setAction(Utilities.ACTION_NEW_EPISODE);
                        // Investigate Correct flag and compatibility
                        startService(intent);
                        bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
                    } else {
                        if (v.getContentDescription() != null) {
                            String episodeID = v.getContentDescription().toString();
                            if (audioService.getPlayer() != null) {
                                if (episodeID.equals(Integer.toString(audioService.getEpisode().getEpisodeID())) ) {
                                    audioService.resumeMedia();
                                } else {
                                    audioService.playNewEpisode(episodeID, true, podcastTitle);
                                }
                            } else {
                                // Play pressed while other app is playing
                                Intent intent = new Intent(getApplicationContext(), AudioPlayerService.class);
                                intent.putExtra(Utilities.EPISODE_ID, v.getContentDescription());
                                intent.putExtra(Utilities.PODCAST_TITLE, podcastTitle);
                                intent.setAction(Utilities.ACTION_NEW_EPISODE);
                                // Investigate Correct flag and compatibility
                                startService(intent);
                                bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);

                            }
                        }
                    }
                } else if (viewId == R.id.pause_icon) {
                    // Pause podcast in background service
                    if(audioService.getPlayer().isPlaying()) {
                        audioService.pauseMedia(false);
                    }
                }
            }
        }
    }
}
