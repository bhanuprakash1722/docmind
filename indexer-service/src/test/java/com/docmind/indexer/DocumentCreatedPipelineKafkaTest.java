package com.docmind.indexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

/**
 * End-to-end pipeline test on an embedded Kafka broker: a document-created event
 * published as JSON (the wire contract the api-service produces) is consumed, chunked,
 * embedded with the deterministic embedder and written to the chunk store. The store is
 * a mock because this test owns the Kafka path, not PostgreSQL; the chunk store is
 * covered separately against the schema.
 *
 * <p>The event is also published a second time to prove redelivery is safe: chunk ids
 * are derived from (documentId, chunkIndex), so the store sees the exact same rows.
 */
@SpringBootTest(properties = {
        "docmind.embedder=deterministic",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@EmbeddedKafka(partitions = 1, topics = "docmind.document-created")
class DocumentCreatedPipelineKafkaTest {

    private static final String TOPIC = "docmind.document-created";
    private static final long LISTENER_TIMEOUT_MS = 30_000;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @MockBean
    private ChunkStore chunkStore;

    private final TextChunker chunker = new TextChunker();
    private final DeterministicEmbedder embedder = new DeterministicEmbedder();

    /** Serializes like the api-service does (Spring Boot defaults: ISO-8601 dates). */
    private final ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void consumesEventChunksEmbedsAndStoresAndSurvivesRedelivery() throws Exception {
        UUID documentId = UUID.randomUUID();
        String body = longBody();
        List<TextChunk> expectedChunks = chunker.chunk(body);
        assertThat(expectedChunks.size())
                .as("fixture body must exercise multi-chunk indexing")
                .isGreaterThan(1);

        String json = mapper.writeValueAsString(
                new DocumentCreatedEvent(documentId, "Pipeline notes", body, Instant.now()));
        KafkaTemplate<String, String> template = producerTemplate();
        template.send(TOPIC, documentId.toString(), json).get(10, TimeUnit.SECONDS);

        ArgumentCaptor<List<ChunkRecord>> saved = chunkListCaptor();
        verify(chunkStore, timeout(LISTENER_TIMEOUT_MS)).saveAll(saved.capture());
        List<ChunkRecord> records = saved.getValue();
        assertThat(records).hasSize(expectedChunks.size());
        for (int i = 0; i < records.size(); i++) {
            ChunkRecord record = records.get(i);
            assertThat(record.id()).isEqualTo(IndexingService.chunkId(documentId, i));
            assertThat(record.documentId()).isEqualTo(documentId);
            assertThat(record.chunkIndex()).isEqualTo(i);
            assertThat(record.content()).isEqualTo(expectedChunks.get(i).text());
        }

        ArgumentCaptor<List<ChunkRecord>> embedded = chunkListCaptor();
        ArgumentCaptor<List<float[]>> vectors = vectorListCaptor();
        verify(chunkStore, timeout(LISTENER_TIMEOUT_MS))
                .saveEmbeddings(embedded.capture(), vectors.capture());
        assertThat(embedded.getValue()).isEqualTo(records);
        List<float[]> written = vectors.getValue();
        assertThat(written).hasSize(records.size());
        for (int i = 0; i < written.size(); i++) {
            assertThat(written.get(i)).hasSize(embedder.dimensions());
            assertThat(written.get(i)).isEqualTo(embedder.embed(records.get(i).content()));
        }

        // Kafka redelivery of the same event must rewrite the exact same rows.
        template.send(TOPIC, documentId.toString(), json).get(10, TimeUnit.SECONDS);
        ArgumentCaptor<List<ChunkRecord>> redelivered = chunkListCaptor();
        verify(chunkStore, timeout(LISTENER_TIMEOUT_MS).times(2)).saveAll(redelivered.capture());
        assertThat(redelivered.getAllValues().get(1))
                .isEqualTo(redelivered.getAllValues().get(0));
    }

    private KafkaTemplate<String, String> producerTemplate() {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<ChunkRecord>> chunkListCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<float[]>> vectorListCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    /** A body long enough for the chunker to split into several overlapping windows. */
    private static String longBody() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            sb.append("Sentence ").append(i)
                    .append(" describes step ").append(i % 9)
                    .append(" of the local vector search indexing pipeline. ");
        }
        return sb.toString();
    }
}
