package com.vpvpteam.xmlinvoicevalidationbackend.validation.messaging;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.BatchStatusService;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchSubmittedProducerTest {

    private static final String TOPIC = "validation-batch-submitted";
    private static final String BATCH_ID = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private BatchStatusService batchStatusService;

    private BatchSubmittedProducer producer;

    @BeforeEach
    void setUp() {
        producer = new BatchSubmittedProducer(kafkaTemplate, batchStatusService, TOPIC);
    }

    @Test
    void onBatchSubmitted_sendsBatchIdAsKeyAndValue() {
        when(kafkaTemplate.send(TOPIC, BATCH_ID, BATCH_ID))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.onBatchSubmitted(new BatchSubmittedEvent(BATCH_ID));

        verify(kafkaTemplate).send(TOPIC, BATCH_ID, BATCH_ID);
        verifyNoInteractions(batchStatusService);
    }

    @Test
    void onBatchSubmitted_whenKafkaIsUnavailable_marksBatchFailed() {
        when(kafkaTemplate.send(TOPIC, BATCH_ID, BATCH_ID))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("Kafka is unavailable")));

        producer.onBatchSubmitted(new BatchSubmittedEvent(BATCH_ID));

        verify(batchStatusService).markFailed(BATCH_ID);
    }
}