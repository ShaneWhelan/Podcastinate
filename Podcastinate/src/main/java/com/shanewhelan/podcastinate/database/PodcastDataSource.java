package com.shanewhelan.podcastinate.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.shanewhelan.podcastinate.Episode;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastContract.EpisodeEntry;
import com.shanewhelan.podcastinate.database.PodcastContract.PodcastEntry;

import java.util.HashMap;

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
        contentValues.put(PodcastEntry.TITLE, title);
        contentValues.put(PodcastEntry.DESCRIPTION, description);
        contentValues.put(PodcastEntry.IMAGE_DIRECTORY, imageDirectory);
        contentValues.put(PodcastEntry.LINK, link);

        long result = 0;
        try {
            result = database.insertOrThrow(PodcastEntry.TABLE_NAME, PodcastEntry.TITLE,
                    contentValues);
        } catch (SQLiteConstraintException e) {
            Utilities.logException(e);
        }
        return result;
    }

    public long insertEpisode(int podcastId, String episodeTitle, String description, String date,
                              String guid, String duration, String enclosure) {
        // Create ContentValues Key-Value pair
        ContentValues contentValues = new ContentValues();
        contentValues.put(EpisodeEntry.PODCAST_ID, podcastId);
        contentValues.put(EpisodeEntry.TITLE, episodeTitle);
        contentValues.put(EpisodeEntry.DESCRIPTION, description);
        contentValues.put(EpisodeEntry.PUB_DATE, date);
        contentValues.put(EpisodeEntry.GUID, guid);
        contentValues.put(EpisodeEntry.DURATION, duration);
        contentValues.put(EpisodeEntry.ENCLOSURE, enclosure);
        return database.insert(EpisodeEntry.TABLE_NAME, EpisodeEntry.TITLE,
                contentValues);
    }

    public long updateEpisodeDirectory(String enclosure, String directory) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EpisodeEntry.DIRECTORY, directory);
        return database.update(EpisodeEntry.TABLE_NAME, contentValues,
                EpisodeEntry.ENCLOSURE + " = \"" + enclosure + "\"", null);
    }

    public long updateCurrentTime(int episodeID, int currentTime) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EpisodeEntry.CURRENT_TIME, currentTime);
        return database.update(EpisodeEntry.TABLE_NAME, contentValues,
                EpisodeEntry.EPISODE_ID + " = \"" + episodeID + "\"", null);
    }

    public int getPodcastID(String podcastTitle) {
        int podcastId = 0;
        String[] columns = {PodcastEntry.PODCAST_ID};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns,
                PodcastEntry.TITLE + " = \"" + podcastTitle + "\"", null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            podcastId = cursor.getInt(cursor.getColumnIndex(PodcastEntry.PODCAST_ID));
            cursor.close();
        }
        return podcastId;
    }

    public Cursor getAllPodcastTitles() {
        return database.rawQuery("SELECT " + PodcastEntry.PODCAST_ID
                + " as _id, title FROM " + PodcastEntry.TABLE_NAME, null);
    }

    public String[] getAllPodcastLinks() {
        String[] columns = {PodcastEntry.LINK};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns, null, null, null, null, null);

        String[] listOfPodcasts = new String[cursor.getCount()];

        if (cursor != null) {
            int i = 0;
            while (cursor.moveToNext()) {
                listOfPodcasts[i] = cursor.getString(cursor.getColumnIndex(PodcastEntry.LINK));
                i++;
            }
            cursor.close();
        }
        return listOfPodcasts;
    }

    public String getPodcastImage(int podcastID) {
        String[] columns = {PodcastEntry.IMAGE_DIRECTORY};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns,
                PodcastEntry.PODCAST_ID + " = \"" + podcastID + "\"", null, null, null, null);

        String imageDirectory = null;
        if (cursor != null) {
            cursor.moveToFirst();
            imageDirectory = cursor.getString(cursor.getColumnIndex(PodcastEntry.IMAGE_DIRECTORY));
            cursor.close();
        }
        return imageDirectory;
    }


    public HashMap<String, String> getAllPodcastTitlesLinks() {
        String[] columns = {PodcastEntry.LINK, PodcastEntry.TITLE};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns, null, null, null, null, null);

        HashMap<String, String> listOfPodcasts = new HashMap<String, String>(cursor.getCount());
        if (cursor != null) {
            while (cursor.moveToNext()) {
                listOfPodcasts.put(
                        cursor.getString(cursor.getColumnIndex(PodcastEntry.TITLE)),
                        cursor.getString(cursor.getColumnIndex(PodcastEntry.LINK)));
            }
            cursor.close();
        }
        return listOfPodcasts;
    }

    public Cursor getAllEpisodeNames(int podcastID) {
        return database.rawQuery("SELECT " + EpisodeEntry.EPISODE_ID +
                " as _id, " + EpisodeEntry.TITLE + ", " +
                EpisodeEntry.DIRECTORY + ", " +
                EpisodeEntry.LISTENED + ", " +
                EpisodeEntry.CURRENT_TIME + ", " +
                EpisodeEntry.DURATION + ", " +
                EpisodeEntry.ENCLOSURE + " FROM " + EpisodeEntry.TABLE_NAME +
                " WHERE " + EpisodeEntry.PODCAST_ID
                + " = " + podcastID + " ORDER BY " + EpisodeEntry.PUB_DATE + " DESC", null);
    }

    public String getEpisodeEnclosure(String podcastTitle, String episodeTitle) {
        String enclosure = "";
        int podcastId = getPodcastID(podcastTitle);
        String[] columns = {EpisodeEntry.ENCLOSURE};
        Cursor cursor = database.query(EpisodeEntry.TABLE_NAME, columns,
                EpisodeEntry.TITLE + " = \"" + episodeTitle + "\" AND " +
                        EpisodeEntry.PODCAST_ID + " = " + podcastId,
                null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            enclosure = cursor.getString(cursor.getColumnIndex(EpisodeEntry.ENCLOSURE));
            cursor.close();
        }
        return enclosure;
    }

    // Called by the AudioService to get all info it needs about podcast
    public Episode getEpisodeMetaData(String directory) {
        Cursor cursor = database.rawQuery("SELECT * FROM episode WHERE directory = \""
                + directory + "\"", null);

        if (cursor != null) {
            cursor.moveToFirst();
            Episode episode = new Episode();
            int listened = cursor.getInt(cursor.getColumnIndex(EpisodeEntry.LISTENED));
            if (listened == 1) {
                episode.setListened(true);
            } else {
                episode.setListened(false);
            }

            episode.setPodcastID(cursor.getInt(cursor.getColumnIndex(
                    EpisodeEntry.PODCAST_ID)));
            episode.setTitle(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.TITLE)));
            episode.setDescription(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.DESCRIPTION)));
            episode.setPubDate(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.PUB_DATE)));
            episode.setDuration(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.DURATION)));
            episode.setEpisodeID(cursor.getInt(cursor.getColumnIndex(
                    EpisodeEntry.EPISODE_ID)));
            episode.setCurrentTime(cursor.getInt(cursor.getColumnIndex(
                    EpisodeEntry.CURRENT_TIME)));

            cursor.close();
            return episode;
        }
        return null;
    }

    // Used for comparing to the latest episode on refresh
    public String getMostRecentEpisodeEnclosure(String podcastTitle) {
        int podcastID = getPodcastID(podcastTitle);
        String[] columns = {EpisodeEntry.ENCLOSURE};

        Cursor cursor = database.query(EpisodeEntry.TABLE_NAME, columns,
                EpisodeEntry.PODCAST_ID + " = \"" + podcastID + "\"",
                null, null, null, EpisodeEntry.PUB_DATE + " DESC", "1");
        if (cursor != null) {
            cursor.moveToFirst();
            return cursor.getString(cursor.getColumnIndex(EpisodeEntry.ENCLOSURE));
        }
        return null;
    }

    public void upgradeDB() {
        databaseHelper.onUpgrade(databaseHelper.getWritableDatabase(), 1, 2);
    }
}
