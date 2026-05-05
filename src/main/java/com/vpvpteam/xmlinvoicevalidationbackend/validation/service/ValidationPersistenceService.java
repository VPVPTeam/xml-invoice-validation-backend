package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.ValidationPersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ValidationPersistenceService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ValidationPersistenceMapper mapper;

    @Transactional
    public void save(ValidationOutput output) {
        ValidationBatchEntity batchEntity = mapper.toBatchEntity(output);
        entityManager.persist(batchEntity);

        ValidationOutputEntity outputEntity = mapper.toOutputEntity(output, batchEntity);

        entityManager.persist(outputEntity);
    }
}