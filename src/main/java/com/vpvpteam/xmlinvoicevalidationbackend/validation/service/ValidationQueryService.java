package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationBatchEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationIssueEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.ValidationOutputEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.ValidationBatchRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.ValidationIssueRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.ValidationOutputRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class ValidationQueryService {

    private final ValidationOutputRepository outputRepository;
    private final ValidationBatchRepository batchRepository;
    private final ValidationIssueRepository issueRepository;

    public Optional<ValidationOutputEntity> getFullReport(String batchId) {
        return outputRepository.findByValidationBatch_BatchId(batchId);
    }

    public List<ValidationBatchEntity> getBatchesByVendor(String vendorId) {
        return batchRepository.findBatchesByVendorId(vendorId);
    }

    public List<ValidationIssueEntity> getIssuesByInvoice(String sellerTaxId, String invoiceNumber) {
        return issueRepository.findBySellerTaxIdAndInvoiceNumber(sellerTaxId, invoiceNumber);
    }
}