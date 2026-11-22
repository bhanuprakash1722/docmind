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
