# docmind

Semantic document search in Java: upload notes and documents, then search them by meaning, not keywords. All AI runs locally on CPU - no paid APIs, no keys, works offline.

Two Spring Boot services:

- **api-service** - REST API: document CRUD, chunking, and the semantic search endpoint. Publishes `document-created` events to Kafka and queries pgvector for nearest chunks.
- **indexer-service** - consumes `document-created` events and computes sentence embeddings locally with Deep Java Library (sentence-transformers/all-MiniLM-L6-v2, ONNX Runtime), storing vectors in PostgreSQL/pgvector.

## Status

Day 10: document CRUD with validation and uniform API errors, a sentence-aware text chunker, the pgvector chunks schema with an idempotent chunk store, Docker images built in CI, the `Embedder` abstraction with two implementations (a deterministic hashing embedder for tests and local development, and `DjlEmbedder`, a real local model - sentence-transformers/all-MiniLM-L6-v2 on ONNX Runtime via DJL, mean pooling, L2-normalized, 384 dimensions), and the start of the event pipeline: creating a document publishes a JSON `DocumentCreatedEvent` to the `docmind.document-created` Kafka topic (declared automatically, keyed by document id; a broker outage is logged but never fails the request). The indexer-service now consumes that event, chunks the body, embeds each chunk and stores the vectors in pgvector; chunk ids are derived from document id and index, so redelivered events do not duplicate rows. This path is covered by unit tests with mocks; the EmbeddedKafka integration test and a full end-to-end run come on Days 11-12. The model is verified by an opt-in integration test (`mvn -pl indexer-service -Dtest=DjlEmbedderIT test`); it downloads ~90 MB on first run and caches under `~/.djl.ai`. The document endpoints below work as shown; `/api/search` still describes the target API.

## Stack

Java 17, Spring Boot 3, Spring Data JPA + JdbcTemplate, PostgreSQL 16 with pgvector, Apache Kafka, Deep Java Library (ONNX Runtime), JUnit 5, Mockito, Docker Compose, GitHub Actions.

## Why local AI?

Embeddings are computed in-process. No API keys, no usage cost, and the model can be swapped for any ONNX-exported sentence transformer behind the `Embedder` interface.

## Run locally

```bash
docker compose up -d postgres kafka   # infrastructure only
mvn -pl indexer-service spring-boot:run
mvn -pl api-service spring-boot:run
```

Or run the whole stack as containers:

```bash
docker compose up --build             # postgres, kafka, api-service (8081), indexer-service (8082)
```

## Try it

```bash
curl -X POST localhost:8081/api/documents \
  -H 'Content-Type: application/json' \
  -d '{"title":"Kafka notes","body":"Kafka is a distributed event log. Producers write records to topics and consumers read them at their own pace."}'

curl -X POST localhost:8081/api/search \
  -H 'Content-Type: application/json' \
  -d '{"query":"how do consumers read events?","limit":5}'
```

## Layout

```
api-service/        REST API, chunking, search queries
indexer-service/    Kafka consumer + local embedding inference
docs/architecture.md
.github/workflows/  CI: build + test on every push
```

## About this project

Portfolio project built incrementally (one meaningful step per day) with AI pair-programming assistance from an Instinct agent. Learning/portfolio work, not commercial or employment history.
