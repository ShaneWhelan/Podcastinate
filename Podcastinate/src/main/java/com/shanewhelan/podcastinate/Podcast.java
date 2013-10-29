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


    String name;
    String description;
    ArrayList<Episode> episodeList = new ArrayList<Episode>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<Episode> getEpisodeList() {
        return episodeList;
    }

    public void setEpisodeList(ArrayList<Episode> episodeList) {
        this.episodeList = episodeList;
    }
}