package com.vpvpteam.xmlinvoicevalidationbackend.validation.messaging;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.BatchProcessingService;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.service.BatchStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BatchValidationConsumerTest {

    private static final String BATCH_ID = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";

    @Mock
    private BatchProcessingService batchProcessingService;

    @Mock
    private BatchStatusService batchStatusService;

    @InjectMocks
    private BatchValidationConsumer consumer;

    @Test
    void onBatchSubmitted_whenProcessingSucceeds_doesNotMarkBatchFailed() {
        consumer.onBatchSubmitted(BATCH_ID);

        verify(batchProcessingService).process(BATCH_ID);
        verifyNoInteractions(batchStatusService);
    }

    @Test
    void onBatchSubmitted_whenProcessingFails_marksBatchFailed() {
        doThrow(new IllegalStateException("Unexpected validation failure"))
                .when(batchProcessingService).process(BATCH_ID);

        consumer.onBatchSubmitted(BATCH_ID);

        verify(batchStatusService).markFailed(BATCH_ID);
    }
}