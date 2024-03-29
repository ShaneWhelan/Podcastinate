package com.shanewhelan.podcastinate.database;

import android.annotation.SuppressLint;
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

    public long updateEpisodeDirectory(int episodeId, String directory) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EpisodeEntry.DIRECTORY, directory);
        return database.update(EpisodeEntry.TABLE_NAME, contentValues,
                EpisodeEntry.EPISODE_ID + " = " + episodeId, null);
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

    // Gets all the relevant info for the PodcastViewerActivity adapter
    public Cursor getAllEpisodeInfoForAdapter(int podcastID) {
        return database.rawQuery("SELECT " + EpisodeEntry.EPISODE_ID + " as _id, " +
                EpisodeEntry.TITLE + ", " +
                EpisodeEntry.DESCRIPTION + ", " +
                EpisodeEntry.ENCLOSURE + ", " +
                EpisodeEntry.PUB_DATE + ", " +
                EpisodeEntry.DURATION + ", " +
                EpisodeEntry.DIRECTORY + ", " +
                EpisodeEntry.NEW_EPISODE + ", " +
                EpisodeEntry.CURRENT_TIME +
                " FROM " + EpisodeEntry.TABLE_NAME +
                " WHERE " + EpisodeEntry.PODCAST_ID
                + " = " + podcastID + " ORDER BY " + EpisodeEntry.PUB_DATE + " DESC", null);
    }

    public Episode getEpisodeMetaDataForDownload(String episodeID) {
        int episodeIDasInt = Integer.parseInt(episodeID);

        Cursor cursor = database.rawQuery("SELECT * FROM " + EpisodeEntry.TABLE_NAME +
                " WHERE " + EpisodeEntry.EPISODE_ID + " = " + episodeIDasInt, null);

        if (cursor != null) {
            cursor.moveToFirst();
            Episode episode = new Episode();
            // Check is new episode
            if (cursor.getInt(cursor.getColumnIndex(EpisodeEntry.NEW_EPISODE)) == 1) {
                episode.setNew(true);
            } else {
                episode.setNew(false);
            }

            episode.setEpisodeID(episodeIDasInt);
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

            cursor.close();
            return episode;
        }
        return null;
    }


    // Called by the AudioService to get all info it needs about podcast
    public Episode getEpisodeMetaDataForPlay(String episodeID) {
        int episodeIDasInt = Integer.parseInt(episodeID);

        Cursor cursor = database.rawQuery("SELECT * FROM " + EpisodeEntry.TABLE_NAME +
                " WHERE " + EpisodeEntry.EPISODE_ID + " = " + episodeIDasInt, null);

        if (cursor != null) {
            cursor.moveToFirst();
            Episode episode = new Episode();
            // Check is new episode
            if (cursor.getInt(cursor.getColumnIndex(EpisodeEntry.NEW_EPISODE)) == 1) {
                episode.setNew(true);
            } else {
                episode.setNew(false);
            }

            episode.setEpisodeID(cursor.getInt(cursor.getColumnIndex(
                    EpisodeEntry.EPISODE_ID)));
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
            episode.setDirectory(cursor.getString(cursor.getColumnIndex(
                    EpisodeEntry.DIRECTORY)));
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

    public long updatePodcastCountNew(int podcastID, int countNew) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PodcastEntry.COUNT_NEW, countNew);
        return database.update(PodcastEntry.TABLE_NAME, contentValues,
                PodcastEntry.PODCAST_ID + " = \"" + podcastID + "\"", null);
    }

    public long updateEpisodeIsNew(int episodeID, int isNew) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(EpisodeEntry.NEW_EPISODE, isNew);
        return database.update(EpisodeEntry.TABLE_NAME, contentValues,
                EpisodeEntry.EPISODE_ID + " = \"" + episodeID + "\"", null);
    }

    public boolean getEpisodeIsNew(int episodeID) {
        Cursor cursor =  database.rawQuery("SELECT " + EpisodeEntry.NEW_EPISODE + " FROM " +
                EpisodeEntry.TABLE_NAME + " WHERE " + EpisodeEntry.EPISODE_ID + " = " + episodeID
                , null);
        if(cursor != null) {
            cursor.moveToFirst();
            return cursor.getInt(cursor.getColumnIndex(EpisodeEntry.NEW_EPISODE)) == 1;
        }
        return false;
    }

    @SuppressLint("UseSparseArrays")
    public void removeTwoEpisodesFromEach() {
        HashMap<String, String> listOfIds = getAllPodcastIDsLinks();
        Set entrySet = listOfIds.entrySet();
         HashMap<Integer, Integer> countsOfDeletedEpisodes = new HashMap<Integer, Integer>(listOfIds.size());
        // Iterate through the list of IDs and use key to delete last two episodes
        for (Object anEntrySet : entrySet) {
            Map.Entry mapEntry = (Map.Entry) anEntrySet;
            int podcast_id = Integer.parseInt(mapEntry.getKey().toString());
            int result = database.delete(EpisodeEntry.TABLE_NAME, EpisodeEntry.EPISODE_ID + " IN (" + "SELECT " +
                    EpisodeEntry.EPISODE_ID + " FROM " + EpisodeEntry.TABLE_NAME + " WHERE "
                    + PodcastEntry.PODCAST_ID + " = \"" + podcast_id + "\"" +
                    " ORDER BY " + EpisodeEntry.PUB_DATE + " DESC LIMIT 2)", null);

            countsOfDeletedEpisodes.put(podcast_id, result);
        }
        synchroniseNewCount(countsOfDeletedEpisodes);
    }

    public void synchroniseNewCount(HashMap<Integer, Integer> countsOfDeletedEpisodes) {
        Set entrySet = countsOfDeletedEpisodes.entrySet();
        for (Object anEntrySet : entrySet) {
            Map.Entry mapEntry = (Map.Entry) anEntrySet;
            int podcast_id = Integer.parseInt(mapEntry.getKey().toString());
            int amountRemoved = Integer.parseInt(mapEntry.getValue().toString());
            int currentCountNew = getCountNew(podcast_id);
            if (currentCountNew != 0) {
                if((currentCountNew - amountRemoved) < 0) {
                    updatePodcastCountNew(podcast_id, 0);
                } else {
                    updatePodcastCountNew(podcast_id, currentCountNew - amountRemoved);
                }
            }
        }
     }
}
