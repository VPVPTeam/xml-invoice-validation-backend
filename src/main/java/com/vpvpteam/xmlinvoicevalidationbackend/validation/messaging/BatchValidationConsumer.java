package com.vpvpteam.xmlinvoicevalidationbackend.validation.messaging;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.BatchProcessingService;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.BatchStatusService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens for submitted batches and hands each one over for validation.
 * An unexpected failure marks the batch as FAILED instead of leaving it in progress forever.
 */
@Component
@AllArgsConstructor
public final class BatchValidationConsumer {
    private static final Logger log = LoggerFactory.getLogger(BatchValidationConsumer.class);

    private final BatchProcessingService batchProcessingService;
    private final BatchStatusService batchStatusService;

    @KafkaListener(topics = "${app.kafka.topics.batch-submitted}")
    public void onBatchSubmitted(String batchId) {
        try {
            batchProcessingService.process(batchId);
        } catch (RuntimeException ex) {
            log.error("Validation of batch {} failed, marking it as FAILED", batchId, ex);
            batchStatusService.markFailed(batchId);
        }
    }
}