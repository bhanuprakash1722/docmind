package com.docmind.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class DocumentEventProducerTest {

    @Mock
    private KafkaTemplate<String, DocumentCreatedEvent> kafkaTemplate;

    @Test
    void publishesToDocumentCreatedTopicKeyedByDocumentId() {
        DocumentEventProducer producer =
                new DocumentEventProducer(kafkaTemplate, KafkaTopicConfig.DOCUMENT_CREATED_TOPIC);
        Document document = new Document("Runbook", "restart the broker");
        when(kafkaTemplate.send(eq(KafkaTopicConfig.DOCUMENT_CREATED_TOPIC), eq(document.getId().toString()),
                org.mockito.ArgumentMatchers.any(DocumentCreatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.publishDocumentCreated(document);

        ArgumentCaptor<DocumentCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(DocumentCreatedEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopicConfig.DOCUMENT_CREATED_TOPIC),
                eq(document.getId().toString()), eventCaptor.capture());
        DocumentCreatedEvent event = eventCaptor.getValue();
        assertEquals(document.getId(), event.documentId());
        assertEquals("Runbook", event.title());
        assertEquals("restart the broker", event.body());
        org.junit.jupiter.api.Assertions.assertNotNull(event.occurredAt());
    }

    @Test
    void toleratesBrokerFailureWithoutThrowing() {
        DocumentEventProducer producer =
                new DocumentEventProducer(kafkaTemplate, KafkaTopicConfig.DOCUMENT_CREATED_TOPIC);
        CompletableFuture<SendResult<String, DocumentCreatedEvent>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(DocumentCreatedEvent.class)))
                .thenReturn(failed);

        // Must not propagate: the API request already succeeded.
        producer.publishDocumentCreated(new Document("t", "b"));
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(DocumentCreatedEvent.class));
    }
}
