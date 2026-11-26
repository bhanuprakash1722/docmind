package com.docmind.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a document is created. The indexer consumes it, chunks the body,
 * embeds the chunks and stores the vectors, so the event carries the full body.
 */
public record DocumentCreatedEvent(UUID documentId, String title, String body, Instant occurredAt) {

    public static DocumentCreatedEvent from(Document document) {
        return new DocumentCreatedEvent(document.getId(), document.getTitle(), document.getBody(),
                Instant.now());
    }
}
