package com.shanewhelan.podcastinate.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.shanewhelan.podcastinate.activities.DownloadActivity;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class DownloadService extends IntentService {
    public static final String DIRECTORY = "/Podcastinate";

    public DownloadService() {
        super("Download service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String podcastTitle = intent.getStringExtra(DownloadActivity.PODCAST_TITLE);
        String episodeTitle = intent.getStringExtra(DownloadActivity.EPISODE_TITLE);
        String enclosure = intent.getStringExtra(DownloadActivity.ENCLOSURE);

        String filepath = null;
        try {
            Log.d("sw9", enclosure);

            HttpGet httpGet = new HttpGet(new URI(enclosure));
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse httpResponse = httpClient.execute(httpGet);


            Log.d("sw9", "response code: " + httpResponse.getStatusLine().getStatusCode());

            File externalStorage = new File(Environment.getExternalStorageDirectory() + DIRECTORY);
            if(!externalStorage.isDirectory()) {
                if(!externalStorage.mkdir()) {
                    throw new IOException("Could not create directory");
                }
            }
            String fileNameTemp = episodeTitle.replace(" ", "-");
            fileNameTemp = fileNameTemp.replaceAll("[$&+,:;=?@#|]", "");  // HUGE POINT OF POSSIBLE ERRORS

            String filename = String.valueOf(podcastTitle.charAt(0)) +
                    String.valueOf(podcastTitle.charAt(podcastTitle.length()-1)) + "-" + fileNameTemp + ".mp3";

            Log.d("sw9", filename);
            File podcastFile = new File(externalStorage, filename);

            if(podcastFile.createNewFile()) {

                FileOutputStream fileOutput = new FileOutputStream(podcastFile);
                InputStream inputStream = httpResponse.getEntity().getContent();

                long contentLength = httpResponse.getEntity().getContentLength();
                int downloadedSize = 0;
                byte[] buffer = new byte[32768];
                int bufferLength;
                long start = System.nanoTime();
                while((bufferLength = inputStream.read(buffer)) > 0 ) {
                    fileOutput.write(buffer, 0, bufferLength);
                    downloadedSize += bufferLength;
                    //Log.d("sw9", "Progress: " + "downloadedSize:" + downloadedSize + " totalSize: " + contentLength) ;
                }
                long end = System.nanoTime();
                long timeTaken = end - start;
                Log.d("sw9", "End - Start " + timeTaken);
                inputStream.close();
                fileOutput.close();
                if(downloadedSize == contentLength) {
                    filepath = podcastFile.getAbsolutePath();
                }
            }

        } catch (MalformedURLException e) {
            Log.e("sw9", "Malformed URL " + e.getMessage());
        } catch (IOException e) {
            filepath = null;
            Log.e("sw9", "IO EXCEPTION " + e.getCause().toString());
        } catch (URISyntaxException e) {
            Log.e("sw9", "URI SYNTAX " + e.getMessage());
        }

        Log.d("sw9", "filepath:" + " " + filepath);
    }
}
