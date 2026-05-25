package com.vpvpteam.xmlinvoicevalidationbackend.validation.repository;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessRuleRepository extends JpaRepository<BusinessRuleEntity, Long> {

    List<BusinessRuleEntity> findByVendor_TaxId(String taxId);

    Optional<BusinessRuleEntity> findByVendor_IdAndRuleKey(Long vendorId, String ruleKey);

    /* TODO: добивить функционал
    Optional<BusinessRuleEntity> findByVendor_IdAndFieldPath(Long vendorId, String fieldPath);
     */
}