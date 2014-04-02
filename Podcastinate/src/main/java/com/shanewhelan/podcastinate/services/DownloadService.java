package com.shanewhelan.podcastinate.services;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.shanewhelan.podcastinate.DownloadListItem;
import com.shanewhelan.podcastinate.Episode;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.activities.DownloadActivity;
import com.shanewhelan.podcastinate.database.PodcastDataSource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class DownloadService extends Service {
    private static ArrayList<DownloadListItem> downloadList;
    private IBinder binder = new DownloadBinder();
    private DownloadManager downloadManager;
    private NotificationCompat.Builder builder;
    private static boolean notificationRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction() != null) {
            if (intent.getAction().equals(Utilities.ACTION_DOWNLOAD)) {
                registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                registerReceiver(receiver, new IntentFilter(Utilities.ACTION_CANCEL));
                registerReceiver(receiver, new IntentFilter(Utilities.ACTION_QUEUED));
                downloadFile(intent);
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
    }

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    @SuppressLint("UseSparseArrays")
    public void downloadFile(Intent intent) {
        String episodeID = intent.getStringExtra(Utilities.EPISODE_ID);
        int podcastID = intent.getIntExtra(Utilities.PODCAST_ID, -1);
        final String podcastTitle = intent.getStringExtra(Utilities.PODCAST_TITLE);

        if(downloadList != null) {
            for (DownloadListItem aDownloadList : downloadList) {
                if (aDownloadList.getEpisode().getEpisodeID() == Integer.parseInt(episodeID)) {
                    // Already in download list, do nothing
                    return;
                }
            }
        }
        PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
        pds.openDbForReading();
        final Episode episode = pds.getEpisodeMetaDataForDownload(episodeID);
        String podcastDirectory = pds.getPodcastDirectory(podcastID);
        pds.closeDb();
        // Format the file name so it looks and works ok
        String filename = formatFileName(podcastTitle, episode.getTitle(), episode.getEnclosure());

        try {
            // Check if default directory exists and create it if not
            File externalStorage = new File(podcastDirectory);
            if (!externalStorage.isDirectory()) {
                if (!externalStorage.mkdir()) {
                    throw new IOException("Could not create directory");
                }
            }
            // Get a file ready for podcast to be fired into
            File podcastFile = new File(externalStorage, filename);

            downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(episode.getEnclosure()));
            request.setDestinationUri(Uri.fromFile(podcastFile));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setVisibleInDownloadsUi(false);
            final long queueID = downloadManager.enqueue(request);

            episode.setDirectory(podcastFile.getAbsolutePath());

            if(downloadList == null) {
                downloadList = new ArrayList<DownloadListItem>();
            }

            downloadList.add(new DownloadListItem(episode, queueID, podcastTitle));
            sendBroadcast(new Intent(Utilities.ACTION_QUEUED));

        } catch (IOException e) {
            Utilities.logException(e);
        }
    }

    public String formatFileName(String podcastTitle, String episodeTitle, String enclosure) {
        // Create a custom filename in case feeds have the same episode name, I must read up
        // on the performance cost of regular expressions.

        // Firstly remove special characters from Episode title so filename will be valid
        String fileNameTemp;
        if (episodeTitle != null) {
            fileNameTemp = episodeTitle.replaceAll("[^A-Za-z0-9-]", "");
        } else {
            Random rand = new Random();
            fileNameTemp = "R" + rand.nextInt(10000000);
        }

        String filename;
        if (podcastTitle != null) {
            podcastTitle = podcastTitle.replaceAll("[^A-Za-z0-9-]", "");
            // Now take the first and last characters of cleaned up podcast title and prepend them to file name
            filename = String.valueOf(podcastTitle.charAt(0)) +
                    String.valueOf(podcastTitle.charAt(podcastTitle.length() - 1)) + "-" + fileNameTemp;
        } else {
            filename = "RP" + "-" + fileNameTemp;
        }

        // Get podcast file extension
        // TODO handle all extension types
        if (enclosure != null) {
            int indexOfExtension = enclosure.lastIndexOf(".");
            filename = filename + enclosure.substring(indexOfExtension, indexOfExtension + 4);
        }

        Log.d("sw9", filename);
        return filename;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                downloadComplete(intent);
            } else if (Utilities.ACTION_CANCEL.equals(intent.getAction())) {
                downloadCancelled(intent);
            } else if (Utilities.ACTION_QUEUED.equals(intent.getAction())) {
                pollDMForNotification();
            }
        }
    };

    private void downloadComplete(Intent intent) {
        // Get downloadId from extras of intent
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        // Get the episode object that is associated with the downloadId
        Episode episode = null;

        int listIndexItemRemove = -1;
        for (int i = 0; i < downloadList.size(); i++ ) {
            if (downloadList.get(i).getQueueID() == downloadId) {
                episode = downloadList.get(i).getEpisode();
                listIndexItemRemove = i;
                break;
            }
        }

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(
                        cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {

                    if(episode != null) {
                        PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
                        // Set Episode directory
                        pds.openDbForWriting();
                        pds.updateEpisodeDirectory(episode.getEpisodeID(), episode.getDirectory());

                        // Recheck if episode new status changed
                        episode.setNew(pds.getEpisodeIsNew(episode.getEpisodeID()));
                        // Update count new while we are at it
                        if (!episode.isNew()) {
                            // While we are at it update the isNew fields in DB, DB instance is only opened once this way
                            pds.updateEpisodeIsNew(episode.getEpisodeID(), 1);
                            int countNew = pds.getCountNew(episode.getPodcastID());
                            if (countNew > 0) {
                                pds.updatePodcastCountNew(episode.getPodcastID(), countNew + 1);
                            }
                        }
                        pds.closeDb();

                        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        notifyManager.cancel(1);

                        downloadList.remove(listIndexItemRemove);

                        Intent iComplete = new Intent(Utilities.ACTION_DOWNLOADED);
                        sendBroadcast(iComplete);
                    }
                }
            }
        }
    }

    private void downloadCancelled(Intent intent) {
        int indexItemRemoved = -1;
        int numRemoved;
        long idCancelEpisode = -1;
        for(int i = 0; i < downloadList.size(); i++) {
            if(downloadList.get(i).getEpisode().getEpisodeID() == intent.getIntExtra(Utilities.EPISODE_ID, -1)) {
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(1);
                idCancelEpisode = downloadList.get(i).getQueueID();
                indexItemRemoved = i;
                break;
            }
        }

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(idCancelEpisode);
        Cursor cursor = downloadManager.query(query);
        if (cursor != null) {
            if(cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    // TODO cancel in any status
                    if (DownloadManager.STATUS_RUNNING == cursor.getInt(
                            cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {

                        int bytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        if (bytes > 2097152) {
                            numRemoved = downloadManager.remove(idCancelEpisode);

                            if (numRemoved > 0) {
                                downloadList.remove(indexItemRemoved);

                                NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                notifyManager.cancel(1);
                                Intent iCanceled = new Intent(Utilities.ACTION_CANCEL_COMPLETE);
                                sendBroadcast(iCanceled);
                            }
                        }
                    }
                }
            }
            cursor.close();
        }
    }

    private void pollDMForNotification() {
        if(!notificationRunning) {
            // Start a new thread that runs until the download queue is empty
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(getApplicationContext() != null) {
                        // Set a boolean that the thread is running, set to false at the end
                        notificationRunning = true;
                        // PendingIntent to fire on notification click
                        PendingIntent pendIntent = PendingIntent.getActivity(getApplicationContext(), 1,
                                new Intent(getApplicationContext(), DownloadActivity.class), 0);

                        // Initialise NotificationManager
                        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        // Initialise variable out of loop for memory management
                        long currentProgress;
                        long totalDownloadSize;


                        // Keep looping while list is populated with anything
                        while (downloadList.size() > 0) {
                            // Get the download queryID of each item in the list and query the running
                            // downloads
                            // Get the download queryID of each item in the list and query the running
                            // downloads
                            long[] arrayOfIds = new long[downloadList.size()];
                            for (int i = 0; i < downloadList.size(); i++) {
                                arrayOfIds[i] = downloadList.get(i).getQueueID();
                            }

                            DownloadManager.Query query = new DownloadManager.Query();
                            query.setFilterById(arrayOfIds);
                            Cursor cursor = downloadManager.query(query);

                            if (cursor != null) {
                                if (cursor.getCount() == 1) {
                                    if (cursor.moveToFirst()) {
                                        if (DownloadManager.STATUS_RUNNING == cursor.getInt(
                                                cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                                            totalDownloadSize = cursor.getLong(
                                                    cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                                            currentProgress = cursor.getLong(
                                                    cursor.getColumnIndex(DownloadManager.
                                                            COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                            );

                                            long queueID = cursor.getLong(cursor.
                                                    getColumnIndex(DownloadManager.COLUMN_ID));

                                            for (DownloadListItem aDownloadList : downloadList) {
                                                if (aDownloadList.getQueueID() == queueID) {
                                                    builder = new NotificationCompat.Builder(getApplicationContext());
                                                    builder.setContentTitle(aDownloadList.getEpisode().getTitle()); // First row
                                                    builder.setSmallIcon(R.drawable.ic_action_download_notification);

                                                    int totalDownloadInt = Utilities.safeLongToInt(totalDownloadSize);
                                                    int progressInt = Utilities.safeLongToInt(currentProgress);

                                                    if (progressInt > 0 && totalDownloadInt > 0) {
                                                        double percent = ((double) progressInt / (double) totalDownloadInt) * 100;
                                                        builder.setContentText((int) percent + "% Complete");
                                                        builder.setProgress(totalDownloadInt, progressInt, false);
                                                    }
                                                    builder.setContentIntent(pendIntent);
                                                    notifyManager.notify(1, builder.build());
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } else if (cursor.getCount() > 1) {
                                    long mTotalDownloadSize = 0;
                                    long mCurrentProgress = 0;
                                    int totalDownloadInt = 0;
                                    int progressInt = 0;
                                    while (cursor.moveToNext()) {
                                        if (DownloadManager.STATUS_RUNNING == cursor.getInt(
                                                cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                                            mTotalDownloadSize += cursor.getLong(
                                                    cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                                            mCurrentProgress += cursor.getLong(
                                                    cursor.getColumnIndex(DownloadManager.
                                                            COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                            );

                                            long queueID = cursor.getLong(cursor.
                                                    getColumnIndex(DownloadManager.COLUMN_ID));

                                            for (DownloadListItem aDownloadList : downloadList) {
                                                if (aDownloadList.getQueueID() == queueID) {
                                                    totalDownloadInt += Utilities.safeLongToInt(mTotalDownloadSize);
                                                    progressInt += Utilities.safeLongToInt(mCurrentProgress);
                                                }
                                            }
                                        }
                                    }

                                    builder = new NotificationCompat.Builder(getApplicationContext());
                                    builder.setContentTitle("Multiple Downloads"); // First row
                                    builder.setSmallIcon(R.drawable.ic_action_download_notification);

                                    if (progressInt > 0 && totalDownloadInt > 0) {
                                        double percent = ((double) progressInt / (double) totalDownloadInt) * 100;
                                        builder.setContentText((int) percent + "% Complete");
                                        builder.setProgress(totalDownloadInt, progressInt, false);
                                    }

                                    builder.setContentIntent(pendIntent);
                                    notifyManager.notify(1, builder.build());

                                    builder.setContentIntent(pendIntent);
                                    notifyManager.notify(1, builder.build());
                                }
                                cursor.close();
                            }
                            try {
                                Thread.sleep(1000, 0);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        // If while loop finishes then no notification running
                        notificationRunning = false;
                    }
                }
            }).start();
        }
    }

    public ArrayList<DownloadListItem> getDownloadList() {
        return downloadList;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void oldMethodOfDownload(Intent intent) {
        /*
        String episodeID = intent.getStringExtra(Utilities.EPISODE_ID);
        int podcastID = intent.getIntExtra(Utilities.PODCAST_ID, -1);
        String podcastTitle = intent.getStringExtra(Utilities.PODCAST_TITLE);

        PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
        pds.openDbForReading();
        Episode episode = pds.getEpisodeMetaDataForDownload(episodeID);
        String podcastDirectory = pds.getPodcastDirectory(podcastID);
        pds.closeDb();

        String episodeTitle = episode.getTitle();
        String enclosure = episode.getEnclosure();

        String directory = null;
        try {
            // Download podcast file
            HttpGet httpGet = new HttpGet(new URI(enclosure));
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse httpResponse = httpClient.execute(httpGet);

            // Exception handle the fact that server could be down
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                int duration = Toast.LENGTH_LONG;
                if (getApplicationContext() != null) {
                    Toast.makeText(getApplicationContext(), "HTTP Error - Could not download file",
                            duration).show();
                }
                // Throw custom exception
                throw new HTTPConnectionException(responseCode);
            }

            // Check if default directory exists and create it if not.
            File externalStorage = new File(podcastDirectory);
            if (!externalStorage.isDirectory()) {
                if (!externalStorage.mkdir()) {
                    throw new IOException("Could not create directory");
                }
            }

            // Create a custom filename in case feeds have the same episode name, I must read up
            // on the performance cost of this.
            String fileNameTemp;
            if (episodeTitle != null) {
                fileNameTemp = episodeTitle.replaceAll("[^A-Za-z0-9-]", "");
            } else {
                Random rand = new Random();
                fileNameTemp = "R" + rand.nextInt(10000000);
            }

            String filename;
            if (podcastTitle != null) {
                filename = String.valueOf(podcastTitle.charAt(0)) +
                        String.valueOf(podcastTitle.charAt(podcastTitle.length() - 1)) + "-" + fileNameTemp;
            } else {
                filename = "RP" + "-" + fileNameTemp;
            }

            // Get podcast file extension
            if (enclosure != null) {
                int indexOfExtension = enclosure.lastIndexOf(".");
                filename = filename + enclosure.substring(indexOfExtension, indexOfExtension + 4);
            }

            Log.d("sw9", filename);
            File podcastFile = new File(externalStorage, filename);

            // Create new episode from InputStream
            if (podcastFile.createNewFile()) {
                FileOutputStream fileOutput = new FileOutputStream(podcastFile);
                InputStream inputStream = httpResponse.getEntity().getContent();

                // Stats for downloading
                long contentLength = httpResponse.getEntity().getContentLength();
                double contentLengthInt = (int) contentLength;
                double downloadedSize = 0;
                byte[] buffer = new byte[32768];
                int bufferLength;
                // Start timer for download
                long start = System.nanoTime();

                PendingIntent pendIntent = PendingIntent.getActivity(this, 1,
                        new Intent(getApplicationContext(), DownloadActivity.class), 0);

                // Initialise Notification and Builder
                notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                builder = new Builder(this);
                builder.setContentTitle(episodeTitle)
                        .setContentText("Download in progress")
                        .setSmallIcon(R.drawable.ic_action_download_notification)
                        .setContentIntent(pendIntent);

                // Use count to make sure we only update the progress bar 50 times in total
                double count = 0;
                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);
                    downloadedSize += bufferLength;
                    // Download progress as a percentage
                    dlProgress = ((downloadedSize / contentLengthInt) * 100);

                    if (dlProgress > count) {
                        // Number we add on here is how frequently we want the progress bar to update
                        count = dlProgress + 2;
                        // Set off a new thread to update the Notification progress bar
                        new Thread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        builder.setProgress(100, (int) dlProgress, false);
                                        notifyManager.notify(1, builder.build());
                                    }
                                }
                        ).start();
                    }
                }
                long end = System.nanoTime();
                long timeTaken = end - start;
                Log.d("sw9", "End - Start " + timeTaken);

                notifyManager.cancel(1);

                // Tidy up and close streams
                inputStream.close();
                fileOutput.close();

                if (downloadedSize == contentLength) {
                    directory = podcastFile.getAbsolutePath();
                }
                // Set Episode directory
                pds.openDbForWriting();
                pds.updateEpisodeDirectory(enclosure, directory);

                // Recheck if episode new status changed
                episode.setNew(pds.getEpisodeIsNew(episode.getEpisodeID()));
                // Update count new while we are at it
                if(!episode.isNew()) {
                    // While we are at it update the isNew fields in DB, DB instance is only opened once this way
                    pds.updateEpisodeIsNew(episode.getEpisodeID(), 1);
                    int countNew = pds.getCountNew(episode.getPodcastID());
                    if (countNew > 0) {
                        pds.updatePodcastCountNew(episode.getPodcastID(), countNew + 1);
                    }
                }

                pds.closeDb();

                Intent iComplete = new Intent();
                iComplete.setAction(Utilities.ACTION_DOWNLOADED);
                sendBroadcast(iComplete);
            }
        } catch (MalformedURLException e) {
            Utilities.logException(e);
        } catch (IOException e) {
            Utilities.logException(e);
        } catch (URISyntaxException e) {
            Utilities.logException(e);
        }
        */
    }
}
