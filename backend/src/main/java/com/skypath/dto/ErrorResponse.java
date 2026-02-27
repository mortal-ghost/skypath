package com.skypath.dto;

import java.time.LocalDateTime;

/**
 * API error response.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String timestamp) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now().toString());
    }
}
