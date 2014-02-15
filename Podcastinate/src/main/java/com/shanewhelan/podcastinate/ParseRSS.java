package com.shanewhelan.podcastinate;

import android.util.Log;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public class ParseRSS {
    private Podcast podcast = new Podcast();

    public XmlPullParser inputStreamToPullParser(InputStream inputStream) {
        try {
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlPullParser.setInput(inputStream, null);
            xmlPullParser.nextTag();
            return xmlPullParser;
        } catch (XmlPullParserException ex) {
            Log.d("sw9", ex.toString());
        } catch (IOException ex) {
            Log.d("sw9", ex.toString() + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    public Podcast parseRSSFeed(XmlPullParser xmlPullParser, String[] listOfLinks) throws DuplicatePodcastException {
        ArrayList<Episode> episodeList = new ArrayList<Episode>();
        try {
            boolean hasParentNodeChannel = false;
            boolean hasParentNodeItem = false;
            Episode episode = null;

            // If you want latest episode Loop should run till END_TAG if you want Whole feed then END_DOCUMENT
            xmlPullParser.require(XmlPullParser.START_TAG, null, "rss");
            while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
                String nodeName = xmlPullParser.getName();
                if (xmlPullParser.getEventType() == XmlPullParser.START_TAG) {
                    // Remember parent node channel for saving podcast name
                    if (nodeName.equals("channel")) {
                        hasParentNodeChannel = true;
                    }
                    if (hasParentNodeChannel) {
                        // Title has multiple occurrences so we must remember its parent.
                        if (nodeName.equals("title")) {
                            savePodcastTitle(xmlPullParser);
                        } else if (nodeName.equals("description")) {
                            savePodcastDescription(xmlPullParser);
                        } else if (nodeName.equals("image")) {
                            podcast.setImageDirectory("testDir");
                        } else if (nodeName.equals("atom:link")) {
                            savePodcastLink(xmlPullParser);
                            if (!isLinkUnique(listOfLinks, podcast.getLink())) {
                                throw new DuplicatePodcastException("Podcast Already in Database");
                            }
                        }
                    }

                    if (nodeName.equals("item")) {
                        hasParentNodeChannel = false;
                        hasParentNodeItem = true;
                        episode = new Episode();
                    }

                    if (hasParentNodeItem) { // Needs some work to get the right metadata
                        if (nodeName.equals("title")) {
                            saveTitle(xmlPullParser, episode);
                        } else if (nodeName.equals("link")) {
                            saveLink(xmlPullParser, episode);
                        } else if (nodeName.equals("description")) {
                            saveDescription(xmlPullParser, episode);
                        } else if (nodeName.equals("pubDate")) {
                            savePubDate(xmlPullParser, episode);
                        } else if (nodeName.equals("guid")) {
                            saveGuid(xmlPullParser, episode);
                        } else if (nodeName.equals("itunes:duration")) {
                            saveDuration(xmlPullParser, episode);
                        } else if (nodeName.equals("episodeImage")) {
                            saveEpisodeImage(xmlPullParser, episode);
                        } else if (nodeName.equals("enclosure")) {
                            saveEnclosure(xmlPullParser, episode);
                        }
                    }
                } else if (xmlPullParser.getEventType() == XmlPullParser.END_TAG) {
                    if (nodeName.equals("item")) {
                        hasParentNodeItem = false;
                        episodeList.add(episode);
                    }
                }
            }
            podcast.setEpisodeList(episodeList);
            Log.d("sw9", "Episode List size: " + podcast.getEpisodeList().size());
            return podcast;
        } catch (XmlPullParserException e) {
            Log.e("sw9", e.getMessage());
        } catch (IOException e) {
            Log.e("sw9", e.getMessage());
        }
        return null;
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

    public boolean isLinkUnique(String[] listOfLinks, String link) {
        boolean linkUnique = true;
        for (String currentLink : listOfLinks) {
            if (link.equals(currentLink)) {
                linkUnique = false;
            }
        }
        return linkUnique;
    }

    public void savePodcastLink(XmlPullParser xmlPullParser) throws IOException,
            XmlPullParserException {
        podcast.setLink(xmlPullParser.getAttributeValue(null, "href"));
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
        try {
            DateFormat pubDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            Date podcastDate = pubDateFormat.parse(xmlPullParser.nextText());
            Calendar cal = Calendar.getInstance();
            cal.setTime(podcastDate);
            SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
            String formattedDate = sqlDateFormat.format(cal.getTime());
            episode.setPubDate(formattedDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
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

    public void saveEnclosure(XmlPullParser xmlPullParser, Episode episode) {
        episode.setEnclosure(xmlPullParser.getAttributeValue(null, "url"));
    }

    public Podcast checkForNewEntries(XmlPullParser xmlPullParser, String mostRecentEpisodeEnclosure, String podcastTitle) {
        ArrayList<Episode> episodeList = new ArrayList<Episode>();
        try {
            boolean hasParentNodeItem = false;
            Episode episode = null;
            podcast.setTitle(podcastTitle);
            // If you want latest episode Loop should run till END_TAG if you want Whole feed then END_DOCUMENT
            xmlPullParser.require(XmlPullParser.START_TAG, null, "rss");
            while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
                String nodeName = xmlPullParser.getName();
                if (xmlPullParser.getEventType() == XmlPullParser.START_TAG) {

                    if (nodeName.equals("item")) {
                        hasParentNodeItem = true;
                        episode = new Episode();
                    }

                    if (hasParentNodeItem) { // Needs some work to get the right metadata
                        if (nodeName.equals("title")) {
                            saveTitle(xmlPullParser, episode);
                        } else if (nodeName.equals("link")) {
                            saveLink(xmlPullParser, episode);
                        } else if (nodeName.equals("description")) {
                            saveDescription(xmlPullParser, episode);
                        } else if (nodeName.equals("pubDate")) {
                            savePubDate(xmlPullParser, episode);
                        } else if (nodeName.equals("guid")) {
                            saveGuid(xmlPullParser, episode);
                        } else if (nodeName.equals("itunes:duration")) {
                            saveDuration(xmlPullParser, episode);
                        } else if (nodeName.equals("episodeImage")) {
                            saveEpisodeImage(xmlPullParser, episode);
                        } else if (nodeName.equals("enclosure")) {
                            // Check if already exists in database and quit if it does.
                            if(!mostRecentEpisodeEnclosure.equals(xmlPullParser.getAttributeValue(null, "url"))) {
                                saveEnclosure(xmlPullParser, episode);
                            } else {
                                break;
                            }
                        }
                    }
                } else if (xmlPullParser.getEventType() == XmlPullParser.END_TAG) {
                    if (nodeName.equals("item")) {
                        hasParentNodeItem = false;
                        episodeList.add(episode);
                    }
                }
            }
            podcast.setEpisodeList(episodeList);
            return podcast;
        } catch (XmlPullParserException e) {
            Log.e("sw9", e.getMessage());
        } catch (IOException e) {
            Log.e("sw9", e.getMessage());
        }
        return null;
    }
}