package com.vpvpteam.xmlinvoicevalidationbackend.validation.repository;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessRuleRepository extends JpaRepository<BusinessRuleEntity, Long> {

    List<BusinessRuleEntity> findByVendor_TaxId(String taxId);

    /* TODO: добивить функционал
    Optional<BusinessRuleEntity> findByVendor_IdAndFieldPath(Long vendorId, String fieldPath);
    Optional<BusinessRuleEntity> findByVendor_IdAndRuleKey(Long vendorId, String ruleKey);
     */
}