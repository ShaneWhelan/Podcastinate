package com.shanewhelan.podcastinate;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.*;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.shanewhelan.podcastinate.activities.DownloadActivity;
import com.shanewhelan.podcastinate.database.PodcastContract.*;

/**
 * Created by Shane on 16/01/14. Podcastinate.
 */
public class EpisodeAdapter extends CursorAdapter implements OnClickListener {
    private final LayoutInflater layoutInflater;
    public Context context;
    private String podcastTitle;

    public EpisodeAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        layoutInflater = LayoutInflater.from(context);
        this.context = context;
    }

    public void setEpisodeName(String episodeName) {
        this.podcastTitle = episodeName;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String episodeName = cursor.getString(cursor.getColumnIndex(EpisodeEntry.COLUMN_NAME_TITLE));
        TextView episodeNameView = (TextView) view.findViewById(R.id.episodeName);
        episodeNameView.setText(episodeName);

        ImageButton downloadButton = (ImageButton) view.findViewById(R.id.download_icon);
        ImageButton playButton = (ImageButton) view.findViewById(R.id.play_icon);
        downloadButton.setContentDescription(episodeName);
        downloadButton.setOnClickListener(this);
        if(cursor.getString(cursor.getColumnIndex(EpisodeEntry.COLUMN_NAME_DIRECTORY)) != null) {
            downloadButton.setVisibility(View.GONE);
            playButton.setVisibility(View.VISIBLE);
        }else{
            playButton.setVisibility(View.GONE);
            downloadButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return layoutInflater.inflate(R.layout.episode_list_item, parent, false);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(context, DownloadActivity.class);
        intent.putExtra(DownloadActivity.EPISODE_TITLE, v.getContentDescription());
        intent.putExtra(DownloadActivity.PODCAST_TITLE, podcastTitle);
        context.startActivity(intent);
    }
}
