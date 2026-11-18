# Architecture

```
client
  |  POST /api/documents
  v
api-service ---- PostgreSQL (documents, chunks)
  |                ^  pgvector: chunks.embedding vector(384)
  |                |
  |  DocumentCreated (Kafka: document-created)   | INSERT/SELECT vectors
  v                |
indexer-service ---+
  local embedding: DJL + all-MiniLM-L6-v2 (ONNX, CPU)

client
  |  POST /api/search {query}
  v
api-service: embed query (same model) -> SELECT ... ORDER BY embedding <=> query LIMIT k
```

- api-service owns documents and chunking; chunk text is written synchronously, embeddings asynchronously.
- Indexing is idempotent on chunkId: redelivered events do not create duplicate vectors.
- Search embeds the query with the identical model so vectors live in the same space.
- Embedder is an interface: tests use a deterministic fake; a hosted model could replace the local one without touching callers.
