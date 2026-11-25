package com.docmind.indexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Integration test that really loads all-MiniLM-L6-v2 (downloads ~90 MB on first run,
 * cached under ~/.djl.ai afterwards). Surefire does not pick up *IT classes in the
 * normal build; run on demand with:
 *
 *   mvn -pl indexer-service -Dtest=DjlEmbedderIT test
 */
class DjlEmbedderIT {

    @Test
    void loadsModelAndRanksByMeaning() {
        try (DjlEmbedder embedder = new DjlEmbedder()) {
            float[] leave = embedder.embed("Employees get twelve days of annual leave");
            float[] holiday = embedder.embed("How much holiday can I take");
            float[] unrelated = embedder.embed("Kafka partition rebalancing protocol");

            assertEquals(DjlEmbedder.DIMENSIONS, leave.length);
            assertTrue(embedder.isLoaded());
            assertTrue(cosine(leave, holiday) > cosine(leave, unrelated),
                    "semantically related texts must score higher than unrelated ones");
        }
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot; // translator normalizes, so dot product is cosine similarity
    }
}
