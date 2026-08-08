package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import lombok.Getter;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Final batch-level validation result.
 * Immutable: status and counters are consistent with the issue list from the moment of creation.
 */
@Getter
public final class ValidationOutput {
    private final ValidationBatch validationBatch;
    private final Severity status;
    private final List<ValidationIssue> issues;
    private final int totalInvoices;
    private final int validInvoices;
    private final int invoicesWithIssues;
    private final int duplicateInvoices;

    private ValidationOutput(ValidationBatch validationBatch,
                             Severity status,
                             List<ValidationIssue> issues,
                             BatchTotals totals) {
        this.validationBatch = requireNonNull(validationBatch, "validationBatch must not be null");
        this.status = requireNonNull(status, "status must not be null");
        this.issues = issues;
        this.totalInvoices = totals.totalInvoices();
        this.validInvoices = totals.validInvoices();
        this.invoicesWithIssues = totals.invoicesWithIssues();
        this.duplicateInvoices = totals.duplicateInvoices();
    }

    public static ValidationOutput of(ValidationBatch validationBatch,
                                      List<ValidationIssue> issues,
                                      int duplicateInvoices) {
        requireNonNull(validationBatch, "validationBatch must not be null");
        List<ValidationIssue> safeIssues = List.copyOf(requireNonNull(issues, "issues must not be null"));

        return new ValidationOutput(
                validationBatch,
                resolveStatus(safeIssues),
                safeIssues,
                BatchTotals.compute(validationBatch.getListOfInvoiceIds(), safeIssues, duplicateInvoices)
        );
    }

    public static ValidationOutput restored(ValidationBatch validationBatch,
                                            Severity status,
                                            List<ValidationIssue> issues,
                                            BatchTotals totals) {
        return new ValidationOutput(
                validationBatch,
                status,
                List.copyOf(requireNonNull(issues, "issues must not be null")),
                requireNonNull(totals, "totals must not be null")
        );
    }

    private static Severity resolveStatus(List<ValidationIssue> issues) {
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == Severity.ERROR)) {
            return Severity.ERROR;
        }
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == Severity.WARNING)) {
            return Severity.WARNING;
        }
        return Severity.OK;
    }
}