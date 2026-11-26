package com.docmind.api;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Declares the document-created topic so a fresh broker is ready without manual setup. */
@Configuration
public class KafkaTopicConfig {

    public static final String DOCUMENT_CREATED_TOPIC = "docmind.document-created";

    @Bean
    public NewTopic documentCreatedTopic(
            @Value("${docmind.topics.document-created:" + DOCUMENT_CREATED_TOPIC + "}") String topic) {
        // One replica: local development runs a single broker.
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
