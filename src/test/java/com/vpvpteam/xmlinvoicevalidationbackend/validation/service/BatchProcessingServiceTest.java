package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BatchFileEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.BatchStatus;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.PersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationBatch;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.XmlFileData;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.BatchFileRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.ValidationBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchProcessingServiceTest {

    private static final String BATCH_ID = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";
    private static final String FORMAT = "KSEF";
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-01-15T10:00:00Z");

    @Mock
    private ValidationBatchRepository batchRepository;

    @Mock
    private BatchFileRepository batchFileRepository;

    @Mock
    private BatchStatusService batchStatusService;

    @Mock
    private ValidationService validationService;

    @Mock
    private ValidationPersistenceService persistenceService;

    @Captor
    private ArgumentCaptor<List<XmlFileData>> filesCaptor;

    private BatchProcessingService service;

    @BeforeEach
    void setUp() {
        service = new BatchProcessingService(
                batchRepository,
                batchFileRepository,
                batchStatusService,
                validationService,
                persistenceService,
                new PersistenceMapper()
        );
    }

    @Test
    void process_withUnknownBatch_doesNothing() {
        when(batchRepository.findByBatchId(BATCH_ID)).thenReturn(Optional.empty());

        service.process(BATCH_ID);

        verifyNoInteractions(batchStatusService, validationService, persistenceService);
    }

    @ParameterizedTest
    @EnumSource(value = BatchStatus.class, names = {"COMPLETED", "FAILED"})
    void process_withFinishedBatch_skipsItOnRedelivery(BatchStatus status) {
        when(batchRepository.findByBatchId(BATCH_ID)).thenReturn(Optional.of(batch(status)));

        service.process(BATCH_ID);

        verifyNoInteractions(batchStatusService, validationService, persistenceService);
    }

    @ParameterizedTest
    @EnumSource(value = BatchStatus.class, names = {"PENDING", "PROCESSING"})
    void process_withUnfinishedBatch_validatesFilesAndCompletesBatch(BatchStatus status) {
        ValidationOutput output = emptyOutput();

        when(batchRepository.findByBatchId(BATCH_ID)).thenReturn(Optional.of(batch(status)));
        when(batchFileRepository.findAllByValidationBatch_BatchIdOrderByIdAsc(BATCH_ID))
                .thenReturn(List.of(file("first.xml"), file("second.xml")));
        when(validationService.validate(eq(BATCH_ID), eq(CREATED_AT), eq(FORMAT), anyList()))
                .thenReturn(output);

        service.process(BATCH_ID);

        InOrder inOrder = inOrder(batchStatusService, validationService, persistenceService);
        inOrder.verify(batchStatusService).markProcessing(BATCH_ID);
        inOrder.verify(validationService).validate(eq(BATCH_ID), eq(CREATED_AT), eq(FORMAT), filesCaptor.capture());
        inOrder.verify(persistenceService).complete(output);

        assertThat(filesCaptor.getValue())
                .extracting(XmlFileData::fileName)
                .containsExactly("first.xml", "second.xml");
    }

    private static ValidationBatchEntity batch(BatchStatus status) {
        ValidationBatchEntity batch = new ValidationBatchEntity();

        batch.setBatchId(BATCH_ID);
        batch.setFormat(FORMAT);
        batch.setStatus(status);
        batch.setCreatedAt(CREATED_AT);

        return batch;
    }

    private static BatchFileEntity file(String fileName) {
        BatchFileEntity file = new BatchFileEntity();

        file.setFileName(fileName);
        file.setContent("<Faktura/>".getBytes(StandardCharsets.UTF_8));

        return file;
    }

    private static ValidationOutput emptyOutput() {
        return ValidationOutput.of(
                ValidationBatch.restored(BATCH_ID, CREATED_AT, List.of(), List.of()),
                List.of(),
                0
        );
    }
}