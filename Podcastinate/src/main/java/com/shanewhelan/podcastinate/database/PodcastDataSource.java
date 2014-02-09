package com.shanewhelan.podcastinate.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.shanewhelan.podcastinate.Episode;
import com.shanewhelan.podcastinate.database.PodcastContract.EpisodeEntry;
import com.shanewhelan.podcastinate.database.PodcastContract.PodcastEntry;

import java.util.Arrays;

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

    public long insertPodcast(String title, String description, String imageDirectory, String link) {
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
        } catch (SQLiteConstraintException e) {
            Log.e("sw9", Arrays.toString(e.getStackTrace()));
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
        return database.insert(EpisodeEntry.TABLE_NAME, EpisodeEntry.COLUMN_NAME_TITLE,
                contentValues);
    }

    public long updateEpisodeDirectory(String enclosure, String directory) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EpisodeEntry.COLUMN_NAME_DIRECTORY, directory);
        return database.update(EpisodeEntry.TABLE_NAME, contentValues,
                EpisodeEntry.COLUMN_NAME_ENCLOSURE + " = \"" + enclosure + "\"", null);
    }


    public int getPodcastID(String podcastTitle) {
        int podcastId = 0;
        String[] columns = {PodcastEntry.COLUMN_NAME_PODCAST_ID};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns,
                PodcastEntry.COLUMN_NAME_TITLE + " = \"" + podcastTitle + "\"", null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            podcastId = cursor.getInt(cursor.getColumnIndex(PodcastEntry.COLUMN_NAME_PODCAST_ID));
            cursor.close();
        }
        return podcastId;
    }

    public Cursor getAllPodcastNames() {
        return database.rawQuery("SELECT " + PodcastEntry.COLUMN_NAME_PODCAST_ID
                + " as _id, title FROM " + PodcastEntry.TABLE_NAME, null);
    }

    public String[] getAllPodcastLinks() {
        String[] columns = {PodcastEntry.COLUMN_NAME_LINK};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns, null, null, null, null, null);

        String[] listOfPodcasts = new String[cursor.getCount()];

        if (cursor != null) {
            int i = 0;
            while (cursor.moveToNext()) {
                listOfPodcasts[i] = cursor.getString(cursor.getColumnIndex(PodcastEntry.COLUMN_NAME_LINK));
                i++;
            }
            cursor.close();
        }
        return listOfPodcasts;
    }

    public Cursor getAllEpisodeNames(int podcastID) {
        return database.rawQuery("SELECT " + EpisodeEntry.COLUMN_NAME_EPISODE_ID +
                " as _id, " + EpisodeEntry.COLUMN_NAME_TITLE + ", " +
                EpisodeEntry.COLUMN_NAME_DIRECTORY + ", " +
                EpisodeEntry.COLUMN_NAME_LISTENED + ", " +
                EpisodeEntry.COLUMN_NAME_CURRENT_TIME + ", " +
                EpisodeEntry.COLUMN_NAME_DURATION + ", " +
                EpisodeEntry.COLUMN_NAME_IMAGE_DIRECTORY + ", " +
                EpisodeEntry.COLUMN_NAME_ENCLOSURE + " FROM " + EpisodeEntry.TABLE_NAME +
                " WHERE " + EpisodeEntry.COLUMN_NAME_PODCAST_ID
                + " = " + podcastID, null);
    }

    public String getEpisodeEnclosure(String podcastTitle, String episodeTitle){
        String enclosure = "";
        int podcastId = getPodcastID(podcastTitle);
        String[] columns = {EpisodeEntry.COLUMN_NAME_ENCLOSURE};
        Cursor cursor = database.query(EpisodeEntry.TABLE_NAME, columns,
                EpisodeEntry.COLUMN_NAME_TITLE + " = \"" + episodeTitle + "\" AND " +
                EpisodeEntry.COLUMN_NAME_PODCAST_ID + " = " + podcastId,
                null, null, null, null);
        if(cursor != null) {
            cursor.moveToFirst();
            enclosure = cursor.getString(cursor.getColumnIndex(EpisodeEntry.COLUMN_NAME_ENCLOSURE));
            cursor.close();
        }
        return enclosure;
    }

    // TODO: Look into refactoring with podcastTitle as a parameter
    public Episode getEpisodeMetaData(String directory) {
        String[] columns = {EpisodeEntry.COLUMN_NAME_LISTENED,
                EpisodeEntry.COLUMN_NAME_CURRENT_TIME, EpisodeEntry.COLUMN_NAME_PODCAST_ID,
                EpisodeEntry.COLUMN_NAME_TITLE, EpisodeEntry.COLUMN_NAME_DESCRIPTION,
                EpisodeEntry.COLUMN_NAME_PUB_DATE, EpisodeEntry.COLUMN_NAME_DURATION,
                EpisodeEntry.COLUMN_NAME_IMAGE_DIRECTORY};

        Cursor cursor = database.query(EpisodeEntry.TABLE_NAME, columns,
                EpisodeEntry.COLUMN_NAME_DIRECTORY + " = \"" + directory + "\"",
                null, null, null, null);
        if(cursor != null) {
            cursor.moveToFirst();
            Episode episode = new Episode();
            int listened = cursor.getInt(cursor.getColumnIndex(EpisodeEntry.COLUMN_NAME_LISTENED));

            if(listened == 1) {
                episode.setListened(true);
            } else {
                episode.setListened(false);
            }
            episode.setCurrentTime(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.COLUMN_NAME_CURRENT_TIME)));
            episode.setPodcastID(cursor.getInt(cursor.getColumnIndex(
                    EpisodeEntry.COLUMN_NAME_PODCAST_ID)));
            episode.setTitle(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.COLUMN_NAME_TITLE)));
            episode.setDescription(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.COLUMN_NAME_DESCRIPTION)));
            episode.setPubDate(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.COLUMN_NAME_PUB_DATE)));
            episode.setDuration(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.COLUMN_NAME_DURATION)));
            episode.setEpisodeImage(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.COLUMN_NAME_IMAGE_DIRECTORY)));
            cursor.close();
            return episode;
        }
        return null;
    }
}
