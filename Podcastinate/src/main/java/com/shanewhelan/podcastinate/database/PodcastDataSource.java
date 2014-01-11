package com.shanewhelan.podcastinate.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.shanewhelan.podcastinate.database.PodcastContract.*;

/**
 * Created by Shane on 11/01/14. Podcastinate.
 */
public class PodcastDataSource {
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;


    public PodcastDataSource(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public void openDb() {
        database = databaseHelper.getWritableDatabase();
    }

    public void closeDb() {
        databaseHelper.close();
    }

    public long insertPodcast(String title, String description, String imageDirectory, String link){
        // Create ContentValues Key-Value pair
        ContentValues contentValues = new ContentValues();
        contentValues.put(PodcastEntry.COLUMN_NAME_TITLE, title);
        contentValues.put(PodcastEntry.COLUMN_NAME_DESCRIPTION, description);
        contentValues.put(PodcastEntry.COLUMN_NAME_IMAGE_DIRECTORY, imageDirectory);
        contentValues.put(PodcastEntry.COLUMN_NAME_LINK, link);

        long result = 0;
        try {
            result = database.insertOrThrow(PodcastEntry.TABLE_NAME, PodcastEntry.COLUMN_NAME_TITLE,
                    contentValues);
        }catch (SQLiteConstraintException e){
            Log.e("sw9", e.getCause() + e.toString() + e.getStackTrace());
        }
        return result;
    }

    public long insertEpisode(int podcastId, String episodeTitle, String link,
                              String description, String date, String guid, String duration,
                              String imageDirectory, String enclosure) {
        // Create ContentValues Key-Value pair
        ContentValues contentValues = new ContentValues();
        contentValues.put(EpisodeEntry.COLUMN_NAME_PODCAST_ID, podcastId);
        contentValues.put(EpisodeEntry.COLUMN_NAME_TITLE, episodeTitle);
        contentValues.put(EpisodeEntry.COLUMN_NAME_EPISODE_LINK, link);
        contentValues.put(EpisodeEntry.COLUMN_NAME_DESCRIPTION, description);
        contentValues.put(EpisodeEntry.COLUMN_NAME_PUB_DATE, date);
        contentValues.put(EpisodeEntry.COLUMN_NAME_GUID, guid);
        contentValues.put(EpisodeEntry.COLUMN_NAME_DURATION, duration);
        contentValues.put(EpisodeEntry.COLUMN_NAME_IMAGE_DIRECTORY, imageDirectory);
        contentValues.put(EpisodeEntry.COLUMN_NAME_ENCLOSURE, enclosure);
        long rowId =  database.insert(EpisodeEntry.TABLE_NAME, EpisodeEntry.COLUMN_NAME_TITLE,
                contentValues);
        return rowId;
    }


    public int getPodcastID(String podcastTitle) {
        int podcastId = 0;
        String[] columns = {PodcastEntry.COLUMN_NAME_PODCAST_ID, PodcastEntry.COLUMN_NAME_TITLE };
        String sortOrder = PodcastEntry.COLUMN_NAME_TITLE + " DESC";
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns, null, null, null, null,
                sortOrder);
            if(cursor != null) {
            cursor.moveToFirst();
            while(cursor.moveToNext()){
                if(cursor.getString(cursor.getColumnIndex(PodcastEntry.COLUMN_NAME_TITLE)).equals(podcastTitle) ) {
                    podcastId = cursor.getInt(cursor.getColumnIndex(PodcastEntry.COLUMN_NAME_PODCAST_ID));
                }
            }
        }
        cursor.close();
        return podcastId;
    }

    public Cursor getAllPodcastNames() {
        Cursor cursor = database.rawQuery("SELECT " + PodcastEntry.COLUMN_NAME_PODCAST_ID
                + " as _id, title FROM " + PodcastEntry.TABLE_NAME, null);
        return cursor;
    }

    public String[] getAllPodcastLinks() {
        String[] columns = {PodcastEntry.COLUMN_NAME_LINK};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns, null, null, null, null, null);

        String[] listOfPodcasts = new String[cursor.getCount()];

        if(cursor != null) {
            cursor.moveToFirst();
            int i = 0;
            while(cursor.moveToNext()){
                listOfPodcasts[i] = cursor.getString(cursor.getColumnIndex(PodcastEntry.COLUMN_NAME_LINK));
                i++;
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return listOfPodcasts;
    }

    public Cursor getAllEpisodeNames(int podcastID) {
        String[] columns = {PodcastContract.EpisodeEntry.COLUMN_NAME_TITLE};
        String sortOrder = PodcastEntry.COLUMN_NAME_TITLE + " DESC";
        Cursor cursor = database.query(EpisodeEntry.TABLE_NAME, columns,
                EpisodeEntry.COLUMN_NAME_PODCAST_ID + " = " + podcastID, null, null, null,
                sortOrder);
        return cursor;
    }
}
