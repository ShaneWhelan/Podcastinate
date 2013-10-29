package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.database.Cursor;
import android.util.Log;

import com.shanewhelan.podcastinate.database.DatabaseHelper;
import com.shanewhelan.podcastinate.database.PodcastContract;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public class PodcastViewerActivity extends Activity {


    public void listAllPodcasts() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        Cursor cursor = dbHelper.getAllPodcastNames();
        if(cursor != null) {
            cursor.moveToFirst();
            while(cursor.moveToNext()){
                Log.d("sw1", cursor.getString(cursor.getColumnIndex(PodcastContract.PodcastEntry.COLUMN_NAME_TITLE)));
            }
        }
        cursor.close();
    }
}
