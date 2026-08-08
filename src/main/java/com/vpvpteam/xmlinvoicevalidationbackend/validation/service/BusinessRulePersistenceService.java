package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalFieldRegistry;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityAlreadyExistsException;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityNotFoundException;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.InvalidRuleExpectedValueException;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.UnsupportedFieldPathException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.VendorEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.mapper.PersistenceMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.BusinessRule;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.BusinessRuleRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.VendorRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business.OperatorEvaluator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write operations on business rules.
 */
@Service
@AllArgsConstructor
public class BusinessRulePersistenceService {
    private final PersistenceMapper mapper;
    private final BusinessRuleRepository businessRuleRepository;
    private final VendorRepository vendorRepository;
    private final OperatorEvaluator operatorEvaluator;

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

    private VendorEntity getVendorEntityOrThrow(String taxId) {
        return vendorRepository.findByTaxId(taxId)
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: " + taxId));
    }

    private BusinessRuleEntity getRuleEntityOrThrow(Long vendorId, String ruleKey) {
        return businessRuleRepository
                .findByVendor_IdAndRuleKey(vendorId, ruleKey)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + ruleKey));
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