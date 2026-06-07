package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityAlreadyExistsException;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityNotFoundException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.VendorEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.ValidationPersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.BusinessRule;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.Vendor;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.BusinessRuleRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.VendorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidationPersistenceService {
    @PersistenceContext
    private EntityManager entityManager;

    private final ValidationPersistenceMapper mapper;
    private final VendorRepository vendorRepository;
    private final BusinessRuleRepository businessRuleRepository;

    public ValidationPersistenceService(ValidationPersistenceMapper mapper, VendorRepository vendorRepository, BusinessRuleRepository businessRuleRepository) {
        this.mapper = mapper;
        this.vendorRepository = vendorRepository;
        this.businessRuleRepository = businessRuleRepository;
    }

    // ValidationOutput
    @Transactional
    public void save(ValidationOutput output) {
        ValidationBatchEntity batchEntity = mapper.toEntity(output.getValidationBatch());
        entityManager.persist(batchEntity);

        ValidationOutputEntity outputEntity = mapper.toEntity(output, batchEntity);
        entityManager.persist(outputEntity);
    }

    // Vendor
    @Transactional
    public void save(Vendor vendor) {
        checkVendorNotExistsOrThrow(vendor.getTaxId());
        vendorRepository.save(mapper.toEntity(vendor));
    }

    @Transactional
    public void update(Vendor vendor) {
        VendorEntity vendorEntity = getVendorEntityOrThrow(vendor.getTaxId());
        vendorEntity.setName(vendor.getName());
    }

    @Transactional
    public void deleteVendor(String taxId) {
        VendorEntity vendorEntity = getVendorEntityOrThrow(taxId);
        vendorRepository.delete(vendorEntity);
    }

    // BusinessRule
    @Transactional
    public void save(BusinessRule rule) {
        VendorEntity vendorEntity = getVendorEntityOrThrow(rule.getVendorTaxId());
        BusinessRuleEntity ruleEntity = mapper.toEntity(rule, vendorEntity);

        businessRuleRepository.save(ruleEntity);
    }

    @Transactional
    public void update(BusinessRule rule) {
        VendorEntity vendorEntity = getVendorEntityOrThrow(rule.getVendorTaxId());
        BusinessRuleEntity ruleEntity = getRuleEntityOrThrow(vendorEntity.getId(), rule.getRuleKey());

        ruleEntity.setFieldPath(rule.getFieldPath());
        ruleEntity.setOperator(rule.getOperator());
        ruleEntity.setExpectedValue(rule.getExpectedValue());
        ruleEntity.setCreatedBy(rule.getCreatedBy());
    }

    @Transactional
    public void delete(String vendorTaxId, String ruleKey) {
        VendorEntity vendorEntity = getVendorEntityOrThrow(vendorTaxId);
        BusinessRuleEntity ruleEntity = getRuleEntityOrThrow(vendorEntity.getId(), ruleKey);

        businessRuleRepository.delete(ruleEntity);
    }

    // Util methods
    private VendorEntity getVendorEntityOrThrow(String taxId) {
        return vendorRepository.findByTaxId(taxId)
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: " + taxId));
    }

    private BusinessRuleEntity getRuleEntityOrThrow(Long vendorId, String ruleKey) {
        return businessRuleRepository
                .findByVendor_IdAndRuleKey(vendorId, ruleKey)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + ruleKey));
    }

    private void checkVendorNotExistsOrThrow(String vendorTaxId) {
        if (vendorRepository.findByTaxId(vendorTaxId).isPresent()) {
            throw new EntityAlreadyExistsException("Vendor already exists:  " + vendorTaxId);
        }
    }
}