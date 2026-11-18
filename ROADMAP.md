# Roadmap

One meaningful step per day.

## Week 1 - api-service core
1. Repo scaffold: README, compose, CI, aggregator pom, service skeletons
2. Document entity + repository + CRUD endpoints
3. Text chunker (sentence-aware, ~500 char windows, overlap) + unit tests
4. Validation + error handling + controller slice tests
5. pgvector schema (chunks table) + JdbcTemplate chunk store
6. Dockerfiles for both services

## Week 2 - embeddings + pipeline
7. Embedder interface + deterministic fake for tests
8. DJL local embedder (all-MiniLM-L6-v2 ONNX, mean pooling, L2 normalize)
9. Kafka topic config + DocumentCreated event + producer in api-service
10. indexer-service consumer: embed chunks, store vectors (idempotent on chunkId)
11. Integration test with EmbeddedKafka + fake embedder
12. First end-to-end local run documented

## Week 3 - search + hardening
13. POST /api/search: query embedding + pgvector cosine top-k
14. Search ranking tests (fixtures with known vectors)
15. Title/tag filters + pagination on search
16. Actuator health/metrics, embedding latency metric
17. Second AI feature: extractive keyword tagging of chunks
18. Architecture diagram + README polish
