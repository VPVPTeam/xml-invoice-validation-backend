package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalFieldRegistry;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityAlreadyExistsException;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityNotFoundException;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.InvalidRuleExpectedValueException;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.UnsupportedFieldPathException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.VendorEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.ValidationPersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.BusinessRule;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.Vendor;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.BusinessRuleRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.VendorRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business.OperatorEvaluator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidationPersistenceService {
    @PersistenceContext
    private EntityManager entityManager;

    private final ValidationPersistenceMapper mapper;
    private final VendorRepository vendorRepository;
    private final BusinessRuleRepository businessRuleRepository;
    private final OperatorEvaluator operatorEvaluator;

    public ValidationPersistenceService(ValidationPersistenceMapper mapper,
                                        VendorRepository vendorRepository,
                                        BusinessRuleRepository businessRuleRepository,
                                        OperatorEvaluator operatorEvaluator) {
        this.mapper = mapper;
        this.vendorRepository = vendorRepository;
        this.businessRuleRepository = businessRuleRepository;
        this.operatorEvaluator = operatorEvaluator;
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
    public Vendor save(Vendor vendor) {
        checkVendorNotExistsOrThrow(vendor.getTaxId());
        VendorEntity saved = vendorRepository.save(mapper.toEntity(vendor));
        return mapper.toModel(saved);
    }

    @Transactional
    public Vendor update(Vendor vendor) {
        VendorEntity vendorEntity = getVendorEntityOrThrow(vendor.getTaxId());
        vendorEntity.setName(vendor.getName());
        return mapper.toModel(vendorEntity);
    }

    @Transactional
    public void deleteVendor(String taxId) {
        VendorEntity vendorEntity = getVendorEntityOrThrow(taxId);
        vendorRepository.delete(vendorEntity);
    }

    // BusinessRule
    @Transactional
    public BusinessRule save(BusinessRule rule) {
        VendorEntity vendorEntity = getVendorEntityOrThrow(rule.getVendorTaxId());

        checkFieldPathSupportedOrThrow(rule.getFieldPath());
        checkExpectedValueValidOrThrow(rule.getOperator(), rule.getExpectedValue());
        checkRuleKeyUniqueForVendorOrThrow(vendorEntity.getId(), rule.getRuleKey());
        checkRuleFieldPathUniqueForVendorOrThrow(vendorEntity.getId(), rule.getFieldPath());

        BusinessRuleEntity saved = businessRuleRepository.save(mapper.toEntity(rule, vendorEntity));
        return mapper.toModel(saved);
    }

    @Transactional
    public BusinessRule update(BusinessRule rule) {
        VendorEntity vendorEntity = getVendorEntityOrThrow(rule.getVendorTaxId());
        BusinessRuleEntity ruleEntity = getRuleEntityOrThrow(vendorEntity.getId(), rule.getRuleKey());

        if (!ruleEntity.getFieldPath().equals(rule.getFieldPath())) {
            checkFieldPathSupportedOrThrow(rule.getFieldPath());
            checkRuleFieldPathUniqueForVendorOrThrow(vendorEntity.getId(), rule.getFieldPath());
        }

        checkExpectedValueValidOrThrow(rule.getOperator(), rule.getExpectedValue());

        ruleEntity.setFieldPath(rule.getFieldPath());
        ruleEntity.setOperator(rule.getOperator());
        ruleEntity.setExpectedValue(rule.getExpectedValue());
        ruleEntity.setCreatedBy(rule.getCreatedBy());

        return mapper.toModel(ruleEntity);
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
            throw new EntityAlreadyExistsException("Vendor already exists: " + vendorTaxId);
        }
    }

    private void checkRuleKeyUniqueForVendorOrThrow(Long vendorId, String ruleKey) {
        if (businessRuleRepository.findByVendor_IdAndRuleKey(vendorId, ruleKey).isPresent()) {
            throw new EntityAlreadyExistsException("Rule with key '" + ruleKey + "' already exists for this vendor");
        }
    }

    private void checkRuleFieldPathUniqueForVendorOrThrow(Long vendorId, String fieldPath) {
        if (businessRuleRepository.findByVendor_IdAndFieldPath(vendorId, fieldPath).isPresent()) {
            throw new EntityAlreadyExistsException("Rule for field path '" + fieldPath + "' already exists for this vendor");
        }
    }

    private void checkFieldPathSupportedOrThrow(String fieldPath) {
        if (!CanonicalFieldRegistry.isSupported(fieldPath)) {
            throw new UnsupportedFieldPathException(fieldPath);
        }
    }

    private void checkExpectedValueValidOrThrow(RuleOperator operator, String expectedValue) {
        if (!operatorEvaluator.isExpectedValueValid(operator, expectedValue)) {
            throw new InvalidRuleExpectedValueException("Invalid expected value '" + expectedValue + "' for operator " + operator);
        }
    }
}