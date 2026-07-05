package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.DuplicateIssueMessages;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private void calculateBatchTotals() {
        Set<String> distinctInvoiceIds = new HashSet<>(validationBatch.getListOfInvoiceIds());

        Set<String> invoiceIdsWithRealIssues = issues.stream()
                .filter(issue -> !isDuplicateIssue(issue))
                .map(this::invoiceId)
                .filter(distinctInvoiceIds::contains)
                .collect(Collectors.toSet());

        totalInvoices = distinctInvoiceIds.size();
        invoicesWithIssues = invoiceIdsWithRealIssues.size();
        validInvoices = totalInvoices - invoicesWithIssues;
    }

    private boolean isDuplicateIssue(ValidationIssue issue) {
        return DuplicateIssueMessages.RULE_DUPLICATE_IN_BATCH.equals(issue.getRuleKey())
                || DuplicateIssueMessages.RULE_DUPLICATE_CROSS_BATCH.equals(issue.getRuleKey());
    }

    private String invoiceId(ValidationIssue issue) {
        return issue.getSellerTaxId() + "|" + issue.getInvoiceNumber();
    }
}