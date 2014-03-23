package com.shanewhelan.podcastinate.async;

/**
 * Created by Shane on 19/03/14. Podcastinate.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

import com.shanewhelan.podcastinate.Utilities;

import java.io.InputStream;

public class DownloadImagesAsyncTask extends AsyncTask<String, Void, Bitmap> {
    private ImageView podcastImage;
    private int position;
    private Bitmap[] bitmapList;

    public DownloadImagesAsyncTask(ImageView podcastImage, int position, Bitmap[] bitmapList) {
        this.podcastImage = podcastImage;
        this.position = position;
        this.bitmapList = bitmapList;
    }

    protected Bitmap doInBackground(String... urls) {
        Bitmap podcastBitmap = null;
        try {
            InputStream inStream = new java.net.URL(urls[0]).openStream();
            podcastBitmap = BitmapFactory.decodeStream(inStream);
        } catch (Exception e) {
            Utilities.logException(e);
        }
        return podcastBitmap;
    }

    protected void onPostExecute(Bitmap podcastBitmap) {
        bitmapList[position] = podcastBitmap;
        // Multi threading check to see if the image view has changed position
        if(podcastImage.getContentDescription() != null) {
            if(podcastImage.getContentDescription().toString().equals("" + position)) {
                podcastImage.setImageBitmap(podcastBitmap);
                podcastImage.setVisibility(View.VISIBLE);
            }
        }
    }
}