package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.shanewhelan.podcastinate.R;

public class DownloadActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        Log.d("sw9", this.getIntent().getStringExtra("episode_name"));
    }

}
