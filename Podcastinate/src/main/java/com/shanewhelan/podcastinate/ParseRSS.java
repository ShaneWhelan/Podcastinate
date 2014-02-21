package com.shanewhelan.podcastinate;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.shanewhelan.podcastinate.exceptions.HTTPConnectionException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */

public class ParseRSS {
    private Podcast podcast = new Podcast();
    private DateFormat rssDateFormatter;
    private SimpleDateFormat sqlDateFormat;
    private Calendar cal;
    private Date podcastDate;
    private String formattedDate;

    public ParseRSS() {
        rssDateFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        cal = Calendar.getInstance();
        podcastDate = null;
        formattedDate = null;
    }

    public XmlPullParser inputStreamToPullParser(InputStream inputStream) {
        try {
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlPullParser.setInput(inputStream, null);
            xmlPullParser.nextTag();
            return xmlPullParser;
        } catch (XmlPullParserException e) {
            Utilities.logException(e);
        } catch (IOException e) {
            Utilities.logException(e);
        }
        return null;
    }

    public Podcast parseRSSFeed(XmlPullParser xmlPullParser, String[] listOfLinks) throws DuplicatePodcastException {
        ArrayList<Episode> episodeList = new ArrayList<Episode>();
        try {
            boolean hasParentNodeChannel = false;
            boolean hasParentNodeItem = false;
            boolean hasParentNodeImage = false;

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
                            hasParentNodeImage = true;
                        } else if (nodeName.equals("itunes:image")) {
                            // Temporarily save image link to directory member of podcast object
                            savePodcastItunesImage(xmlPullParser);
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

                    if (hasParentNodeImage) {
                        if (nodeName.equals("url")) {
                            // Temporarily save image link to directory member of podcast object
                            savePodcastImageDirectory(xmlPullParser);
                        }
                    }
                } else if (xmlPullParser.getEventType() == XmlPullParser.END_TAG) {
                    if (nodeName.equals("item")) {
                        hasParentNodeItem = false;
                        episodeList.add(episode);
                    } else if (nodeName.equals("image")) {
                        hasParentNodeImage = false;
                    }
                }
            }

            podcast.setEpisodeList(episodeList);
            
            downloadEpisodeImage(podcast.getImageDirectory(), podcast.getTitle(), true);
            return podcast;
        } catch (XmlPullParserException e) {
            Utilities.logException(e);
        } catch (IOException e) {
            Utilities.logException(e);
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

    public void savePodcastItunesImage(XmlPullParser xmlPullParser) throws IOException,
            XmlPullParserException {
        podcast.setImageDirectory(xmlPullParser.getAttributeValue(null, "href"));
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

    public void saveDescription(XmlPullParser xmlPullParser, Episode episode) throws IOException, XmlPullParserException {
        episode.setDescription(xmlPullParser.nextText());
    }

    public void savePubDate(XmlPullParser xmlPullParser, Episode episode) throws IOException, XmlPullParserException {
        try {
            podcastDate = rssDateFormatter.parse(xmlPullParser.nextText());
            formattedDate = sqlDateFormat.format(cal.getTime());
            cal.setTime(podcastDate);
            episode.setPubDate(formattedDate);
        } catch (ParseException e) {
            Utilities.logException(e);
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
                            if (!mostRecentEpisodeEnclosure.equals(xmlPullParser.getAttributeValue(null, "url"))) {
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
            Utilities.logException(e);
        } catch (IOException e) {
            Utilities.logException(e);
        }
        return null;
    }

    public void downloadEpisodeImage(String imageDirectory, String title, boolean isNewImage) {
        try {
            // Download podcast file
            HttpGet httpGet = new HttpGet(new URI(imageDirectory));
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse httpResponse = httpClient.execute(httpGet);

            // Exception handle the fact that server could be down
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                // Throw custom exception
                throw new HTTPConnectionException(responseCode);
            }

            // Check if default directory exists and create it if not.
            File externalStorage = new File(Environment.getExternalStorageDirectory() + Utilities.DIRECTORY + "/" + title.replaceAll("[^A-Za-z0-9]", "-") + "/images");
            if (!externalStorage.isDirectory()) {
                if (!externalStorage.mkdirs()) {
                    throw new IOException("Could not create directory");
                }
            }

            String filename = "";
            // Get image file extension
            if (imageDirectory != null) {
                filename = imageDirectory.substring(imageDirectory.lastIndexOf("/"));
            }

            Log.d("sw9", filename);
            File imageFile = new File(externalStorage, filename);

            if(isNewImage) {
                podcast.setImageDirectory(imageFile.getAbsolutePath());
            }

            // Create new image from InputStream
            if (imageFile.createNewFile()) {
                FileOutputStream fileOutput = new FileOutputStream(imageFile);
                InputStream inputStream = httpResponse.getEntity().getContent();

                byte[] buffer = new byte[32768];
                int bufferLength;

                // Use count to make sure we only update the progress bar 50 times in total
                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);
                    // Download progress as a percentage
                }

                // Tidy up and close streams
                inputStream.close();
                fileOutput.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}