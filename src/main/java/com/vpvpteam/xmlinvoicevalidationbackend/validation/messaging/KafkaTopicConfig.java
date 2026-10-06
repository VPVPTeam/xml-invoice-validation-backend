package com.vpvpteam.xmlinvoicevalidationbackend.validation.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    private static final int PARTITIONS = 1;
    private static final int REPLICAS = 1;

    @Bean
    public NewTopic batchSubmittedTopic(@Value("${app.kafka.topics.batch-submitted}") String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}