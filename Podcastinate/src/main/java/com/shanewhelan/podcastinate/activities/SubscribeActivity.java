package com.shanewhelan.podcastinate.activities;

import android.app.Activity;
import com.shanewhelan.podcastinate.R;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.shanewhelan.podcastinate.*;
/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public class SubscribeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_activity);

        final Button button = (Button) findViewById(R.string.testButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if(testNetwork()){
                    subscribeToFeed();
                }else{

                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.subscribe, menu);
        return true;
    }

    public void subscribeToFeed(){
        String urlToTest = "http://nerdist.libsyn.com/rss";
        DownloadRSSFeed downFeed = new DownloadRSSFeed();
        downFeed.execute(urlToTest);
        TextView textView = (TextView) findViewById(R.string.displayRSSResults);
        textView.setText("Success");
    }


    public boolean testNetwork() {
        ConnectivityManager conMan = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conMan.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            //Say to the user that URL is invalid
            Log.i("sw9", "No network connection available.");
            return false;
        }
    }
}