package com.vpvpteam.xmlinvoicevalidationbackend.validation.repository;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationIssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationIssueRepository extends JpaRepository<ValidationIssueEntity, Long> {
    List<ValidationIssueEntity> findBySellerTaxIdAndInvoiceNumber(String sellerTaxId, String invoiceNumber);
}