package com.vpvpteam.xmlinvoicevalidationbackend.validation.repository;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BusinessRuleRepository extends JpaRepository<BusinessRuleEntity, Long> {
    List<BusinessRuleEntity> findByVendor_TaxId(String taxId);
    Optional<BusinessRuleEntity> findByVendor_IdAndRuleKey(Long vendorId, String ruleKey);
    Optional<BusinessRuleEntity> findByVendor_IdAndFieldPath(Long vendorId, String fieldPath);

    @Query("SELECT r FROM BusinessRuleEntity r JOIN FETCH r.vendor v WHERE v.taxId IN :taxIds")
    List<BusinessRuleEntity> findAllByVendorTaxIdIn(Collection<String> taxIds);
}