package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

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

import java.util.Optional;

@Service
@AllArgsConstructor
public class ValidationPersistenceService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ValidationPersistenceMapper mapper;
    private final VendorRepository vendorRepository;
    private final BusinessRuleRepository businessRuleRepository;

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
        boolean vendorExists = vendorRepository.findByTaxId(vendor.getTaxId()).isPresent();

        if (vendorExists) {
            return;
        }

        vendorRepository.save(mapper.toEntity(vendor));
    }

    @Transactional
    public void update(Vendor vendor) {
        Optional<VendorEntity> existing = vendorRepository.findByTaxId(vendor.getTaxId());
        existing.ifPresent(entity -> entity.setName(vendor.getName()));
    }

    // BusinessRule
    @Transactional
    public void save(BusinessRule rule) {
        VendorEntity vendorEntity = vendorRepository.findByTaxId(rule.getVendorTaxId())
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: " + rule.getVendorTaxId()));

        BusinessRuleEntity ruleEntity = mapper.toEntity(rule, vendorEntity);
        businessRuleRepository.save(ruleEntity);
    }

    // TODO: ВЫНЕСТИ ПОИСК ЭНТИТИ В ОТДЕЛЬНЫЕ ФУНКЦИИ
    @Transactional
    public void update(BusinessRule rule) {
        VendorEntity vendorEntity = vendorRepository.findByTaxId(rule.getVendorTaxId())
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: " + rule.getVendorTaxId()));

        BusinessRuleEntity ruleEntity = businessRuleRepository
                .findByVendor_IdAndRuleKey(vendorEntity.getId(), rule.getRuleKey())
                .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + rule.getRuleKey()));

        ruleEntity.setFieldPath(rule.getFieldPath());
        ruleEntity.setOperator(rule.getOperator());
        ruleEntity.setExpectedValue(rule.getExpectedValue());
        ruleEntity.setCreatedBy(rule.getCreatedBy());
    }

    @Transactional
    public void delete(String vendorTaxId, String ruleKey) {
        VendorEntity vendorEntity = vendorRepository.findByTaxId(vendorTaxId)
                .orElseThrow(() -> new EntityNotFoundException("Vendor not found: " + vendorTaxId));

        BusinessRuleEntity ruleEntity = businessRuleRepository
                .findByVendor_IdAndRuleKey(vendorEntity.getId(), ruleKey)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + ruleKey));

        businessRuleRepository.delete(ruleEntity);
    }
}