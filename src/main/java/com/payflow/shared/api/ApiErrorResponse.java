package com.payflow.shared.api;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error response.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> validationErrors
) {

    public static ApiErrorResponse of(
            int status,
            String error,
            String message,
            String path
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                List.of()
        );
    }

    public static ApiErrorResponse validation(
            int status,
            String error,
            String message,
            String path,
            List<String> validationErrors
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                List.copyOf(validationErrors)
        );
    }
}