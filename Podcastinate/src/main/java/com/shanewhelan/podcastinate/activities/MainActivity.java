package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.shanewhelan.podcastinate.ParseRSS;
import com.shanewhelan.podcastinate.Podcast;
import com.shanewhelan.podcastinate.R;
import com.shanewhelan.podcastinate.Utilities;
import com.shanewhelan.podcastinate.database.DatabaseHelper;
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
TODO: Add Refresh button
TODO: Add picture beside podcast name
TODO: Add long press options (Maybe refresh individual feeds, mark done/new, add to playlist, sort options, force update of thumnail
TODO: Use date format in sql database, fix columns episode link
TODO: GET file extension from feed
TODO: Streaming: Must keep WIFI from sleeping
 */

public class MainActivity extends Activity {
    private PodcastDataSource dataSource;
    private SimpleCursorAdapter simpleCursorAdapter;
    private Cursor allPodcastNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) throws NullPointerException {
        super.onCreate(savedInstanceState);
        this.setTitle("Podcasts");
        setContentView(R.layout.activity_main);

        try {
            copyAppDbToDownloadFolder();
        } catch (IOException e) {
            e.printStackTrace();
        }

        dataSource = new PodcastDataSource(this);

        dataSource.openDb();
        allPodcastNames = dataSource.getAllPodcastTitles();
        String[] fromColumns = new String[]{PodcastEntry.COLUMN_NAME_TITLE};
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
                PodcastDataSource dataSource = new PodcastDataSource(getApplicationContext());
                dataSource.openDb();
                HashMap<String, String> podcastInfo = dataSource.getAllPodcastTitlesLinks();
                dataSource.closeDb();
                RefreshRSSFeed refreshFeed = new RefreshRSSFeed();
                if(podcastInfo != null){
                    //noinspection unchecked
                    refreshFeed.execute(podcastInfo);
                }

                return true;
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
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        databaseHelper.onUpgrade(databaseHelper.getWritableDatabase(), 1, 2);
    }

    public void viewEpisode(String podcastChosen) {
        Intent intent = new Intent(this, PodcastViewerActivity.class);
        intent.putExtra("userChoice", podcastChosen);
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
            MediaScannerConnection.scanFile(this, new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/backup.db"}, null, null);
        }
    }

    public class RefreshRSSFeed extends AsyncTask<HashMap<String, String>, Void, String> {
        @Override
        protected String doInBackground(HashMap<String, String>... urlList) {
            try {
                Set entrySet = urlList[0].entrySet();
                int[] results = new int[entrySet.size()];
                int i = 0;
                for (Object anEntrySet : entrySet) {
                    Map.Entry mapEntry = (Map.Entry) anEntrySet;
                    results[i] = refreshRSSFeed(mapEntry.getValue().toString(), mapEntry.getKey().toString());
                    i++;
                }
                return Integer.toString(results.length);
/*
                if (result == Utilities.SUCCESS) {
                    return "refreshed";
                } else if (result == Utilities.INVALID_URL) {
                    return "URL Invalid";
                } else if (result == Utilities.FAILURE_TO_PARSE) {
                    return "Not Valid Podcast Feed";
                } else if (result == Utilities.NO_NEW_EPISODES) {
                    return "NoEpisodes";
                }
                */
            } catch (HTTPConnectionException httpException) {
                return "Connection Error " + httpException.getResponseCode();
            } catch (IOException e) {
                Log.e("sw9", "Fail on ic_download RSS Feed, ERROR DUMP: " + e.getMessage() + " " + e.getClass());
                return "Exception: " + e.getClass();
            }
         //   return "Error";
        }

        @Override
        protected void onPostExecute(String subscribed) {

            int duration = Toast.LENGTH_LONG;
            if (subscribed.equals("refreshed")) {
                // Send out a toast displaying success
                // May be able to get this toast to the user faster
                if (getApplicationContext() != null) {
                    Toast.makeText(getApplicationContext(), "Refreshed", duration).show();
                }
                //successfulSubscription();
            } else if (subscribed.equals("NoEpisodes")) {
                // Do Something
            } else {
                if (getApplicationContext() != null) {
                    Toast.makeText(getApplicationContext(), subscribed, duration).show();
                }
            }
            // Implement the observer design pattern here to move to feeds page.
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
                        if(podcast.getEpisodeList().size() > 0) {
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
                        Log.e("sw9", e.getMessage());
                    }
                }
            }
            return Utilities.INVALID_URL;
        }
    }
}
