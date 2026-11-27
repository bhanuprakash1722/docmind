package com.docmind.indexer;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Stores document chunks in PostgreSQL (pgvector database). Inserts are idempotent on
 * the natural key (document_id, chunk_index): re-saving a document's chunks updates
 * content in place instead of duplicating rows. Embeddings are written later by the
 * indexing pipeline.
 */
@Repository
public class ChunkStore {

    private final JdbcTemplate jdbc;

    public ChunkStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void saveAll(List<ChunkRecord> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(
                "INSERT INTO chunks (id, document_id, chunk_index, content) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (document_id, chunk_index) DO UPDATE SET content = EXCLUDED.content",
                chunks,
                chunks.size(),
                (ps, chunk) -> {
                    ps.setObject(1, chunk.id());
                    ps.setObject(2, chunk.documentId());
                    ps.setInt(3, chunk.chunkIndex());
                    ps.setString(4, chunk.content());
                });
    }

    /** Writes vectors for already-saved chunks (same order and size as {@code chunks}). */
    public void saveEmbeddings(List<ChunkRecord> chunks, List<float[]> vectors) {
        if (chunks.size() != vectors.size()) {
            throw new IllegalArgumentException(
                    "chunks and vectors differ in size: " + chunks.size() + " vs " + vectors.size());
        }
        if (chunks.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(
                "UPDATE chunks SET embedding = ?::vector WHERE document_id = ? AND chunk_index = ?",
                java.util.stream.IntStream.range(0, chunks.size()).boxed().toList(),
                chunks.size(),
                (ps, i) -> {
                    ps.setString(1, toVectorLiteral(vectors.get(i)));
                    ps.setObject(2, chunks.get(i).documentId());
                    ps.setInt(3, chunks.get(i).chunkIndex());
                });
    }

    /** pgvector text format, e.g. [0.1,0.2,0.3]. */
    static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }

    public List<ChunkRecord> findByDocumentId(UUID documentId) {
        return jdbc.query(
                "SELECT id, document_id, chunk_index, content FROM chunks "
                        + "WHERE document_id = ? ORDER BY chunk_index",
                (rs, rowNum) -> new ChunkRecord(
                        rs.getObject("id", UUID.class),
                        rs.getObject("document_id", UUID.class),
                        rs.getInt("chunk_index"),
                        rs.getString("content")),
                documentId);
    }

    public int deleteByDocumentId(UUID documentId) {
        return jdbc.update("DELETE FROM chunks WHERE document_id = ?", documentId);
    }
}
