package com.docmind.api;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(UUID id, String title, String body, Instant createdAt) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(document.getId(), document.getTitle(), document.getBody(), document.getCreatedAt());
    }
}
