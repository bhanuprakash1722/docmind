package com.docmind.indexer;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side view of the event the api-service publishes when a document is created.
 * Kept as its own record so the two services share only the JSON contract, not code.
 */
public record DocumentCreatedEvent(UUID documentId, String title, String body, Instant occurredAt) {
}
