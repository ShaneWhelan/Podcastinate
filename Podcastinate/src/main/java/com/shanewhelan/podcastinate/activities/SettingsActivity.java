package com.shanewhelan.podcastinate.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

import java.io.File;
import java.io.IOException;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference preference = findPreference("wipe_db");
        if (preference != null) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    wipeDb();
                    return true;
                }
            });
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        if(getPreferenceScreen() != null) {
            if(getPreferenceScreen().getSharedPreferences() != null) {
                getPreferenceScreen().getSharedPreferences()
                        .registerOnSharedPreferenceChangeListener(this);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        if(getPreferenceScreen() != null) {
            if (getPreferenceScreen().getSharedPreferences() != null) {
                getPreferenceScreen().getSharedPreferences()
                        .unregisterOnSharedPreferenceChangeListener(this);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("refresh_interval")) {
            setRecurringRefreshTask(
                    sharedPreferences.getString("refresh_interval", "14400000"));
        }
    }

    private void setRecurringRefreshTask(String minutes) {
        if(getApplicationContext() != null) {
            // Cancel an alarm that may be previously set
            Intent refreshFeedsCancel = new Intent(getApplicationContext(), Utilities.RefreshFeeds.class);
            refreshFeedsCancel.setAction(Utilities.ACTION_REFRESH);

            PendingIntent recurringRefreshCancel = PendingIntent.getBroadcast(getApplicationContext(),
                    0, refreshFeedsCancel, PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(recurringRefreshCancel);
            if (!minutes.equals("0")) {
                // Set up new alarm
                long lengthOfTime = Long.parseLong(minutes);
                Intent refreshFeedsIntent = new Intent(getApplicationContext(), Utilities.RefreshFeeds.class);
                refreshFeedsIntent.setAction(Utilities.ACTION_REFRESH);

                PendingIntent recurringRefresh = PendingIntent.getBroadcast(getApplicationContext(),
                        0, refreshFeedsIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + lengthOfTime, lengthOfTime, recurringRefresh);
            }
        }
    }

    // Following two methods are debug only methods
    public void wipeDb() {
        // Only to be left in developer version - wipes db and external storage directory
        PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
        pds.openDbForWriting();
        pds.upgradeDB();
        pds.closeDb();
        Log.d("sw9", Environment.getExternalStorageDirectory().getAbsolutePath() + "/Podcastinate");
        File externalDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Podcastinate");

        if (externalDir.exists()) {
            String deleteCmd = "rm -r " + externalDir.getAbsolutePath();
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
                Log.d("sw9", "Directory deleted");
            } catch (IOException e) {
                Utilities.logException(e);
            }
        }
    }
}