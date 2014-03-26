package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.MenuItem;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.services.DownloadService;

public class DownloadActivity extends Activity {
    private String mostRecentPodcastTitle;
    private int mostRecentPodcastID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        if (getIntent() != null) {
            if (getIntent().getAction() != null) {
                if(getIntent().getAction().equals(Utilities.ACTION_DOWNLOAD)) {
                    addToDownloadQueue();
                }
            }
        }
    }

    private void addToDownloadQueue() {
        String episodeID = getIntent().getStringExtra(Utilities.EPISODE_ID);
        mostRecentPodcastID = getIntent().getIntExtra(Utilities.PODCAST_ID, -1);
        mostRecentPodcastTitle = getIntent().getStringExtra(Utilities.PODCAST_TITLE);

        Intent intent = new Intent(getApplicationContext(), DownloadService.class);
        intent.putExtra(Utilities.EPISODE_ID, episodeID);
        intent.putExtra(Utilities.PODCAST_ID, mostRecentPodcastID);
        intent.putExtra(Utilities.PODCAST_TITLE, mostRecentPodcastTitle);
        startService(intent);
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
}
