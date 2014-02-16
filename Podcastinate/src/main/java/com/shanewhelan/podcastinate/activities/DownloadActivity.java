package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.services.DownloadService;

public class DownloadActivity extends Activity {
    public static final String PODCAST_TITLE = "podcast_title";
    public static final String EPISODE_TITLE = "episode_title";
    public static final String ENCLOSURE = "enclosure";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        addToDownloadQueue();
    }

    private void addToDownloadQueue() {
        String podcastTitle = this.getIntent().getStringExtra(PODCAST_TITLE);
        String episodeTitle = this.getIntent().getStringExtra(EPISODE_TITLE);

        PodcastDataSource dataSource = new  PodcastDataSource(this);
        dataSource.openDb();
        String enclosure = dataSource.getEpisodeEnclosure(podcastTitle, episodeTitle);
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra(PODCAST_TITLE, podcastTitle);
        intent.putExtra(EPISODE_TITLE, episodeTitle);
        intent.putExtra(ENCLOSURE, enclosure);
        this.startService(intent);
        dataSource.closeDb();
    }

}
