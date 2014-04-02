package com.shanewhelan.podcastinate.async;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageButton;

import com.shanewhelan.podcastinate.Utilities;

import java.io.File;

public class LoadImageFromDisk extends AsyncTask<String, Void, Bitmap> {
    private ImageButton imageButton;

    public LoadImageFromDisk(ImageButton imageButton) {
        this.imageButton = imageButton;
    }

    protected Bitmap doInBackground(String... directory) {
        return Utilities.decodeFile(new File(directory[0]));
    }

    protected void onPostExecute(Bitmap result) {
        imageButton.setImageBitmap(result);
    }
}