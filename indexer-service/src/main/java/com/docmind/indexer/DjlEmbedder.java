package com.docmind.indexer;

import ai.djl.ModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import java.io.IOException;

/**
 * Embedder backed by a local sentence-transformers model (all-MiniLM-L6-v2, 384 dims)
 * running on ONNX Runtime through Deep Java Library. The model downloads from the DJL
 * model zoo on first {@link #embed(String)} and is cached under ~/.djl.ai - no API keys
 * and no network after the first load. Mean pooling and L2 normalization are applied by
 * the translator, so returned vectors are unit length.
 */
public class DjlEmbedder implements Embedder, AutoCloseable {

    public static final String DEFAULT_MODEL_URL =
            "djl://ai.djl.huggingface.onnxruntime/sentence-transformers/all-MiniLM-L6-v2";
    public static final int DIMENSIONS = 384;

    private final String modelUrl;
    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;

    public DjlEmbedder() {
        this(DEFAULT_MODEL_URL);
    }

    public DjlEmbedder(String modelUrl) {
        if (modelUrl == null || modelUrl.isBlank()) {
            throw new IllegalArgumentException("modelUrl must not be blank");
        }
        this.modelUrl = modelUrl;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @Override
    public synchronized float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[DIMENSIONS];
        }
        if (predictor == null) {
            load();
        }
        try {
            return predictor.predict(text);
        } catch (TranslateException e) {
            throw new IllegalStateException("Embedding inference failed", e);
        }
    }

    private void load() {
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls(modelUrl)
                .optEngine("OnnxRuntime")
                .optArgument("pooling", "mean")
                .optArgument("normalize", "true")
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .build();
        try {
            model = criteria.loadModel();
        } catch (IOException | ModelException e) {
            throw new IllegalStateException("Could not load embedding model from " + modelUrl, e);
        }
        predictor = model.newPredictor();
    }

    /** True once the model is in memory. Kept package-visible for tests via accessor below. */
    public boolean isLoaded() {
        return predictor != null;
    }

    @Override
    public synchronized void close() {
        if (predictor != null) {
            predictor.close();
            predictor = null;
        }
        if (model != null) {
            model.close();
            model = null;
        }
    }
}
