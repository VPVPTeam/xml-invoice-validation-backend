package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Final batch-level validation result.
 * Keeps summary status, ids, issue list and counters.
 */
@Getter
@Setter
@NoArgsConstructor
public class ValidationResult {
    /**
     * Batch id (upload/package id).
     */
    private String batchId;

    /**
     * Overall result status for the whole batch: OK / WARNING / ERROR.
     */
    private Severity status;

    /**
     * Unique seller ids found in this batch.
     */
    private List<String> listOfVendorIds = new ArrayList<>();

    /**
     * Unique invoice ids.<br>
     * Format: sellerTaxId + "|" + invoiceNumber
     */
    private List<String> listOfInvoiceIds = new ArrayList<>();

    /**
     * All issues collected during validation.
     */
    private List<ValidationIssue> issues = new ArrayList<>();

    /**
     * Total number of invoices processed in the batch.
     */
    private int totalInvoices;

    /**
     * Number of invoices with no blocking errors.
     */
    private int validInvoices;

    /**
     * Number of invoices with blocking errors/warnings.
     */
    private int invoicesWithIssues;

    /**
     * Number of duplicates found in the same batch.
     */
    private int duplicateInvoices;

    /**
     * Result creation timestamp.
     */
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ValidationResult(String batchId,
                            Severity status,
                            List<String> listOfVendorIds,
                            List<String> listOfInvoiceIds,
                            List<ValidationIssue> issues,
                            int totalInvoices,
                            int validInvoices,
                            int invoicesWithIssues,
                            int duplicateInvoices,
                            OffsetDateTime createdAt) {
        this.batchId = batchId;
        this.status = status;
        this.listOfVendorIds = listOfVendorIds == null ? new ArrayList<>() : new ArrayList<>(listOfVendorIds);
        this.listOfInvoiceIds = listOfInvoiceIds == null ? new ArrayList<>() : new ArrayList<>(listOfInvoiceIds);
        this.issues = issues == null ? new ArrayList<>() : new ArrayList<>(issues);
        this.totalInvoices = totalInvoices;
        this.validInvoices = validInvoices;
        this.invoicesWithIssues = invoicesWithIssues;
        this.duplicateInvoices = duplicateInvoices;
        this.createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }
}