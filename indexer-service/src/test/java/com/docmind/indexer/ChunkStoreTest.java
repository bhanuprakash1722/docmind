package com.docmind.indexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

class ChunkStoreTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ChunkStore store = new ChunkStore(jdbc);

    @Test
    @SuppressWarnings("unchecked")
    void saveAllBatchUpsertsOnNaturalKey() throws Exception {
        ChunkRecord chunk = new ChunkRecord(UUID.randomUUID(), UUID.randomUUID(), 0, "content");
        when(jdbc.batchUpdate(anyString(), anyCollection(), anyInt(), any(ParameterizedPreparedStatementSetter.class)))
                .thenReturn(new int[][] {{1}});

        store.saveAll(List.of(chunk));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ParameterizedPreparedStatementSetter<ChunkRecord>> setter =
                ArgumentCaptor.forClass(ParameterizedPreparedStatementSetter.class);
        verify(jdbc).batchUpdate(sql.capture(), eq(List.of(chunk)), eq(1), setter.capture());
        assertTrue(sql.getValue().contains("ON CONFLICT (document_id, chunk_index)"),
                "inserts must be idempotent on the natural key");

        PreparedStatement ps = mock(PreparedStatement.class);
        setter.getValue().setValues(ps, chunk);
        verify(ps).setObject(1, chunk.id());
        verify(ps).setObject(2, chunk.documentId());
        verify(ps).setInt(3, 0);
        verify(ps).setString(4, "content");
    }

    @Test
    void saveAllWithEmptyListSkipsDatabase() {
        store.saveAll(List.of());

        verifyNoInteractions(jdbc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByDocumentIdOrdersByChunkIndexAndMapsRows() throws Exception {
        UUID documentId = UUID.randomUUID();
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        store.findByDocumentId(documentId);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RowMapper<ChunkRecord>> mapper = ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbc).query(sql.capture(), mapper.capture(), eq(documentId));
        assertTrue(sql.getValue().contains("ORDER BY chunk_index"));

        ResultSet rs = mock(ResultSet.class);
        UUID id = UUID.randomUUID();
        when(rs.getObject("id", UUID.class)).thenReturn(id);
        when(rs.getObject("document_id", UUID.class)).thenReturn(documentId);
        when(rs.getInt("chunk_index")).thenReturn(2);
        when(rs.getString("content")).thenReturn("text");

        ChunkRecord mapped = mapper.getValue().mapRow(rs, 0);

        assertEquals(new ChunkRecord(id, documentId, 2, "text"), mapped);
    }

    @Test
    void deleteByDocumentIdReturnsDeletedCount() {
        UUID documentId = UUID.randomUUID();
        when(jdbc.update(anyString(), any(Object.class))).thenReturn(3);

        int deleted = store.deleteByDocumentId(documentId);

        assertEquals(3, deleted);
        verify(jdbc).update("DELETE FROM chunks WHERE document_id = ?", documentId);
    }
}
