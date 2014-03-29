package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.shanewhelan.podcastinate.DownloadListItem;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.services.DownloadService;

import java.util.ArrayList;

public class DownloadActivity extends Activity {
    private DownloadService downloadService;
    private DownloadAdapter downloadAdapter;
    private ServiceConnection serviceConnection;
    private String mostRecentPodcastTitle;
    private int mostRecentPodcastID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        if (getIntent() != null) {
            if (getIntent().getAction() != null) {
                if (getIntent().getAction().equals(Utilities.ACTION_DOWNLOAD)) {
                    addToDownloadQueue();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                DownloadService.DownloadBinder binder = (DownloadService.DownloadBinder) service;
                downloadService = binder.getService();
                initialiseAdapter();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                downloadService = null;
            }
        };

        Intent intent = new Intent(getApplicationContext(), DownloadService.class);
        // 3 parameter is 0 because this means "bind if exists"
        bindService(intent, serviceConnection, 0);

        registerReceiver(broadcastReceiver, new IntentFilter(Utilities.ACTION_DOWNLOADED));
        registerReceiver(broadcastReceiver, new IntentFilter(Utilities.ACTION_CANCEL_COMPLETE));
        registerReceiver(broadcastReceiver, new IntentFilter(Utilities.ACTION_QUEUED));


        // TODO syncControlPanel();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(serviceConnection);
        unregisterReceiver(broadcastReceiver);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utilities.ACTION_DOWNLOADED.equals(intent.getAction())) {
                downloadAdapter.notifyDataSetChanged();
            } else if(Utilities.ACTION_CANCEL_COMPLETE.equals(intent.getAction())) {
                downloadAdapter.notifyDataSetChanged();
            } else if(Utilities.ACTION_QUEUED.equals(intent.getAction())) {
                /*
                if(downloadAdapter != null) {
                    downloadAdapter.notifyDataSetChanged();
                } else {
                    initialiseAdapter();
                }*/

            }
        }
    };

    private void addToDownloadQueue() {
        String episodeID = getIntent().getStringExtra(Utilities.EPISODE_ID);
        mostRecentPodcastID = getIntent().getIntExtra(Utilities.PODCAST_ID, -1);
        mostRecentPodcastTitle = getIntent().getStringExtra(Utilities.PODCAST_TITLE);

        Intent intent = new Intent(getApplicationContext(), DownloadService.class);
        intent.putExtra(Utilities.EPISODE_ID, episodeID);
        intent.putExtra(Utilities.PODCAST_ID, mostRecentPodcastID);
        intent.putExtra(Utilities.PODCAST_TITLE, mostRecentPodcastTitle);
        intent.setAction(Utilities.ACTION_DOWNLOAD);
        startService(intent);
    }

    public void initialiseAdapter() {
        ListView downloadList = (ListView) findViewById(R.id.listOfDownloads);
        downloadAdapter = new DownloadAdapter(getApplicationContext(),
                R.layout.download_list_item, R.id.downloadEpisodeTitle, downloadService.getDownloadList());
        downloadList.setAdapter(downloadAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(getApplicationContext(), PodcastViewerActivity.class);
                upIntent.setAction(Utilities.VIEW_PODCAST);
                upIntent.putExtra(Utilities.PODCAST_TITLE, mostRecentPodcastTitle);
                upIntent.putExtra(Utilities.PODCAST_ID, mostRecentPodcastID);
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

    public class DownloadAdapter extends ArrayAdapter<DownloadListItem> implements View.OnClickListener{

        public DownloadAdapter(Context context, int resource, int textViewResourceId, ArrayList<DownloadListItem> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        private void bindView(int position, View view) {
            DownloadListItem downloadListItem = super.getItem(position);
            if (downloadListItem != null) {
                /*
                // Load image into thumbnail slot asynchronously
                ImageView thumbNail = (ImageView) view.findViewById(R.id.recommendationImage);
                thumbNail.setContentDescription("" + position);
                thumbNail.setOnClickListener(this);
                */
                // Update text view for this result
                TextView episodeTitle = (TextView) view.findViewById(R.id.downloadEpisodeTitle);
                episodeTitle.setText(downloadListItem.getEpisode().getTitle());

                TextView podcastTitle = (TextView) view.findViewById(R.id.downloadPodcastTitle);
//              podcastTitle.setOnClickListener(this);
                podcastTitle.setText(downloadListItem.getPodcastTitle());
                podcastTitle.setContentDescription("" + position);

                ImageButton cancelButton = (ImageButton) view.findViewById(R.id.cancelButton);
                cancelButton.setContentDescription(Integer.toString(position));
                cancelButton.setOnClickListener(this);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // convertView = layoutInflater.inflate(R.layout.activity_search_results, parent, false);
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
            if(v.getContentDescription() != null) {
                if(getApplicationContext() != null) {
                    int position = Integer.parseInt(v.getContentDescription().toString());
                    // Start New Subscribe Activity
                    Intent cancelIntent = new Intent(Utilities.ACTION_CANCEL);
                    cancelIntent.putExtra(Utilities.EPISODE_ID,
                            downloadService.getDownloadList().get(position).getEpisode().
                                    getEpisodeID());
                    getApplicationContext().sendBroadcast(cancelIntent);
                    //Intent subscribe = new Intent(getApplicationContext(), MainActivity.class);
                    //subscribe.setAction(Utilities.ACTION_SUBSCRIBE);
                    //subscribe.putExtra(Utilities.PODCAST_LINK, episode[position].getLink());
                    //startActivity(subscribe);
                }
            }
        }
    }
}
