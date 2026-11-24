package com.docmind.indexer;

import java.util.zip.CRC32;

/**
 * Deterministic, dependency-free {@link Embedder} for tests and local development until
 * the DJL/ONNX model is wired in. Uses the hashing trick: each lowercased word adds or
 * subtracts 1 in a bucket derived from its hash, and the result is L2-normalized. The
 * same input always yields the same vector, and texts sharing words land closer
 * together than unrelated texts, which is all the pipeline tests need.
 */
public class DeterministicEmbedder implements Embedder {

    public static final int DEFAULT_DIMENSIONS = 384;

    private final int dimensions;

    public DeterministicEmbedder() {
        this(DEFAULT_DIMENSIONS);
    }

    public DeterministicEmbedder(int dimensions) {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be positive, got " + dimensions);
        }
        this.dimensions = dimensions;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimensions];
        if (text == null || text.isBlank()) {
            return vector;
        }
        for (String token : text.toLowerCase().split("[^a-z0-9]+")) {
            if (token.isEmpty()) {
                continue;
            }
            int bucket = Math.floorMod(token.hashCode(), dimensions);
            vector[bucket] += sign(token);
        }
        return l2Normalize(vector);
    }

    private static int sign(String token) {
        CRC32 crc = new CRC32();
        crc.update(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return (crc.getValue() & 1) == 0 ? 1 : -1;
    }

    private static float[] l2Normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0) {
            return vector;
        }
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
        return vector;
    }
}
