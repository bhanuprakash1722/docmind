package com.docmind.indexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes document-created events and hands them to the indexing service. */
@Component
public class DocumentCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentCreatedListener.class);

    private final IndexingService indexingService;

    public DocumentCreatedListener(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @KafkaListener(topics = "${docmind.topics.document-created:docmind.document-created}")
    public void onDocumentCreated(DocumentCreatedEvent event) {
        int count = indexingService.index(event);
        log.info("Indexed document {} into {} chunks", event.documentId(), count);
    }
}
