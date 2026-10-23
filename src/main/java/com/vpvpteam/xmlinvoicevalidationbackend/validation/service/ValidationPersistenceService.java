package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityNotFoundException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.BatchStatus;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.PersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationBatch;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.ValidationBatchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * Stores the result of a validation batch and marks the batch as COMPLETED in the same transaction.
 */
@Service
public class ValidationPersistenceService {
    @PersistenceContext
    private EntityManager entityManager;

    private final PersistenceMapper mapper;
    private final ValidationBatchRepository batchRepository;

    public ValidationPersistenceService(PersistenceMapper mapper, ValidationBatchRepository batchRepository) {
        this.mapper = mapper;
        this.batchRepository = batchRepository;
    }

    @Transactional
    public void complete(ValidationOutput output) {
        ValidationBatch batch = output.getValidationBatch();
        ValidationBatchEntity batchEntity = getBatchEntityOrThrow(batch.getBatchId());

        batchEntity.setVendorIds(new ArrayList<>(batch.getListOfVendorIds()));
        batchEntity.setInvoiceIds(new ArrayList<>(batch.getListOfInvoiceIds()));
        batchEntity.setStatus(BatchStatus.COMPLETED);

        ValidationOutputEntity outputEntity = mapper.toEntity(output, batchEntity);
        entityManager.persist(outputEntity);
    }

    private ValidationBatchEntity getBatchEntityOrThrow(String batchId) {
        return batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));
    }
}