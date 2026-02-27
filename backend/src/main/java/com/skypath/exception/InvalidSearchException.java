package com.skypath.exception;

/**
 * Thrown when search parameters are invalid.
 */
public class InvalidSearchException extends RuntimeException {

    public InvalidSearchException(String message) {
        super(message);
    }
}
