package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.BatchStatus;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.PersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.XmlFileData;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.BatchFileRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.ValidationBatchRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Takes one submitted batch from PENDING to COMPLETED: loads its files, validates them
 * and stores the result. Safe to call more than once for the same batch.
 */
@Service
@AllArgsConstructor
public final class BatchProcessingService {
    private static final Logger log = LoggerFactory.getLogger(BatchProcessingService.class);

    private final ValidationBatchRepository batchRepository;
    private final BatchFileRepository batchFileRepository;
    private final BatchStatusService batchStatusService;
    private final ValidationService validationService;
    private final ValidationPersistenceService persistenceService;
    private final PersistenceMapper mapper;

    public void process(String batchId) {
        Optional<ValidationBatchEntity> foundBatch = batchRepository.findByBatchId(batchId);

        if (foundBatch.isEmpty()) {
            log.warn("Batch {} does not exist, message skipped", batchId);
            return;
        }

        ValidationBatchEntity batchEntity = foundBatch.get();

        if (isAlreadyFinished(batchEntity)) {
            log.info("Batch {} is already {}, message skipped", batchId, batchEntity.getStatus());
            return;
        }

        batchStatusService.markProcessing(batchId);

        ValidationOutput output = validationService.validate(
                batchId,
                batchEntity.getCreatedAt(),
                batchEntity.getFormat(),
                loadFiles(batchId)
        );

        persistenceService.complete(output);
        log.info("Batch {} validated with result {}", batchId, output.getStatus());
    }

    private static boolean isAlreadyFinished(ValidationBatchEntity batchEntity) {
        return batchEntity.getStatus() == BatchStatus.COMPLETED
                || batchEntity.getStatus() == BatchStatus.FAILED;
    }

    private List<XmlFileData> loadFiles(String batchId) {
        return batchFileRepository.findAllByValidationBatch_BatchIdOrderByIdAsc(batchId).stream()
                .map(mapper::toModel)
                .toList();
    }
}