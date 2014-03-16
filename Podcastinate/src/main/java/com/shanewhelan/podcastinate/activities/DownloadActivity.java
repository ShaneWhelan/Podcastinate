package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.services.DownloadService;

public class DownloadActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        addToDownloadQueue();
    }

    private void addToDownloadQueue() {
        String podcastTitle = getIntent().getStringExtra(Utilities.PODCAST_TITLE);
        String episodeTitle = getIntent().getStringExtra(Utilities.EPISODE_TITLE);
        int podcastID = getIntent().getIntExtra(Utilities.PODCAST_ID, -1);

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
}
