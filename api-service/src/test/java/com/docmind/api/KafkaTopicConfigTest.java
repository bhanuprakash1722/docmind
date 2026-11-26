package com.docmind.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

class KafkaTopicConfigTest {

    @Test
    void documentCreatedTopicUsesConfiguredName() {
        NewTopic topic = new KafkaTopicConfig().documentCreatedTopic("custom.topic");

        assertEquals("custom.topic", topic.name());
        assertEquals(3, topic.numPartitions());
        assertEquals(1, topic.replicationFactor());
    }

    @Test
    void defaultTopicNameIsStable() {
        assertEquals("docmind.document-created", KafkaTopicConfig.DOCUMENT_CREATED_TOPIC);
    }
}
