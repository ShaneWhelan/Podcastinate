package com.shanewhelan.podcastinate.database;

import android.provider.BaseColumns;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public final class PodcastContract {

    public PodcastContract(){
    }

    public static abstract class PodcastEntry implements BaseColumns {
        public static final String TABLE_NAME = "podcast";
        public static final String COLUMN_NAME_PODCAST_ID = "podcast_id";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_IMAGE_DIRECTORY = "image_directory";
        public static final String COLUMN_NAME_link = "link";
    }

    public static abstract class EpisodeEntry implements BaseColumns{
        public static final String TABLE_NAME = "episode";
        public static final String COLUMN_NAME_EPISODE_ID = "episode_id";
        public static final String COLUMN_NAME_LISTENED = "listened";
        public static final String COLUMN_NAME_CURRENT_TIME = "current_time";
        public static final String COLUMN_NAME_PODCAST_ID = "podcast_id";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_EPISODE_LINK = "episode_link";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_PUB_DATE= "pub_date";
        public static final String COLUMN_NAME_GUID = "guid";
        public static final String COLUMN_NAME_DURATION = "duration";
        public static final String COLUMN_NAME_IMAGE_DIRECTORY = "image_directory";
        public static final String COLUMN_NAME_ENCLOSURE = "enclosure";
    }

    public static abstract class CategoryEntry implements BaseColumns{
        public static final String TABLE_NAME = "category";
        public static final String COLUMN_NAME_CAT_ID = "cat_id";
        public static final String COLUMN_NAME_NAME = "name";
    }
}