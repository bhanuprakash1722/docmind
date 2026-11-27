package com.docmind.indexer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chooses the embedder: the real local model by default, the deterministic one on request. */
@Configuration
public class EmbedderConfig {

    @Bean(destroyMethod = "close")
    public Embedder embedder(@Value("${docmind.embedder:djl}") String kind) {
        if ("deterministic".equalsIgnoreCase(kind)) {
            return new NoCloseEmbedder(new DeterministicEmbedder());
        }
        return new DjlEmbedder();
    }

    /** Adapts an Embedder with nothing to release to the bean's close() destroy method. */
    static final class NoCloseEmbedder implements Embedder, AutoCloseable {
        private final Embedder delegate;

        NoCloseEmbedder(Embedder delegate) {
            this.delegate = delegate;
        }

        @Override
        public int dimensions() {
            return delegate.dimensions();
        }

        @Override
        public float[] embed(String text) {
            return delegate.embed(text);
        }

        @Override
        public void close() {
        }
    }
}
