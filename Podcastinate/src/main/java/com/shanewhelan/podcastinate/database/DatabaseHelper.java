package com.shanewhelan.podcastinate.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.shanewhelan.podcastinate.database.PodcastContract.*;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 *
 * Things left to implement:
 * Check if already exists
 * CRUD
 *
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_PODCAST =
            "CREATE TABLE " + PodcastEntry.TABLE_NAME + " (" +
                    PodcastEntry.COLUMN_NAME_PODCAST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    PodcastEntry.COLUMN_NAME_TITLE + TEXT_TYPE + " UNIQUE" + COMMA_SEP + " " +
                    PodcastEntry.COLUMN_NAME_DESCRIPTION + TEXT_TYPE + COMMA_SEP + " " +
                    PodcastEntry.COLUMN_NAME_IMAGE_DIRECTORY + TEXT_TYPE + COMMA_SEP + " " +
                    PodcastEntry.COLUMN_NAME_link + TEXT_TYPE + " UNIQUE" +
                    " );";
    private static final String SQL_CREATE_EPISODE =
            "CREATE TABLE " + EpisodeEntry.TABLE_NAME + " (" +
                    EpisodeEntry.COLUMN_NAME_EPISODE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    EpisodeEntry.COLUMN_NAME_PODCAST_ID + " INTEGER" + COMMA_SEP + " " +
                    EpisodeEntry.COLUMN_NAME_LISTENED + TEXT_TYPE + COMMA_SEP + " " +
                    EpisodeEntry.COLUMN_NAME_CURRENT_TIME + TEXT_TYPE + COMMA_SEP + " " +
                    EpisodeEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP + " " +
                    EpisodeEntry.COLUMN_NAME_EPISODE_LINK + TEXT_TYPE + " UNIQUE" + COMMA_SEP + " " +
                    EpisodeEntry.COLUMN_NAME_DESCRIPTION + TEXT_TYPE + COMMA_SEP + " " +
                    EpisodeEntry.COLUMN_NAME_PUB_DATE + TEXT_TYPE + COMMA_SEP + " " +
                    EpisodeEntry.COLUMN_NAME_GUID + TEXT_TYPE + COMMA_SEP + " " +
                    EpisodeEntry.COLUMN_NAME_DURATION + TEXT_TYPE + COMMA_SEP + " " +
                    EpisodeEntry.COLUMN_NAME_IMAGE_DIRECTORY + TEXT_TYPE + COMMA_SEP + " " +
                    EpisodeEntry.COLUMN_NAME_ENCLOSURE + TEXT_TYPE + COMMA_SEP + " " +
                    "FOREIGN KEY(" + EpisodeEntry.COLUMN_NAME_PODCAST_ID + ") REFERENCES " +
                    PodcastEntry.TABLE_NAME + "(" + PodcastEntry.COLUMN_NAME_PODCAST_ID + ") " +
                    " );";
    private static final String SQL_CREATE_CATEGORY =
            "CREATE TABLE " + CategoryEntry.TABLE_NAME + " (" +
                    CategoryEntry.COLUMN_NAME_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    CategoryEntry.COLUMN_NAME_NAME + " TEXT" +
                    " );";
    private static final String SQL_DELETE_PODCAST_TABLE =
            "DROP TABLE IF EXISTS " + PodcastEntry.TABLE_NAME;
    private static final String SQL_DELETE_EPISODE_TABLE =
            "DROP TABLE IF EXISTS " + EpisodeEntry.TABLE_NAME;
    private static final String SQL_DELETE_CATEGORY_TABLE =
            "DROP TABLE IF EXISTS " + CategoryEntry.TABLE_NAME;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Podcastinate.db";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }  // Use this to instantiate DatabaseHelper databaseHelper = new DatabaseHelper(getContext());

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PODCAST);
        db.execSQL(SQL_CREATE_EPISODE);
        db.execSQL(SQL_CREATE_CATEGORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Database is a cache for online podcast data, its upgrade policy
        // could turn out to be "discard the data and start over"
        db.execSQL(SQL_DELETE_PODCAST_TABLE);
        db.execSQL(SQL_DELETE_EPISODE_TABLE);
        db.execSQL(SQL_DELETE_CATEGORY_TABLE);
        onCreate(db);
    }

    public long insertPodcast(String title, String description, String imageDirectory, String link) {
        // Create ContentValues Key-Value pair
        ContentValues contentValues = new ContentValues();
        contentValues.put(PodcastEntry.COLUMN_NAME_TITLE, title);
        contentValues.put(PodcastEntry.COLUMN_NAME_DESCRIPTION, description);
        contentValues.put(PodcastEntry.COLUMN_NAME_IMAGE_DIRECTORY, imageDirectory);
        contentValues.put(PodcastEntry.COLUMN_NAME_link, link);

        SQLiteDatabase database = getWritableDatabase();
        return database.insert(PodcastEntry.TABLE_NAME, PodcastEntry.COLUMN_NAME_TITLE,
                contentValues);
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
        SQLiteDatabase database = getWritableDatabase();
        return database.insert(EpisodeEntry.TABLE_NAME, EpisodeEntry.COLUMN_NAME_TITLE,
                contentValues);
    }

    public int getPodcastID(String podcastTitle) {
        SQLiteDatabase database = getWritableDatabase();
        int podcastId = 0;
        String[] columns = {PodcastEntry.COLUMN_NAME_PODCAST_ID,
                PodcastEntry.COLUMN_NAME_TITLE };
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
        SQLiteDatabase database = getWritableDatabase();
        String[] columns = {
                PodcastEntry.COLUMN_NAME_TITLE
        };
        String sortOrder = PodcastEntry.COLUMN_NAME_TITLE + " DESC";
        return database.query(PodcastEntry.TABLE_NAME, columns, null, null, null, null,
                sortOrder);
    }

    public Cursor getAllEpisodeNames(int podcastID) {
        SQLiteDatabase database = getWritableDatabase();
        String[] columns = {
                EpisodeEntry.COLUMN_NAME_TITLE
        };
        String sortOrder = PodcastEntry.COLUMN_NAME_TITLE + " DESC";
        return database.query(EpisodeEntry.TABLE_NAME, columns,
                EpisodeEntry.COLUMN_NAME_PODCAST_ID + " = " + podcastID, null, null, null,
                sortOrder);
    }
}