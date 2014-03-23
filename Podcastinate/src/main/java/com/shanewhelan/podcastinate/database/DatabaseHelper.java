package com.shanewhelan.podcastinate.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.shanewhelan.podcastinate.database.PodcastContract.CategoryEntry;
import com.shanewhelan.podcastinate.database.PodcastContract.EpisodeEntry;
import com.shanewhelan.podcastinate.database.PodcastContract.PodcastEntry;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ", ";
    private static final String SQL_CREATE_PODCAST =
            "CREATE TABLE " + PodcastEntry.TABLE_NAME + " (" +
                    PodcastEntry.PODCAST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    PodcastEntry.TITLE + TEXT_TYPE + COMMA_SEP +
                    PodcastEntry.DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                    PodcastEntry.IMAGE_DIRECTORY + TEXT_TYPE + COMMA_SEP +
                    PodcastEntry.DIRECTORY + TEXT_TYPE + COMMA_SEP +
                    PodcastEntry.LINK + TEXT_TYPE + " UNIQUE" + COMMA_SEP +
                    PodcastEntry.COUNT_NEW + " INTEGER" +
                    " );";
    private static final String SQL_CREATE_EPISODE =
            "CREATE TABLE " + EpisodeEntry.TABLE_NAME + " (" +
                    EpisodeEntry.EPISODE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    EpisodeEntry.PODCAST_ID + " INTEGER" + COMMA_SEP +
                    EpisodeEntry.TITLE + TEXT_TYPE + COMMA_SEP +
                    EpisodeEntry.DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                    EpisodeEntry.ENCLOSURE + TEXT_TYPE + " UNIQUE" + COMMA_SEP +
                    EpisodeEntry.PUB_DATE + TEXT_TYPE + COMMA_SEP +
                    EpisodeEntry.DURATION + TEXT_TYPE + COMMA_SEP +
                    EpisodeEntry.GUID + TEXT_TYPE + COMMA_SEP +
                    EpisodeEntry.DIRECTORY + TEXT_TYPE + COMMA_SEP +
                    EpisodeEntry.NEW_EPISODE + " INTEGER" + COMMA_SEP + // 1 for true, 0 for false
                    EpisodeEntry.CURRENT_TIME + " INTEGER" + COMMA_SEP +
                    "FOREIGN KEY(" + EpisodeEntry.PODCAST_ID + ") REFERENCES " +
                    PodcastEntry.TABLE_NAME + "(" + PodcastEntry.PODCAST_ID + ") " +
                    " );";
    private static final String SQL_CREATE_CATEGORY =
            "CREATE TABLE " + CategoryEntry.TABLE_NAME + " (" +
                    CategoryEntry.CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    CategoryEntry.PODCAST_ID + " INTEGER" + COMMA_SEP +
                    CategoryEntry.CATEGORY + " TEXT" + COMMA_SEP +
                    "FOREIGN KEY(" + CategoryEntry.PODCAST_ID + ") REFERENCES " +
                    PodcastEntry.TABLE_NAME + "(" + PodcastEntry.PODCAST_ID + ") " +
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
}