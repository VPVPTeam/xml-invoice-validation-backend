package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EmptyBatchException;
import com.vpvpteam.xmlinvoicevalidationbackend.formats.FormatProcessorRegistry;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BatchFileEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.BatchStatus;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.PersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.messaging.BatchSubmittedEvent;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.BatchStatusSummary;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.XmlFileData;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.BatchFileRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.ValidationBatchRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Accepts a batch for asynchronous validation: stores it as PENDING together with its files
 * and announces it once the transaction has committed.
 */
@Service
@AllArgsConstructor
public class BatchSubmissionService {
    private final FormatProcessorRegistry formatProcessorRegistry;
    private final ValidationBatchRepository batchRepository;
    private final BatchFileRepository batchFileRepository;
    private final PersistenceMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BatchStatusSummary submit(List<XmlFileData> xmlFilesData, String format) {
        ensureBatchIsNotEmpty(xmlFilesData);
        formatProcessorRegistry.ensureFormatSupported(format);

        ValidationBatchEntity batchEntity = batchRepository.save(newPendingBatch(format));
        batchFileRepository.saveAll(toFileEntities(xmlFilesData, batchEntity));

        eventPublisher.publishEvent(new BatchSubmittedEvent(batchEntity.getBatchId()));

        return mapper.toStatusSummary(batchEntity);
    }

    private void ensureBatchIsNotEmpty(List<XmlFileData> xmlFilesData) {
        if (xmlFilesData == null || xmlFilesData.isEmpty()) {
            throw new EmptyBatchException("Batch has no XML files to validate");
        }
    }

    private ValidationBatchEntity newPendingBatch(String format) {
        ValidationBatchEntity batchEntity = new ValidationBatchEntity();

        batchEntity.setBatchId(UUID.randomUUID().toString());
        batchEntity.setFormat(format);
        batchEntity.setStatus(BatchStatus.PENDING);
        batchEntity.setCreatedAt(OffsetDateTime.now());

        return batchEntity;
    }

    private List<BatchFileEntity> toFileEntities(List<XmlFileData> xmlFilesData, ValidationBatchEntity batchEntity) {
        return xmlFilesData.stream()
                .map(xmlFileData -> mapper.toEntity(xmlFileData, batchEntity))
                .toList();
    }
}