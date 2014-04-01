package com.shanewhelan.podcastinate.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.shanewhelan.podcastinate.R;

public class SettingsActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}