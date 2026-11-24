package com.docmind.indexer;

import java.util.List;

/**
 * Computes embedding vectors for text. Implementations run models locally (DJL/ONNX)
 * or stand in deterministically for tests. The vector dimension is fixed per
 * implementation and must match the pgvector column.
 */
public interface Embedder {

    /** Embedding vector dimension, constant for the implementation. */
    int dimensions();

    /** Embeds one text into a vector of {@link #dimensions()} floats. */
    float[] embed(String text);

    /** Embeds a batch in order. */
    default List<float[]> embedAll(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }
}
