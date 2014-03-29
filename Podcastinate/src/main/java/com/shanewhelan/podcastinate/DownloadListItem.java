package com.shanewhelan.podcastinate;

/**
 * Created by Shane on 28/03/2014. Podcastinate.
 */
public class DownloadListItem {
    private Episode episode;
    private long queueID;
    private String podcastTitle;

    public DownloadListItem(Episode episode, long queueID, String podcastTitle) {
        this.episode = episode;
        this.queueID = queueID;
        this.podcastTitle = podcastTitle;
    }

    public Episode getEpisode() {
        return episode;
    }

    public void setEpisode(Episode episode) {
        this.episode = episode;
    }

    public long getQueueID() {
        return queueID;
    }

    public void setQueueID(long queueID) {
        this.queueID = queueID;
    }

    public String getPodcastTitle() {
        return podcastTitle;
    }

    public void setPodcastTitle(String podcastTitle) {
        this.podcastTitle = podcastTitle;
    }
}
