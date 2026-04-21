package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Final batch-level validation result.
 * Keeps summary status, ids, issue list and counters.
 */
@Getter
@Setter
@NoArgsConstructor
public class ValidationOutput {
    /**
     * Batch id (upload/package id).
     */
    private String batchId;

    /**
     * Overall result status for the whole batch: OK / WARNING / ERROR.
     */
    private Severity status = Severity.OK;

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
    private int totalInvoices = 0;

    /**
     * Number of invoices with no blocking errors.
     */
    private int validInvoices = 0;

    /**
     * Number of invoices with blocking errors/warnings.
     */
    private int invoicesWithIssues = 0;

    /**
     * Number of duplicates found in the same batch.
     */
    private int duplicateInvoices = 0;

    /**
     * Result creation timestamp.
     */
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ValidationOutput(String batchId,
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

    /**
     * Prepares final output summary before returning it to API/client.
     */
    public void prepareFinalReport() {
        resolveStatus();
        calculateBatchTotals();
    }

    /**
     * Increments duplicate invoice counter for current batch.
     * Called when the same invoiceId appears more than once.
     */
    public void increaseDuplicateInvoicesCount() {
        duplicateInvoices++;
    }

    /**
     * Resolves final batch status:
     * ERROR > WARNING > OK
     */
    private void resolveStatus() {
        // 1) No issues means batch is OK.
        if (issues == null || issues.isEmpty()) { return; }

        if (issues.stream().anyMatch(i -> i.getSeverity() == Severity.ERROR)) {
            // 2) Any ERROR makes final status ERROR.
            setStatus(Severity.ERROR);
        } else if (issues.stream().anyMatch(i -> i.getSeverity() == Severity.WARNING)) {
            // 3) if no ERROR but at least one WARNING, status is WARNING.
            setStatus(Severity.WARNING);
        }
    }

    /**
     * Calculates final batch counters based on collected invoice ids and issues.
     * Uses unique invoice ids from issues to avoid counting the same invoice multiple times.
     */
    private void calculateBatchTotals() {
        HashSet<String> invoiceIdsWithIssues = issues.stream()
                .map(issue -> issue.getSellerTaxId() + "|" + issue.getInvoiceNumber())
                .collect(Collectors.toCollection(HashSet::new));

        invoicesWithIssues = invoiceIdsWithIssues.size() - duplicateInvoices;
        totalInvoices = listOfInvoiceIds.size() + duplicateInvoices;
        validInvoices = listOfInvoiceIds.size() - invoicesWithIssues;
    }
}