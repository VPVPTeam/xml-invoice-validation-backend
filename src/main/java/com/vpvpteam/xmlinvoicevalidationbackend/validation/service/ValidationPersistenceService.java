package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.PersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores the result of a validation batch.
 */
@Service
public class ValidationPersistenceService {
    @PersistenceContext
    private EntityManager entityManager;

    private final PersistenceMapper mapper;

    public ValidationPersistenceService(PersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void save(ValidationOutput output) {
        ValidationBatchEntity batchEntity = mapper.toEntity(output.getValidationBatch());
        entityManager.persist(batchEntity);

        ValidationOutputEntity outputEntity = mapper.toEntity(output, batchEntity);
        entityManager.persist(outputEntity);
    }
}