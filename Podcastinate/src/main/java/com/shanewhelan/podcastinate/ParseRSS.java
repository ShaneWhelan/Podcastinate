package com.shanewhelan.podcastinate;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public class ParseRSS {
    Podcast podcast = new Podcast();

    public ParseRSS(InputStream inputStream){
        if(inputStream != null){
            //String feed = convertStreamToString(inputStream);
            //longLogCat(feed);
            //Log.d("sw9", "Feed Length: " + feed.length());
            inputStreamToPullParser(inputStream);
        }else{
            Log.d("sw9", "Instream is null");
        }
    }

    private void inputStreamToPullParser(InputStream inputStream) {
        try {
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlPullParser.setInput(inputStream, null);
            xmlPullParser.nextTag();
            parseRSSFeed(xmlPullParser);
        }catch(XmlPullParserException ex){
            Log.d("sw9", ex.toString());
        }catch (IOException ex){
            Log.d("sw9", ex.toString() + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void parseRSSFeed(XmlPullParser xmlPullParser){
        ArrayList<Episode> episodeList = new ArrayList<Episode>();
        try {
            boolean hasParentNodeChannel = false;
            boolean hasParentNodeItem = false;
            boolean isNewEpisode = true;
            Episode episode = null;

            // If you want latest episode Loop should run till END_TAG if you want Whole feed then END_DOCUMENT
            xmlPullParser.require(XmlPullParser.START_TAG, null, "rss");
            while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
                String nodeName = xmlPullParser.getName();
                if (xmlPullParser.getEventType() == XmlPullParser.START_TAG) {
                    // Remember parent node channel for saving podcast name
                    if(nodeName.equals("channel")) {
                        hasParentNodeChannel = true;
                    }

                    if(hasParentNodeChannel) {
                        // Title has multiple occurrences so we must remember its parent.
                        if(nodeName.equals("title")) {
                            savePodcastTitle(xmlPullParser);
                        }else if(nodeName.equals("description")) {
                            savePodcastDescription(xmlPullParser);
                        }else if(nodeName.equals("image")) {
                            podcast.setImageDirectory("testDir");
                        }else if(nodeName.equals("link")) {
                            savePodcastLink(xmlPullParser);
                        }
                    }

                    if(nodeName.equals("item")){
                        hasParentNodeChannel = false;
                        hasParentNodeItem = true;
                        isNewEpisode = true;
                    }

                    if(isNewEpisode) {
                        episode = new Episode();
                    }

                    if(hasParentNodeItem) {
                        if(nodeName.equals("title")) {
                            saveTitle(xmlPullParser, episode);
                        }else if(nodeName.equals("link")) {
                            saveLink(xmlPullParser, episode);
                        }else if(nodeName.equals("description")) {
                            saveDescription(xmlPullParser, episode);
                        }else if(nodeName.equals("pubDate")) {
                            savePubDate(xmlPullParser, episode);
                        }else if(nodeName.equals("guid")){
                            saveGuid(xmlPullParser, episode);
                        }else if(nodeName.equals("duration")) {
                            saveDuration(xmlPullParser, episode);
                        }else if(nodeName.equals("episodeImage")) {
                            saveEpisodeImage(xmlPullParser, episode);
                        }else if(nodeName.equals("enclosure")) {
                            saveEnclosure(xmlPullParser, episode);
                        }

                    }
                }else if(xmlPullParser.getEventType() == XmlPullParser.END_TAG){
                    if(nodeName.equals("item")) {
                        hasParentNodeItem = false;
                        // Possible bug here
                        isNewEpisode = false;
                        episodeList.add(episode);
                    }
                }
            }
            podcast.setEpisodeList(episodeList);
            Log.d("sw9", " " + podcast.getEpisodeList().size());
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            Log.d("sw9", e.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savePodcastTitle(XmlPullParser xmlPullParser) throws IOException, XmlPullParserException {
        podcast.setTitle(xmlPullParser.nextText());
    }

    public void savePodcastDescription(XmlPullParser xmlPullParser) throws IOException, XmlPullParserException {
        podcast.setDescription(xmlPullParser.nextText());
    }

    public void savePodcastImageDirectory(XmlPullParser xmlPullParser) throws IOException,
            XmlPullParserException {
        podcast.setImageDirectory(xmlPullParser.nextText());
    }

    public void savePodcastLink(XmlPullParser xmlPullParser) throws IOException,
            XmlPullParserException {
        podcast.setLink(xmlPullParser.nextText());
    }

    public void saveTitle(XmlPullParser xmlPullParser, Episode episode) throws IOException, XmlPullParserException {
        episode.setTitle(xmlPullParser.nextText());
    }

    public void saveLink(XmlPullParser xmlPullParser, Episode episode) throws IOException, XmlPullParserException {
        episode.setLink(xmlPullParser.nextText());
    }

    public void saveDescription(XmlPullParser xmlPullParser, Episode episode) throws IOException, XmlPullParserException {
        episode.setDescription(xmlPullParser.nextText());
    }

    public void savePubDate(XmlPullParser xmlPullParser, Episode episode) throws IOException, XmlPullParserException {
        episode.setPubDate(xmlPullParser.nextText());
    }

    public void saveGuid(XmlPullParser xmlPullParser, Episode episode) throws IOException, XmlPullParserException {
        episode.setGuid(xmlPullParser.nextText());
    }

    public void saveDuration(XmlPullParser xmlPullParser, Episode episode) throws IOException, XmlPullParserException {
        episode.setDuration(xmlPullParser.nextText());
    }

    public void saveEpisodeImage(XmlPullParser xmlPullParser, Episode episode) throws IOException, XmlPullParserException {
        episode.setEpisodeImage(xmlPullParser.nextText());
    }

    public void saveEnclosure(XmlPullParser xmlPullParser, Episode episode){
        episode.setEnclosure(xmlPullParser.getAttributeValue(null, "url"));
    }

    public String convertStreamToString(InputStream inputStream) {
        /*
        // Google's way that performs better but requires int length parameter
        // 80 MS
        try {
            Reader reader = null;
            reader = new InputStreamReader(inStream, "UTF-8");
            char[] buffer = new char[length];
            reader.read(buffer);
            return new String(buffer);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
        */
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder total = new StringBuilder();
        String line;
        try {
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        line = total.toString();
        return line;
    }

    public static void longLogCat(String str) {
        // Possible recusion here?
        if(str.length() > 4000) {
            Log.d("sw9", str.substring(0, 4000));
            longLogCat(str.substring(4000));
        } else
            Log.d("sw9", str);
    }

    public Podcast getPodcast() {
        return podcast;
    }
}