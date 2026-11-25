package com.docmind.indexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests that never load the ONNX model - inference is covered by
 * {@code DjlEmbedderIT}, which runs on demand (mvn -Dtest=DjlEmbedderIT test) since it
 * downloads the model on first run.
 */
class DjlEmbedderTest {

    @Test
    void exposes384DimensionsWithoutLoadingModel() {
        try (DjlEmbedder embedder = new DjlEmbedder()) {
            assertEquals(384, embedder.dimensions());
            assertEquals(384, embedder.embed("").length, "blank input must short-circuit before model load");
            assertFalse(embedder.isLoaded(), "blank input must not trigger the model download");
        }
    }

    @Test
    void nullInputReturnsZeroVectorWithoutLoading() {
        try (DjlEmbedder embedder = new DjlEmbedder()) {
            assertEquals(384, embedder.embed(null).length);
            assertFalse(embedder.isLoaded());
        }
    }

    @Test
    void rejectsBlankModelUrl() {
        assertThrows(IllegalArgumentException.class, () -> new DjlEmbedder(" "));
    }

    @Test
    void closeIsIdempotentBeforeLoad() {
        DjlEmbedder embedder = new DjlEmbedder();
        embedder.close();
        embedder.close();
    }
}
