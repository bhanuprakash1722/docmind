package com.docmind.api;

import java.util.Map;

/**
 * Uniform error body for the REST API. {@code fieldErrors} is populated only for
 * validation failures (field name to message), and serializes as null otherwise.
 */
public record ApiError(String error, String message, Map<String, String> fieldErrors) {

    public static ApiError of(String error, String message) {
        return new ApiError(error, message, null);
    }

    public static ApiError validation(Map<String, String> fieldErrors) {
        return new ApiError("validation_failed", "Request validation failed", fieldErrors);
    }
}
