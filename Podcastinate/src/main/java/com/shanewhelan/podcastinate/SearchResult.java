package com.shanewhelan.podcastinate;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchResult implements Parcelable {
    private String title;
    private String imageLink;
    private String link;
    private String description;
    //private List<String> genres;

    public SearchResult() {
    }

    public SearchResult(Parcel parcel) {
        title = parcel.readString();
        imageLink = parcel.readString();
        link = parcel.readString();
        description = parcel.readString();
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

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(title);
        parcel.writeString(imageLink);
        parcel.writeString(link);
        parcel.writeString(description);
    }

    public static final Parcelable.Creator<SearchResult> CREATOR = new Parcelable.Creator<SearchResult>() {
        public SearchResult createFromParcel(Parcel parcel) {
            return new SearchResult(parcel);
        }

        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };


}
