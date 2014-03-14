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
        String podcastTitle = this.getIntent().getStringExtra(Utilities.PODCAST_TITLE);
        String episodeTitle = this.getIntent().getStringExtra(Utilities.EPISODE_TITLE);

        PodcastDataSource dataSource = new PodcastDataSource(getApplicationContext());
        dataSource.openDbForReading();
        String enclosure = dataSource.getEpisodeEnclosure(podcastTitle, episodeTitle);
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra(Utilities.PODCAST_TITLE, podcastTitle);
        intent.putExtra(Utilities.EPISODE_TITLE, episodeTitle);
        intent.putExtra(Utilities.ENCLOSURE, enclosure);
        this.startService(intent);
        dataSource.closeDb();
    }
}
