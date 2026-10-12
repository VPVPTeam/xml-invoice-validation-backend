package com.vpvpteam.xmlinvoicevalidationbackend.validation.messaging;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.BatchStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes a submitted batch to Kafka once its transaction has committed.
 * If Kafka is unreachable, the batch is marked FAILED instead of staying PENDING forever.
 */
@Component
public final class BatchSubmittedProducer {
    private static final Logger log = LoggerFactory.getLogger(BatchSubmittedProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BatchStatusService batchStatusService;
    private final String topic;

    public BatchSubmittedProducer(KafkaTemplate<String, String> kafkaTemplate,
                                  BatchStatusService batchStatusService,
                                  @Value("${app.kafka.topics.batch-submitted}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.batchStatusService = batchStatusService;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBatchSubmitted(BatchSubmittedEvent event) {
        String batchId = event.batchId();

        try {
            kafkaTemplate.send(topic, batchId, batchId).join();
            log.info("Batch {} published to topic {}", batchId, topic);
        } catch (RuntimeException ex) {
            log.error("Cannot publish batch {} to Kafka, marking it as FAILED", batchId, ex);
            batchStatusService.markFailed(batchId);
        }
    }
}