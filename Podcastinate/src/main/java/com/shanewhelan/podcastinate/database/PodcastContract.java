package com.shanewhelan.podcastinate.database;

import android.provider.BaseColumns;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public final class PodcastContract {

    public static abstract class PodcastEntry implements BaseColumns {
        public static final String TABLE_NAME = "podcast";
        public static final String PODCAST_ID = "podcast_id";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String IMAGE_DIRECTORY = "image_directory";
        public static final String DIRECTORY = "directory";
        public static final String LINK = "link";
        public static final String COUNT_NEW = "count_new";
    }

    public static abstract class EpisodeEntry implements BaseColumns {
        public static final String TABLE_NAME = "episode";
        public static final String EPISODE_ID = "episode_id";
        public static final String PODCAST_ID = "podcast_id";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String ENCLOSURE = "enclosure";
        public static final String PUB_DATE = "pub_date";
        public static final String DURATION = "duration";
        public static final String GUID = "guid";
        public static final String DIRECTORY = "directory";
        public static final String NEW_EPISODE = "new_episode";
        public static final String CURRENT_TIME = "current_time";
    }

    public static abstract class CategoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "category";
        public static final String CAT_ID = "cat_id";
        public static final String PODCAST_ID = "podcast_id";
        public static final String CATEGORY = "category";
    }
}