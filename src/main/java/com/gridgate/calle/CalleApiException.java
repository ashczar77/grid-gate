package com.gridgate.calle;

/**
 * Raised when a CALL-E HTTP request fails.
 */
public class CalleApiException extends RuntimeException {

    private final int statusCode;

    public CalleApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public CalleApiException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
