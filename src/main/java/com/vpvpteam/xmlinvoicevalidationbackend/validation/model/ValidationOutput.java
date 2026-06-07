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
public final class ValidationOutput {
    private final ValidationBatch validationBatch;

    private Severity status = Severity.OK;
    private List<ValidationIssue> issues = new ArrayList<>();
    private int totalInvoices = 0;
    private int validInvoices = 0;
    private int invoicesWithIssues = 0;
    private int duplicateInvoices = 0;

    public ValidationOutput(ValidationBatch validationBatch) {
        this.validationBatch = requireNonNull(validationBatch, "validationBatch must not be null");
    }

    public void prepareFinalReport() {
        resolveStatus();
        calculateBatchTotals();
    }

    public void increaseDuplicateInvoicesCount() {
        duplicateInvoices++;
    }

    private void resolveStatus() {
        if (issues == null || issues.isEmpty()) { return; }

        if (issues.stream().anyMatch(i -> i.getSeverity() == Severity.ERROR)) {
            setStatus(Severity.ERROR);
        } else if (issues.stream().anyMatch(i -> i.getSeverity() == Severity.WARNING)) {
            setStatus(Severity.WARNING);
        }
    }

    // TODO: НЕ УЧЛИ МИНУС (потенциально некорректная арифметика с дупликатами)
    private void calculateBatchTotals() {
        HashSet<String> invoiceIdsWithIssues = issues.stream()
                .map(issue -> issue.getSellerTaxId() + "|" + issue.getInvoiceNumber())
                .collect(Collectors.toCollection(HashSet::new));

        invoicesWithIssues = invoiceIdsWithIssues.size() - duplicateInvoices;
        totalInvoices = validationBatch.getListOfInvoiceIds().size() + duplicateInvoices;
        validInvoices = validationBatch.getListOfInvoiceIds().size() - invoicesWithIssues;
    }
}