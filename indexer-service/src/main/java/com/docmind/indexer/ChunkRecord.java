package com.docmind.indexer;

import java.util.UUID;

/**
 * One stored chunk of a document, before its embedding is computed. Chunks are
 * identified naturally by (documentId, chunkIndex), which is what makes re-indexing
 * a document idempotent.
 */
public record ChunkRecord(UUID id, UUID documentId, int chunkIndex, String content) {
}
