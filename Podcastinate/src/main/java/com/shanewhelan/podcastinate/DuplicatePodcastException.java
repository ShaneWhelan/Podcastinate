package com.shanewhelan.podcastinate;

/**
 * Created by Shane on 11/01/14. Podcastinate.
 */
public class DuplicatePodcastException extends Exception {

    public DuplicatePodcastException(String message) {
        super(message);
    }

    /*
    public DuplicatePodcastException(String message, Throwable throwable){
        super(message, throwable);
    }
    */
}
