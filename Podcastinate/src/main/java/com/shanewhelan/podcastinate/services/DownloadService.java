package com.shanewhelan.podcastinate.services;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification.*;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.activities.DownloadActivity;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.exceptions.HTTPConnectionException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;

public class DownloadService extends IntentService {
    public static final String DIRECTORY = "/Podcastinate";
    private NotificationManager notifyManager;
    private Builder builder;
    private double dlProgress;



    public DownloadService() {
        super("Download service");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onHandleIntent(Intent intent) {
        String podcastTitle = intent.getStringExtra(DownloadActivity.PODCAST_TITLE);
        String episodeTitle = intent.getStringExtra(DownloadActivity.EPISODE_TITLE);
        String enclosure = intent.getStringExtra(DownloadActivity.ENCLOSURE);

        String directory = null;
        try {
            // Download podcast file
            HttpGet httpGet = new HttpGet(new URI(enclosure));
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse httpResponse = httpClient.execute(httpGet);

            // Exception handle the fact that server could be down
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if(responseCode != 200) {
                int duration = Toast.LENGTH_LONG;
                if (getApplicationContext() != null) {
                    Toast.makeText(getApplicationContext(), "HTTP Error - Could not download file",
                            duration).show();
                }
                // Throw custom exception
                throw new HTTPConnectionException(responseCode);
            }

            // Check if default directory exists and create it if not.
            File externalStorage = new File(Environment.getExternalStorageDirectory() + DIRECTORY);
            if(!externalStorage.isDirectory()) {
                if(!externalStorage.mkdir()) {
                    throw new IOException("Could not create directory");
                }
            }

            // Create a custom filename in case feeds have the same episode name, I must read up
            // on the performance cost of this.
            String fileNameTemp;
            if (episodeTitle != null) {
                fileNameTemp = episodeTitle.replace(" ", "-");
                fileNameTemp = fileNameTemp.replaceAll("[$&+,:;=?!@#|]", ""); // Check this regex
            } else {
                Random rand = new Random();
                fileNameTemp = "R" + rand.nextInt(10000000);
            }


            String filename;
            if (podcastTitle != null) {
                filename = String.valueOf(podcastTitle.charAt(0)) +
                        String.valueOf(podcastTitle.charAt(podcastTitle.length()-1)) + "-" + fileNameTemp + ".mp3";
            } else {
                filename = "RP" + "-" + fileNameTemp + ".mp3";
            }

            Log.d("sw9", filename);
            File podcastFile = new File(externalStorage, filename);

            // Create new episode from InputStream
            if(podcastFile.createNewFile()) {
                FileOutputStream fileOutput = new FileOutputStream(podcastFile);
                InputStream inputStream = httpResponse.getEntity().getContent();

                // Stats for downloading
                long contentLength = httpResponse.getEntity().getContentLength();
                double contentLengInt = (int) contentLength;
                double downloadedSize = 0;
                byte[] buffer = new byte[32768];
                int bufferLength;
                // Start timer for download
                long start = System.nanoTime();

                // Initialise Notification and Builder
                notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                builder = new Builder(this);
                builder.setContentTitle(episodeTitle).setContentText("Download in progress")
                        .setSmallIcon(R.drawable.ic_action_download);

                // Use count to make sure we only update the progress bar 50 times in total
                double count = 0;
                while((bufferLength = inputStream.read(buffer)) > 0 ) {
                    fileOutput.write(buffer, 0, bufferLength);
                    downloadedSize += bufferLength;
                    // Download progress as a percentage
                    dlProgress = ((downloadedSize /contentLengInt)*100);

                    if(dlProgress > count) {
                        // Number we add on here is how frequently we want the progress bar to update
                        count = dlProgress + 2;
                        // Set off a new thread to update the Notification progress bar
                        new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    builder.setProgress(100, (int) dlProgress, false);
                                    notifyManager.notify(0, builder.build());
                                }
                            }
                        ).start();
                    }
                }
                long end = System.nanoTime();
                long timeTaken = end - start;
                Log.d("sw9", "End - Start " + timeTaken);

                builder.setContentText("Download Complete").setProgress(0, 0, false);
                notifyManager.notify(0, builder.build());

                // Tidy up and close streams
                inputStream.close();
                fileOutput.close();

                if(downloadedSize == contentLength) {
                    directory = podcastFile.getAbsolutePath();
                }

                PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
                pds.openDb();
                pds.updateEpisodeDirectory(enclosure, directory);
                pds.closeDb();
                // Update DB that file is downloaded with the path
                Log.d("sw9", "filepath:" + " " + directory);
            }
        } catch (MalformedURLException e) {
            Log.e("sw9", "Malformed URL " + e.getMessage());
        } catch (IOException e) {
            Log.e("sw9", "IO EXCEPTION " + e.getCause().toString());
        } catch (URISyntaxException e) {
            Log.e("sw9", "URI SYNTAX " + e.getMessage());
        }
    }
}
