package com.shanewhelan.podcastinate;

public class Episode {
    private int episodeID;
    private int podcastID;
    private String title;
    private String description;
    private String enclosure;
    private String pubDate;
    private String duration;
    private String guid;
    private String directory;
    private boolean isNew;
    private int currentTime;


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

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getEnclosure() {
        return enclosure;
    }

    public void setEnclosure(String enclosure) {
        this.enclosure = enclosure;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        this.isNew = aNew;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;
    }

    public int getPodcastID() {
        return podcastID;
    }

    public void setPodcastID(int podcastID) {
        this.podcastID = podcastID;
    }

    public int getEpisodeID() {
        return episodeID;
    }

    public void setEpisodeID(int episodeID) {
        this.episodeID = episodeID;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
