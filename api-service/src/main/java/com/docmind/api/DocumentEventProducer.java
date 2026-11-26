package com.docmind.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes document lifecycle events. Sending is asynchronous: a broker outage is logged
 * but never fails the API request, because the document is already durably stored.
 */
@Component
public class DocumentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(DocumentEventProducer.class);

    private final KafkaTemplate<String, DocumentCreatedEvent> kafkaTemplate;
    private final String documentCreatedTopic;

    public DocumentEventProducer(
            KafkaTemplate<String, DocumentCreatedEvent> kafkaTemplate,
            @Value("${docmind.topics.document-created:" + KafkaTopicConfig.DOCUMENT_CREATED_TOPIC + "}")
                    String documentCreatedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.documentCreatedTopic = documentCreatedTopic;
    }

    public void publishDocumentCreated(Document document) {
        DocumentCreatedEvent event = DocumentCreatedEvent.from(document);
        // Key by document id so all events for one document stay ordered on one partition.
        kafkaTemplate.send(documentCreatedTopic, document.getId().toString(), event)
                .whenComplete((result, failure) -> {
                    if (failure != null) {
                        log.error("Failed to publish document-created event for {}", document.getId(),
                                failure);
                    } else {
                        log.info("Published document-created event for {} to {}",
                                document.getId(), documentCreatedTopic);
                    }
                });
    }
}
