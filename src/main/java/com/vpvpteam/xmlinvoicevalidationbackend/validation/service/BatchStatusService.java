package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityNotFoundException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.BatchStatus;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.ValidationBatchRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves a validation batch through its lifecycle statuses.
 */
@Service
@AllArgsConstructor
public class BatchStatusService {
    private final ValidationBatchRepository batchRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String batchId) {
        getBatchEntityOrThrow(batchId).setStatus(BatchStatus.FAILED);
    }

    private ValidationBatchEntity getBatchEntityOrThrow(String batchId) {
        return batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));
    }
}