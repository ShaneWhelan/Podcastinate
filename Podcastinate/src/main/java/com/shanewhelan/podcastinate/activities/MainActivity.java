package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.*;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.shanewhelan.podcastinate.DuplicatePodcastException;
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

import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;

import static android.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER;

/*
FEATURES:
TODO: Add picture beside podcast name
TODO: Create Download Queue (Cancel, pause and start downloads)
TODO: Add long press options (Maybe refresh individual feeds, mark done/new, add to playlist, sort options, force update of thumnail)
TODO: Delete Subscription
TODO: Persistent notification while episode plays
TODO: Click RSS link to go to Podcastinate
TODO: Set back button to go to right activities
MAJOR FEATURES:
TODO: Cloud backup
TODO: User Settings
TODO: Car Mode
TODO: Recommendations
TODO: User Settings
LOW PRIORITY:
TODO: Populate isListened DB entry
TODO: Streaming: Must keep WIFI from sleeping
TODO: Help Section
TODO: Add Paging to podcast viewing activity to
BUGS:
TODO: BUG download service, no retry on download fail
TODO: Multi select number of selected is fucked
*/

public class MainActivity extends Activity {
    private PodcastDataSource dataSource;
    private MenuItem refreshAction;
    private ProgressBar mProgressBar1;
    private Cursor podcastCursor;
    private static PodcastAdapter podcastAdapter;
    private static ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) throws NullPointerException {
        super.onCreate(savedInstanceState);
        setTitle("Podcasts");
        setContentView(R.layout.activity_main);

        // TODO: Dev only, take out for release
        try {
            copyAppDbToDownloadFolder();
        } catch (IOException e) {
            Utilities.logException(e);
        }


        listView = (ListView) findViewById(R.id.listOfPodcasts);
        initialiseAdapter();
        initialiseSelectionListeners();

        mProgressBar1 = (ProgressBar) findViewById(R.id.smoothProgressBar);
        mProgressBar1.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(getApplicationContext()).interpolator(new AccelerateInterpolator()).build());

        Intent incomingIntent = getIntent();
        if(incomingIntent != null) {
            if(incomingIntent.getAction() != null){
                if(incomingIntent.getAction().equals(Utilities.ACTION_SUBSCRIBE)) {
                    // Received URL to subscribe to now process it
                    if(Utilities.testNetwork(getApplicationContext())) {
                        DownloadRSSFeed downloadRSSFeed = new DownloadRSSFeed();
                        downloadRSSFeed.execute(incomingIntent.getStringExtra(Utilities.PODCAST_LINK));
                    }
                }
            }
        }
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
                startActivity(intent);
                return true;
            case R.id.action_settings:

                return true;
            case R.id.action_refresh:
                refreshAction = item;
                if (Utilities.testNetwork(getApplicationContext())) {
                    PodcastDataSource dataSource = new PodcastDataSource(getApplicationContext());
                    dataSource.openDbForReading();
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

    public void initialiseAdapter() {
        dataSource = new PodcastDataSource(getApplicationContext());
        dataSource.openDbForReading();
        // Get Podcast ID so we can get all episode names from DB
        podcastCursor = dataSource.getAllPodcastTitlesImages();
        podcastAdapter = new PodcastAdapter(getApplicationContext(), podcastCursor,
                FLAG_REGISTER_CONTENT_OBSERVER);
        listView.setAdapter(podcastAdapter);
        dataSource.closeDb();
    }

    public class MultiChoiceModeListener implements ListView.MultiChoiceModeListener {
        private int nr = 0;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            // Here you can do something when items are selected/de-selected,
            // such as update the title in the CAB
            if (checked) {
                nr++;
                podcastAdapter.setNewSelection(position, true);
            } else {
                nr--;
                podcastAdapter.removeSelection(position);
            }
            mode.setTitle(nr + " Selected");
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB
            MenuInflater inflater = mode.getMenuInflater();
            if (inflater != null) {
                inflater.inflate(R.menu.multi_select, menu);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Here you can perform updates to the CAB due to
            // an invalidate() request
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Respond to clicks on the actions in the CAB
            switch (item.getItemId()) {
                case R.id.delete_selection_action:
                    //deleteSelectedItems();
                    updateListOfPodcasts();
                    nr = 0;
                    podcastAdapter.clearSelection();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Here you can make any necessary updates to the activity when
            // the CAB is removed. By default, selected items are deselected/unchecked.
            podcastAdapter.clearSelection();
            nr = 0;
        }
    }

    public void initialiseSelectionListeners() {
        OnItemClickListener itemCLickHandler = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(view.getId() == R.id.podcastArtImage) {
                    ImageView imageView = (ImageView) view.findViewById(R.id.podcastArtImage);
                    if (imageView.getContentDescription() != null) {
                        viewPodcast(imageView.getContentDescription().toString());
                    }
                } else {
                    TextView textView = (TextView) view.findViewById(R.id.podcastName);
                    if (textView.getText() != null) {
                        viewPodcast(textView.getText().toString());
                    }
                }
            }
        };
        listView.setOnItemClickListener(itemCLickHandler);

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new MultiChoiceModeListener());
    }

    public void updateListOfPodcasts() {
        dataSource.openDbForReading();
        podcastCursor = dataSource.getAllPodcastTitlesImages();
        podcastAdapter.swapCursor(podcastCursor);
        podcastAdapter.notifyDataSetChanged();
        dataSource.closeDb();
    }

    public void wipeDb() {
        // Only to be left in developer version - wipes db and external storage directory
        PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
        pds.openDbForWriting();
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

    public void viewPodcast(String podcastChosen) {
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

            // MediaScannerConnection.scanFile(this, new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/backup.db"}, null, null);
        }
    }

    public String[] getPodcastLinks() {
        PodcastDataSource dataSource = new PodcastDataSource(getApplicationContext());
        dataSource.openDbForReading();
        String[] listOfLinks = dataSource.getAllPodcastLinks();
        dataSource.closeDb();
        return listOfLinks;
    }

    public boolean isLinkUnique(String[] listOfLinks, String link) {
        boolean linkUnique = true;
        for (String currentLink : listOfLinks) {
            if (link.equals(currentLink)) {
                linkUnique = false;
            }
        }
        return linkUnique;
    }

    public class RefreshRSSFeed extends AsyncTask<HashMap<String, String>, Void, HashMap<String, String>> {

        @Override
        protected void onPreExecute() {
            // Set Refresh icon to progress bar
            refreshAction.setActionView(R.layout.actionbar_indeterminate_progress);
            refreshAction.expandActionView();

            mProgressBar1.setVisibility(View.VISIBLE);
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
            mProgressBar1.setVisibility(View.GONE);


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
                    inputStream = httpCon.getInputStream();
                } else {
                    throw new HTTPConnectionException(response);
                }

                ParseRSS parseRSS = new ParseRSS();
                XmlPullParser xmlPullParser = parseRSS.inputStreamToPullParser(inputStream);
                if (xmlPullParser != null) {
                    PodcastDataSource pds = new PodcastDataSource(getApplicationContext());
                    pds.openDbForReading();
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

    public class DownloadRSSFeed extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            mProgressBar1.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                int result = downloadRSSFeed(urls[0]);
                if (result == Utilities.SUCCESS) {
                    return "subscribed";
                } else if (result == Utilities.INVALID_URL) {
                    return "URL Invalid";
                } else if (result == Utilities.FAILURE_TO_PARSE) {
                    return "Not Valid Podcast Feed";
                }
            } catch (DuplicatePodcastException e) {
                return "Already subscribed to podcast";
            } catch (HTTPConnectionException e) {
                Utilities.logException(e);
                return "Connection Error " + e.getResponseCode();
            } catch (IOException e) {
                Utilities.logException(e);
                return "Exception: " + e.getClass();
            }
            return "Error";
        }

        @Override
        protected void onPostExecute(String subscribed) {
            mProgressBar1.setVisibility(View.GONE);

            int duration = Toast.LENGTH_LONG;
            if (subscribed.equals("subscribed")) {
                // Send out a toast displaying success
                // May be able to get this toast to the user faster
                if (getApplicationContext() != null) {
                    Toast.makeText(getApplicationContext(), "Subscribed", duration).show();
                }
                updateListOfPodcasts();
            } else {
                if (getApplicationContext() != null) {
                    Toast.makeText(getApplicationContext(), subscribed, duration).show();
                }
            }
        }

        private int downloadRSSFeed(String url) throws DuplicatePodcastException, IOException {
            // Check for existing podcast
            String[] listOfLinks = getPodcastLinks();
            if (!isLinkUnique(listOfLinks, url)) {
                throw new DuplicatePodcastException("Podcast Already in Database");
            }

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

                    Podcast podcast = parseRSS.parseRSSFeed(xmlPullParser, url);

                    if (podcast != null) {
                        Utilities.savePodcastToDb(getApplicationContext(), podcast, true);
                        return Utilities.SUCCESS;
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

    public class PodcastAdapter extends CursorAdapter implements View.OnClickListener {
        private final LayoutInflater layoutInflater;
        private SparseBooleanArray sparseBArray = new SparseBooleanArray();

        public PodcastAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String podcastTitle = cursor.getString(cursor.getColumnIndex(PodcastEntry.TITLE));
            TextView podcastTitleView = (TextView) view.findViewById(R.id.podcastName);
            podcastTitleView.setText(podcastTitle);

            // Load images in background thread
            ImageView podcastImage = (ImageView) view.findViewById(R.id.podcastArtImage);
            podcastImage.setContentDescription(podcastTitle);

            LoadImageFromDisk loadImage = new LoadImageFromDisk(podcastImage);
            loadImage.execute(cursor.getString(cursor.getColumnIndex(PodcastEntry.IMAGE_DIRECTORY)));
            /*
            // Set up listeners or nothing will work
            podcastTitleView.setOnClickListener(this);
            podcastImage.setOnClickListener(this);
            */
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return layoutInflater.inflate(R.layout.podcast_list_item, parent, false);
        }

        public void setNewSelection(int position, boolean value) {
            sparseBArray.put(position, value);
            notifyDataSetChanged();
        }

        public void removeSelection(int position) {
            sparseBArray.delete(position);
            notifyDataSetChanged();
        }

        public void clearSelection() {
            sparseBArray = new SparseBooleanArray();
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Adapter handles setting up rows
            View v = super.getView(position, convertView, parent);
            if (v != null) {
                v.setBackgroundColor(getResources().getColor(android.R.color.background_light));
                if (sparseBArray.get(position)) {
                    v.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                }
            }
            return v;
        }

        @Override
        public void onClick(View view) {

        }
    }

    public class LoadImageFromDisk extends AsyncTask<String, Void, Bitmap> {
        private ImageView ImageView;

        public LoadImageFromDisk(ImageView imageView) {
            this.ImageView = imageView;
        }

        protected Bitmap doInBackground(String... directory) {
            return BitmapFactory.decodeFile(directory[0]);
        }

        protected void onPostExecute(Bitmap result) {
            ImageView.setImageBitmap(result);
        }
    }
}
