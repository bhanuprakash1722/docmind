package com.docmind.indexer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Turns a created document into stored, embedded chunks. Chunk ids are derived from
 * (documentId, chunkIndex), so processing the same event twice (Kafka redelivery)
 * rewrites the same rows instead of duplicating them.
 */
@Service
public class IndexingService {

    private final TextChunker chunker = new TextChunker();
    private final ChunkStore store;
    private final Embedder embedder;

    public IndexingService(ChunkStore store, Embedder embedder) {
        this.store = store;
        this.embedder = embedder;
    }

    /** Indexes the document and returns the number of chunks stored. */
    public int index(DocumentCreatedEvent event) {
        List<TextChunk> chunks = chunker.chunk(event.body());
        if (chunks.isEmpty()) {
            return 0;
        }
        List<ChunkRecord> records = chunks.stream()
                .map(c -> new ChunkRecord(chunkId(event.documentId(), c.index()), event.documentId(),
                        c.index(), c.text()))
                .toList();
        store.saveAll(records);
        List<float[]> vectors = embedder.embedAll(records.stream().map(ChunkRecord::content).toList());
        store.saveEmbeddings(records, vectors);
        return records.size();
    }

    static UUID chunkId(UUID documentId, int chunkIndex) {
        return UUID.nameUUIDFromBytes((documentId + ":" + chunkIndex).getBytes(StandardCharsets.UTF_8));
    }
}
