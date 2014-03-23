package com.shanewhelan.podcastinate.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.shanewhelan.podcastinate.Episode;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastContract.EpisodeEntry;
import com.shanewhelan.podcastinate.database.PodcastContract.PodcastEntry;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Shane on 11/01/14. Podcastinate.
 */
public class PodcastDataSource {
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    public PodcastDataSource(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public void openDbForWriting() {
        database = databaseHelper.getWritableDatabase();
        if (database != null) {
            database.enableWriteAheadLogging();
        }
    }

    public void openDbForReading() {
        database = databaseHelper.getReadableDatabase();
        if (database != null) {
            database.enableWriteAheadLogging();
        }
    }

    public void closeDb() {
        databaseHelper.close();
    }

    public long insertPodcast(String title, String description, String imageDirectory, String directory, String link, int countNew) {
        // Create ContentValues Key-Value pair
        ContentValues contentValues = new ContentValues();
        contentValues.put(PodcastEntry.TITLE, title);
        contentValues.put(PodcastEntry.DESCRIPTION, description);
        contentValues.put(PodcastEntry.IMAGE_DIRECTORY, imageDirectory);
        contentValues.put(PodcastEntry.DIRECTORY, directory);
        contentValues.put(PodcastEntry.LINK, link);
        contentValues.put(PodcastEntry.COUNT_NEW, countNew);

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
                              String guid, String duration, String enclosure, boolean isNew) {
        // Create ContentValues Key-Value pair
        ContentValues contentValues = new ContentValues();
        contentValues.put(EpisodeEntry.PODCAST_ID, podcastId);
        contentValues.put(EpisodeEntry.TITLE, episodeTitle);
        contentValues.put(EpisodeEntry.DESCRIPTION, description);
        contentValues.put(EpisodeEntry.PUB_DATE, date);
        contentValues.put(EpisodeEntry.GUID, guid);
        contentValues.put(EpisodeEntry.DURATION, duration);
        contentValues.put(EpisodeEntry.ENCLOSURE, enclosure);
        if(isNew) {
            contentValues.put(EpisodeEntry.NEW_EPISODE, 1);
        } else {
            contentValues.put(EpisodeEntry.NEW_EPISODE, 0);
        }
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

    // Used to verify if podcast exists already or not
    public int getPodcastIDWithLink(String podcastLink) {
        Log.d("sw9", "Link " + podcastLink);
        int podcastId = -1;
        String[] columns = {PodcastEntry.PODCAST_ID};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns,
                PodcastEntry.LINK + " = \"" + podcastLink + "\"", null, null, null, null);
        try {
            if (cursor != null) {
                cursor.moveToFirst();
                podcastId = cursor.getInt(cursor.getColumnIndex(PodcastEntry.PODCAST_ID));
                Log.d("sw9", "ID " + podcastId);
                cursor.close();
            }
        } catch (Exception e) {
            Utilities.logException(e);
        }
        return podcastId;
    }

    public Cursor getPodcastInfoForAdapter() {
        return database.rawQuery("SELECT " + PodcastEntry.PODCAST_ID + " as _id, " +
                PodcastEntry.TITLE + ", " + PodcastEntry.IMAGE_DIRECTORY + ", " +
                PodcastEntry.DIRECTORY + ", " + PodcastEntry.COUNT_NEW +
                " FROM " + PodcastEntry.TABLE_NAME, null);
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


    public HashMap<String, String> getAllPodcastIDsLinks() {
        String[] columns = {PodcastEntry.PODCAST_ID, PodcastEntry.LINK};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns, null, null, null, null, null);

        HashMap<String, String> listOfPodcasts = new HashMap<String, String>(cursor.getCount());
        if (cursor != null) {
            while (cursor.moveToNext()) {
                listOfPodcasts.put(
                        String.valueOf(cursor.getString(cursor.getColumnIndex(PodcastEntry.PODCAST_ID))),
                        cursor.getString(cursor.getColumnIndex(PodcastEntry.LINK)) );
            }
            cursor.close();
        }
        return listOfPodcasts;
    }

    public JSONArray getAllPodcastLinksJson() {
        String[] columns = {PodcastEntry.LINK};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns, null, null, null, null, null);

        if (cursor != null) {
            JSONArray similarJson = new JSONArray();
            while (cursor.moveToNext()) {
                similarJson.put(cursor.getString(cursor.getColumnIndex(PodcastEntry.LINK)));
            }
            cursor.close();
            return similarJson;
        }
        return null;
    }


    public Cursor getAllEpisodeNames(int podcastID) {
        return database.rawQuery("SELECT " + EpisodeEntry.EPISODE_ID +
                " as _id, " + EpisodeEntry.TITLE + ", " +
                EpisodeEntry.DIRECTORY + ", " +
                EpisodeEntry.NEW_EPISODE + ", " +
                EpisodeEntry.CURRENT_TIME + ", " +
                EpisodeEntry.DURATION + ", " +
                EpisodeEntry.ENCLOSURE + " FROM " + EpisodeEntry.TABLE_NAME +
                " WHERE " + EpisodeEntry.PODCAST_ID
                + " = " + podcastID + " ORDER BY " + EpisodeEntry.PUB_DATE + " DESC", null);
    }

