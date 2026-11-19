# docmind

Semantic document search in Java: upload notes and documents, then search them by meaning, not keywords. All AI runs locally on CPU - no paid APIs, no keys, works offline.

Two Spring Boot services:

- **api-service** - REST API: document CRUD, chunking, and the semantic search endpoint. Publishes `document-created` events to Kafka and queries pgvector for nearest chunks.
- **indexer-service** - consumes `document-created` events and computes sentence embeddings locally with Deep Java Library (sentence-transformers/all-MiniLM-L6-v2, ONNX Runtime), storing vectors in PostgreSQL/pgvector.

## Status

Day 2: document CRUD is implemented and unit-tested - create, list, get, update and delete via `/api/documents`, with 404s on unknown ids. Chunking, embedding inference and search land over the next weeks per ROADMAP.md. The document endpoints below work as shown; `/api/search` still describes the target API.

## Stack

Java 17, Spring Boot 3, Spring Data JPA + JdbcTemplate, PostgreSQL 16 with pgvector, Apache Kafka, Deep Java Library (ONNX Runtime), JUnit 5, Mockito, Docker Compose, GitHub Actions.

## Why local AI?

Embeddings are computed in-process. No API keys, no usage cost, and the model can be swapped for any ONNX-exported sentence transformer behind the `Embedder` interface.

## Run locally

```bash
docker compose up -d            # postgres+pgvector and kafka
./mvnw -pl indexer-service spring-boot:run
./mvnw -pl api-service spring-boot:run
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
