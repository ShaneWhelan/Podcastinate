package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.database.Cursor;
import android.util.Log;

import com.shanewhelan.podcastinate.database.PodcastContract;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public class PodcastViewerActivity extends Activity {


    public void listAllPodcasts() {
        PodcastDataSource dataSource = new PodcastDataSource(this);
        dataSource.openDb();
        Cursor cursor = dataSource.getAllPodcastNames();
        if(cursor != null) {
            cursor.moveToFirst();
            while(cursor.moveToNext()){
                Log.d("sw9", cursor.getString(cursor.getColumnIndex(PodcastContract.PodcastEntry.COLUMN_NAME_TITLE)));
            }
        }
        cursor.close();
        dataSource.closeDb();
    }
}
