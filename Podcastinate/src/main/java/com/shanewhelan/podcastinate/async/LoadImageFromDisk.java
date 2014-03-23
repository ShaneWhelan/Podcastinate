package com.shanewhelan.podcastinate.async;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageButton;

/**
 * Created by Shane on 23/03/14. Podcastinate.
 */
public class LoadImageFromDisk extends AsyncTask<String, Void, Bitmap> {
    private ImageButton imageButton;

    public LoadImageFromDisk(ImageButton imageButton) {
        this.imageButton = imageButton;
    }

    protected Bitmap doInBackground(String... directory) {
        return BitmapFactory.decodeFile(directory[0]);
    }

    protected void onPostExecute(Bitmap result) {
        imageButton.setImageBitmap(result);
    }
}