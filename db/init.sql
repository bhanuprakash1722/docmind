CREATE EXTENSION IF NOT EXISTS vector;

-- One row per document chunk. embedding stays NULL until the indexer computes it
-- (384 dimensions = sentence-transformers/all-MiniLM-L6-v2 output size).
CREATE TABLE IF NOT EXISTS chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(384),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS chunks_document_id_idx ON chunks (document_id);
