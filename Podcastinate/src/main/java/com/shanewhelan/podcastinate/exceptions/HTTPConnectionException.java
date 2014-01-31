package com.shanewhelan.podcastinate.exceptions;

import java.io.IOException;

/**
 * Created by Shane on 31/01/14. Podcastinate.
 */
public class HTTPConnectionException extends IOException {
    private int responseCode;

    public HTTPConnectionException(int responseCode) {
        super();
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
