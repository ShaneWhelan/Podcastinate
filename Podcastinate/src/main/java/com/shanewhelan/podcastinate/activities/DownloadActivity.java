package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.MenuItem;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.services.DownloadService;

public class DownloadActivity extends Activity {
    private String podcastTitle;
    private int podcastID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        addToDownloadQueue();
    }

    private void addToDownloadQueue() {
        podcastTitle = getIntent().getStringExtra(Utilities.PODCAST_TITLE);
        String episodeTitle = getIntent().getStringExtra(Utilities.EPISODE_TITLE);
        podcastID = getIntent().getIntExtra(Utilities.PODCAST_ID, -1);

        PodcastDataSource dataSource = new PodcastDataSource(getApplicationContext());
        dataSource.openDbForReading();
        String enclosure = dataSource.getEpisodeEnclosure(podcastID, episodeTitle);
        Intent intent = new Intent(getApplicationContext(), DownloadService.class);
        intent.putExtra(Utilities.PODCAST_TITLE, podcastTitle);
        intent.putExtra(Utilities.EPISODE_TITLE, episodeTitle);
        intent.putExtra(Utilities.PODCAST_ID, podcastID);
        intent.putExtra(Utilities.ENCLOSURE, enclosure);
        startService(intent);
        dataSource.closeDb();
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
                upIntent.putExtra(Utilities.PODCAST_TITLE, podcastTitle);
                upIntent.putExtra(Utilities.PODCAST_ID, podcastID);
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