    public String getEpisodeEnclosure(int podcastId, String episodeTitle) {
        String enclosure = "";
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
            // Check is new episode
            if (cursor.getInt(cursor.getColumnIndex(EpisodeEntry.NEW_EPISODE)) == 1) {
                episode.setNew(true);
            } else {
                episode.setNew(false);
            }

            episode.setPodcastID(cursor.getInt(cursor.getColumnIndex(
                    EpisodeEntry.PODCAST_ID)));
            episode.setTitle(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.TITLE)));
            episode.setDescription(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.DESCRIPTION)));
            episode.setEnclosure(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.ENCLOSURE)));
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
    public String getMostRecentEpisodeEnclosure(int podcastID) {
        String[] columns = {EpisodeEntry.ENCLOSURE};

        Cursor cursor = database.query(EpisodeEntry.TABLE_NAME, columns,
                EpisodeEntry.PODCAST_ID + " = \"" + podcastID + "\"",
                null, null, null, EpisodeEntry.PUB_DATE + " DESC", "1");
        if (cursor != null) {
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                return cursor.getString(cursor.getColumnIndex(EpisodeEntry.ENCLOSURE));
            }
        }
        return null;
    }

    public void upgradeDB() {
        databaseHelper.onUpgrade(databaseHelper.getWritableDatabase(), 1, 2);
    }

    public int deletePodcast(int podcastID) {
        database.delete(EpisodeEntry.TABLE_NAME, EpisodeEntry.PODCAST_ID  + " = \"" + podcastID + "\"", null);
        return database.delete(PodcastEntry.TABLE_NAME, PodcastEntry.PODCAST_ID + " = \"" + podcastID + "\"", null);
    }

    public String getPodcastTitle(int podcastID) {
        String[] columns = {PodcastEntry.TITLE};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns,
                PodcastEntry.PODCAST_ID + " = \"" + podcastID + "\"",
                null, null, null, null);
        String podcastTitle = "";
        if (cursor != null) {
            cursor.moveToFirst();
            podcastTitle = cursor.getString(cursor.getColumnIndex(PodcastEntry.TITLE));
            cursor.close();
        }
        return podcastTitle;
    }

    public String getPodcastDirectory(int podcastID) {
        String[] columns = {PodcastEntry.DIRECTORY};
        Cursor cursor = database.query(PodcastEntry.TABLE_NAME, columns,
                PodcastEntry.PODCAST_ID + " = \"" + podcastID + "\"",
                null, null, null, null);
        String podcastDirectory = "";
        if (cursor != null) {
            cursor.moveToFirst();
            podcastDirectory = cursor.getString(cursor.getColumnIndex(PodcastEntry.DIRECTORY));
            cursor.close();
        }
        return podcastDirectory;
    }

    public int getCountNew(int podcastID) {
        Cursor cursor =  database.rawQuery("SELECT " + PodcastEntry.COUNT_NEW + " FROM " +
                PodcastEntry.TABLE_NAME + " WHERE " + PodcastEntry.PODCAST_ID + " = " + podcastID
                , null);
        int countNew = 0;
        if(cursor != null) {
            cursor.moveToFirst();
            countNew = cursor.getInt(cursor.getColumnIndex(PodcastEntry.COUNT_NEW));
        }
        return countNew;
    }

    public long updateCountNew(int podcastID, int countNew) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PodcastEntry.COUNT_NEW, countNew);
        return database.update(PodcastEntry.TABLE_NAME, contentValues,
                PodcastEntry.PODCAST_ID + " = \"" + podcastID + "\"", null);
    }

    public void removeTwoEpisodesFromEach() {
        HashMap<String, String> listOfIds = getAllPodcastIDsLinks();
        Set entrySet = listOfIds.entrySet();
        for (Object anEntrySet : entrySet) {
            Map.Entry mapEntry = (Map.Entry) anEntrySet;
            Log.d("sw9", "" + database.delete(EpisodeEntry.TABLE_NAME, EpisodeEntry.EPISODE_ID + " IN (" + "SELECT " +
                    EpisodeEntry.EPISODE_ID + " FROM " + EpisodeEntry.TABLE_NAME + " WHERE "
                    + PodcastEntry.PODCAST_ID + " = \"" + Integer.parseInt(mapEntry.getKey().toString()) + "\"" +
                    " ORDER BY " + EpisodeEntry.PUB_DATE + " DESC LIMIT 2)", null));
        }
        synchroniseNewCount();
    }

    public void synchroniseNewCount() {
        Cursor cursor = database.rawQuery("SELECT " + EpisodeEntry.PODCAST_ID + ", count(" +
                EpisodeEntry.NEW_EPISODE + ") FROM " + EpisodeEntry.TABLE_NAME +
                " WHERE " + EpisodeEntry.NEW_EPISODE + " = 1" +
                " GROUP BY " + EpisodeEntry.PODCAST_ID, null);

        if(cursor != null) {
            if(cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndex(EpisodeEntry.PODCAST_ID));
                    int count = cursor.getInt(cursor.getColumnIndex("count(" + EpisodeEntry.NEW_EPISODE + ")"));

                    updateCountNew(id, count);
                }
            } else {
                HashMap<String, String> listOfIds = getAllPodcastIDsLinks();
                Set entrySet = listOfIds.entrySet();
                for (Object anEntrySet : entrySet) {
                    Map.Entry mapEntry = (Map.Entry) anEntrySet;
                    updateCountNew(Integer.parseInt(mapEntry.getKey().toString()), 0);
                }
            }
            cursor.close();
        }
     }
}
