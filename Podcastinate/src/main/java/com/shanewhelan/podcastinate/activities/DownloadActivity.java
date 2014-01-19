package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

public class DownloadActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        String podcastTitle = this.getIntent().getStringExtra("podcast_title");
        String episodeTitle = this.getIntent().getStringExtra("episode_title");
        addToDownloadQueue(podcastTitle, episodeTitle);
    }

    private void addToDownloadQueue(String podcastTitle, String episodeTitle) {
        PodcastDataSource dataSource = new  PodcastDataSource(this);
        dataSource.openDb();
        Log.d("sw9", dataSource.getEpisodeEnclosure(podcastTitle, episodeTitle));
        dataSource.closeDb();
    }

}
