package com.docmind.indexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IndexingServiceTest {

    private final ChunkStore store = mock(ChunkStore.class);
    private final IndexingService service = new IndexingService(store, new DeterministicEmbedder());

    private static DocumentCreatedEvent event(UUID id, String body) {
        return new DocumentCreatedEvent(id, "title", body, Instant.now());
    }

    @Test
    @SuppressWarnings("unchecked")
    void storesChunksAndEmbeddingsInOrder() {
        String body = "Employees get twelve days of leave. ".repeat(40);
        UUID id = UUID.randomUUID();

        int count = service.index(event(id, body));

        ArgumentCaptor<List<ChunkRecord>> chunks = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<float[]>> vectors = ArgumentCaptor.forClass(List.class);
        verify(store).saveAll(chunks.capture());
        verify(store).saveEmbeddings(any(), vectors.capture());
        assertTrue(count > 1);
        assertEquals(count, chunks.getValue().size());
        assertEquals(count, vectors.getValue().size());
        for (int i = 0; i < count; i++) {
            assertEquals(i, chunks.getValue().get(i).chunkIndex());
            assertEquals(id, chunks.getValue().get(i).documentId());
            assertEquals(384, vectors.getValue().get(i).length);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void chunkIdsAreStableAcrossRedelivery() {
        UUID id = UUID.randomUUID();
        service.index(event(id, "Same body."));
        service.index(event(id, "Same body."));

        ArgumentCaptor<List<ChunkRecord>> chunks = ArgumentCaptor.forClass(List.class);
        verify(store, org.mockito.Mockito.times(2)).saveAll(chunks.capture());
        assertEquals(chunks.getAllValues().get(0).get(0).id(), chunks.getAllValues().get(1).get(0).id());
        assertEquals(IndexingService.chunkId(id, 0), chunks.getAllValues().get(0).get(0).id());
    }

    @Test
    void blankBodyStoresNothing() {
        assertEquals(0, service.index(event(UUID.randomUUID(), "   ")));
        verifyNoInteractions(store);
    }

    @Test
    void differentChunksGetDifferentIds() {
        UUID id = UUID.randomUUID();
        assertTrue(!IndexingService.chunkId(id, 0).equals(IndexingService.chunkId(id, 1)));
        verify(store, never()).saveAll(any());
    }
}
