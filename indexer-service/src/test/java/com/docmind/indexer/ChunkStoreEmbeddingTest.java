package com.docmind.indexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ChunkStoreEmbeddingTest {

    @Test
    void vectorLiteralUsesPgvectorTextFormat() {
        assertEquals("[0.5,-1.0,2.0]", ChunkStore.toVectorLiteral(new float[] {0.5f, -1f, 2f}));
    }

    @Test
    void mismatchedSizesAreRejected() {
        ChunkStore store = new ChunkStore(mock(JdbcTemplate.class));
        ChunkRecord chunk = new ChunkRecord(UUID.randomUUID(), UUID.randomUUID(), 0, "c");
        assertThrows(IllegalArgumentException.class, () -> store.saveEmbeddings(List.of(chunk), List.of()));
    }

    @Test
    void emptyInputSkipsDatabase() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        new ChunkStore(jdbc).saveEmbeddings(List.of(), List.of());
        verifyNoInteractions(jdbc);
    }
}
