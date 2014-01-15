package com.shanewhelan.podcastinate;

import java.util.ArrayList;

/**
 * Created by Shane on 29/10/13. Podcastinate.
 */
public class Podcast {
      /*
      item
	  title
	  link
	  description
	  pubDate
	  guid
	  itunes:subtitle
	  itunes:summary
	  itunes:author
	  itunes:explicit
	  itunes:duration
	  itunes:keywords
	  itunes:image
	  media:content
	  enclosure
     */

    String title;
    String description;
    String imageDirectory;
    String link;
    ArrayList<Episode> episodeList = new ArrayList<Episode>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageDirectory() {
        return imageDirectory;
    }

    public void setImageDirectory(String imageDirectory) {
        this.imageDirectory = imageDirectory;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public ArrayList<Episode> getEpisodeList() {
        return episodeList;
    }

    public void setEpisodeList(ArrayList<Episode> episodeList) {
        this.episodeList = episodeList;
    }
}
