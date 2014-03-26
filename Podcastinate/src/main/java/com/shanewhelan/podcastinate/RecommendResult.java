package com.shanewhelan.podcastinate;

import java.util.List;

public class RecommendResult {
    private String title;
    private String imageLink;
    private String link;
    @SuppressWarnings("UnusedDeclaration")
    private String description;
    private List<String> genres;

    public RecommendResult() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @SuppressWarnings("UnusedDeclaration")
    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }



}

