package com.shanewhelan.podcastinate.exceptions;

import java.io.IOException;

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
