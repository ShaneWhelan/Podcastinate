package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.shanewhelan.podcastinate.ParseRSS;
import com.shanewhelan.podcastinate.Podcast;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.PodcastContract.PodcastEntry;
import com.shanewhelan.podcastinate.database.PodcastDataSource;
import com.shanewhelan.podcastinate.exceptions.HTTPConnectionException;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
TODO: Add picture beside podcast name
TODO: Create Download Queue
TODO: Add Paging to podcast viewing activity to
TODO: Add long press options (Maybe refresh individual feeds, mark done/new, add to playlist, sort options, force update of thumnail)
TODO: Streaming: Must keep WIFI from sleeping
TODO: Help Section
TODO: Start Main activity if subscribed
TODO: On delete of podcast release media player
TODO: On delete clear current time
TODO: Populate is Listened DB entry
TODO: When start player activity on pause the seekbar and timer are blank.
TODO: Test Other app taking over audio on my app
TODO: Test what happens when podcast ends
TODO: Delete Podcasts
TODO: CLICK RSS LINK GO TO MY APP
 */

public class MainActivity extends Activity {
    private PodcastDataSource dataSource;
    private SimpleCursorAdapter simpleCursorAdapter;
    private Cursor allPodcastNames;
    private MenuItem refreshAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) throws NullPointerException {
        super.onCreate(savedInstanceState);
        this.setTitle("Podcasts");
        setContentView(R.layout.activity_main);

        // TODO: Dev only, take out for release
        try {
            copyAppDbToDownloadFolder();
        } catch (IOException e) {
            Utilities.logException(e);
        }

        dataSource = new PodcastDataSource(this);

        dataSource.openDb();
        allPodcastNames = dataSource.getAllPodcastTitles();
        String[] fromColumns = new String[]{PodcastEntry.TITLE};
        int[] toViews = new int[]{R.id.podcastName};
        simpleCursorAdapter = new SimpleCursorAdapter(this, R.layout.podcast_list_item,
                allPodcastNames, fromColumns, toViews, 0);

        ListView listView = (ListView) findViewById(R.id.listOfPodcasts);
        listView.setAdapter(simpleCursorAdapter);
        dataSource.closeDb();

        OnItemClickListener itemCLickHandler = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = ((TextView) view.findViewById(R.id.podcastName));
                if (textView.getText() != null) {
                    viewEpisode(textView.getText().toString());
                }
            }
        };
        listView.setOnItemClickListener(itemCLickHandler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.add_feed_action:
                Intent intent = new Intent(this, SubscribeActivity.class);
                startActivityForResult(intent, 1);
                return true;
            case R.id.action_settings:

                return true;
            case R.id.action_refresh:
                refreshAction = item;
                if (Utilities.testNetwork(this)) {
                    PodcastDataSource dataSource = new PodcastDataSource(getApplicationContext());
                    dataSource.openDb();
                    HashMap<String, String> podcastInfo = dataSource.getAllPodcastTitlesLinks();
                    dataSource.closeDb();
                    RefreshRSSFeed refreshFeed = new RefreshRSSFeed();
                    if (podcastInfo != null) {
                        //noinspection unchecked
                        refreshFeed.execute(podcastInfo);
                    }
                    return true;
                } else {
                    return false;
                }
            case R.id.action_wipe_db:
                wipeDb();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                updateListOfPodcasts();
            }
        }
    }

    public void updateListOfPodcasts() {
        dataSource.openDb();
        allPodcastNames = dataSource.getAllPodcastTitles();
        simpleCursorAdapter.swapCursor(allPodcastNames);
        simpleCursorAdapter.notifyDataSetChanged();
        dataSource.closeDb();
    }

    public void wipeDb() {
        // Only to be left in developer version - wipes db and external storage directory
        PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
        pds.openDb();
        pds.upgradeDB();
        pds.closeDb();
        Log.d("sw9", Environment.getExternalStorageDirectory().getAbsolutePath() + "/Podcastinate");
        File externalDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Podcastinate");

        if (externalDir.exists()) {
            String deleteCmd = "rm -r " + externalDir.getAbsolutePath();
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
                Log.d("sw9", "Directory deleted");
            } catch (IOException e) {
                Utilities.logException(e);
            }
        }
    }

    public void viewEpisode(String podcastChosen) {
        Intent intent = new Intent(this, PodcastViewerActivity.class);
        intent.putExtra(Utilities.PODCAST_TITLE, podcastChosen);
        startActivity(intent);
    }

    public void copyAppDbToDownloadFolder() throws IOException {
        File backupDB = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "backup.db"); // for example "my_data_backup.db"
        File currentDB = this.getDatabasePath("Podcastinate.db"); //databaseName=your current application database name, for example "my_data.db"
        if (currentDB.exists()) {
            FileChannel src = new FileInputStream(currentDB).getChannel();
            FileChannel dst = new FileOutputStream(backupDB).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            // TODO: Service connection leak here
            //MediaScannerConnection.scanFile(this, new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/backup.db"}, null, null);
        }
    }

    public class RefreshRSSFeed extends AsyncTask<HashMap<String, String>, Void, HashMap<String, String>> {

        @Override
        protected void onPreExecute() {
            refreshAction.setActionView(R.layout.action_progress_bar);
            refreshAction.expandActionView();
        }

        @Override
        protected HashMap<String, String> doInBackground(HashMap<String, String>... urlList) {
            HashMap<String, String> resultMap = new HashMap<String, String>(urlList[0].size());
            try {
                Set entrySet = urlList[0].entrySet();
                int result;
                for (Object anEntrySet : entrySet) {
                    Map.Entry mapEntry = (Map.Entry) anEntrySet;
                    result = refreshRSSFeed(mapEntry.getValue().toString(), mapEntry.getKey().toString());
                    resultMap.put(mapEntry.getValue().toString(), String.valueOf(result));
                }
                return resultMap;
            } catch (HTTPConnectionException e) {
                Utilities.logException(e);
                resultMap.put("error", "Connection Error " + e.getResponseCode());
                return resultMap;
            } catch (IOException e) {
                Utilities.logException(e);
                resultMap.put("error", "Fail on ic_download RSS Feed, ERROR DUMP: " + e.getMessage() + " " + e.getClass());
                return resultMap;
            }
        }

        @Override
        protected void onPostExecute(HashMap<String, String> resultMap) {
            // Replace the progress bar with the refresh button again
            refreshAction.collapseActionView();
            refreshAction.setActionView(null);

            Set entrySet = resultMap.entrySet();
            int numNewEpisodes = 0;
            String error = null;
            for (Object anEntrySet : entrySet) {
                Map.Entry mapEntry = (Map.Entry) anEntrySet;
                if (mapEntry.getValue() == String.valueOf(Utilities.SUCCESS)) {
                    numNewEpisodes++;
                } else if (mapEntry.getKey().toString().equals("error")) {
                    error = mapEntry.getValue().toString();
                }
            }

            if (getApplicationContext() != null) {
                if (numNewEpisodes > 1) {
                    Toast.makeText(getApplicationContext(), numNewEpisodes + " new episodes.", Toast.LENGTH_LONG).show();
                } else if (numNewEpisodes == 1) {
                    Toast.makeText(getApplicationContext(), numNewEpisodes + " new episode.", Toast.LENGTH_LONG).show();
                } else if (numNewEpisodes == 0) {
                    Toast.makeText(getApplicationContext(), "No new episodes", Toast.LENGTH_LONG).show();
                } else if (error != null) {
                    Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
                }
            }
        }

        private int refreshRSSFeed(String url, String podcastTitle) throws IOException {
            InputStream inputStream = null;
            int response;
            try {
                URL feedURL = new URL(url);
                HttpURLConnection httpCon = (HttpURLConnection) feedURL.openConnection();
                httpCon.setReadTimeout(100000);
                httpCon.setConnectTimeout(150000);
                httpCon.setRequestMethod("GET");
                httpCon.setUseCaches(true);
                httpCon.addRequestProperty("Content-Type", "text/xml; charset=utf-8");
                httpCon.setDoInput(true);
                httpCon.connect();
                response = httpCon.getResponseCode();

                if (response == 200) {
                    //TODO STOP HERE
                    inputStream = httpCon.getInputStream();
                } else {
                    throw new HTTPConnectionException(response);
                }

                ParseRSS parseRSS = new ParseRSS();
                XmlPullParser xmlPullParser = parseRSS.inputStreamToPullParser(inputStream);
                if (xmlPullParser != null) {
                    PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
                    pds.openDb();
                    String enclosure = pds.getMostRecentEpisodeEnclosure(podcastTitle);
                    pds.closeDb();
                    Podcast podcast = parseRSS.checkForNewEntries(xmlPullParser, enclosure, podcastTitle);

                    if (podcast != null) {
                        if (podcast.getEpisodeList().size() > 0) {
                            Utilities.savePodcastToDb(getApplicationContext(), podcast, false);
                            return Utilities.SUCCESS;
                        } else {
                            return Utilities.NO_NEW_EPISODES;
                        }
                    } else {
                        // Won't be false unless parser threw exception, causing podcast to be null
                        return Utilities.FAILURE_TO_PARSE;
                    }
                }

            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Utilities.logException(e);
                    }
                }
            }
            return Utilities.INVALID_URL;
        }
    }
}
