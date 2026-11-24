package com.docmind.indexer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DeterministicEmbedderTest {

    private final DeterministicEmbedder embedder = new DeterministicEmbedder();

    @Test
    void sameInputAlwaysProducesSameVector() {
        float[] first = embedder.embed("Kafka consumers read events at their own pace");
        float[] second = embedder.embed("Kafka consumers read events at their own pace");

        assertArrayEquals(first, second);
    }

    @Test
    void vectorHasConfiguredDimensions() {
        assertEquals(384, embedder.dimensions());
        assertEquals(384, embedder.embed("hello").length);
        assertEquals(16, new DeterministicEmbedder(16).embed("hello").length);
    }

    @Test
    void nonBlankInputIsUnitLength() {
        float[] vector = embedder.embed("some ordinary words about documents");

        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        assertEquals(1.0, Math.sqrt(norm), 1e-5);
    }

    @Test
    void blankInputIsZeroVector() {
        for (String blank : new String[] {null, "", "   "}) {
            float[] vector = embedder.embed(blank);
            assertEquals(384, vector.length);
            for (float value : vector) {
                assertEquals(0f, value);
            }
        }
    }

    @Test
    void differentInputsProduceDifferentVectors() {
        assertFalse(java.util.Arrays.equals(
                embedder.embed("annual leave policy"),
                embedder.embed("kafka consumer groups")));
    }

    @Test
    void textsSharingWordsEmbedCloserThanUnrelatedTexts() {
        float[] a = embedder.embed("kafka consumers read events");
        float[] b = embedder.embed("kafka consumers process events");
        float[] unrelated = embedder.embed("quantum chromodynamics lattice");

        assertTrue(cosine(a, b) > cosine(a, unrelated) + 0.2,
                "overlapping texts should be noticeably closer than unrelated ones");
    }

    @Test
    void embedAllPreservesOrderAndSize() {
        List<String> texts = List.of("one", "two", "three");

        List<float[]> vectors = embedder.embedAll(texts);

        assertEquals(3, vectors.size());
        assertArrayEquals(embedder.embed("one"), vectors.get(0));
        assertArrayEquals(embedder.embed("three"), vectors.get(2));
    }

    @Test
    void rejectsNonPositiveDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new DeterministicEmbedder(0));
        assertThrows(IllegalArgumentException.class, () -> new DeterministicEmbedder(-5));
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot; // both vectors are unit-length, so dot product is cosine
    }
}
