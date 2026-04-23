package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Final batch-level validation result.
 * Keeps summary status, ids, issue list and counters.
 */
@Getter
@Setter
public class ValidationOutput {
    private final ValidationBatch validationBatch;

    /**
     * Overall result status for the whole batch: OK / WARNING / ERROR.
     */
    private Severity status = Severity.OK;

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

    public ValidationOutput(ValidationBatch validationBatch) {
        this.validationBatch = requireNonNull(validationBatch, "validationBatch must not be null");
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
        totalInvoices = validationBatch.getListOfInvoiceIds().size() + duplicateInvoices;
        validInvoices = validationBatch.getListOfInvoiceIds().size() - invoicesWithIssues;
    }
}