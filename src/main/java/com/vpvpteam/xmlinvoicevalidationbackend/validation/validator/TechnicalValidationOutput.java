package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Output of technical validation step.
 * Contains canonical invoice (if mapping succeeded)
 * and list of technical issues found during validation.
 */
@Getter
@Setter
@NoArgsConstructor
public final class TechnicalValidationOutput {

    /**
     * Null when technical validation/mapping failed.
     */
    private CanonicalInvoice canonicalInvoice;

    /**
     * Technical issues found during validation.
     */
    private List<ValidationIssue> issues = new ArrayList<>();

    /**
     * Creates output with invoice + issue list.
     * Makes defensive copy of incoming issues list.
     */
    public TechnicalValidationOutput(CanonicalInvoice canonicalInvoice, List<ValidationIssue> issues) {
        this.canonicalInvoice = canonicalInvoice;
        this.issues = Objects.requireNonNullElse(issues, new ArrayList<>());
    }

    /**
     * Factory method for successful technical validation.
     */
    public static TechnicalValidationOutput success(CanonicalInvoice canonicalInvoice) {
        return new TechnicalValidationOutput(canonicalInvoice, List.of());
    }

    /**
     * Factory method for failed technical validation.
     */
    public static TechnicalValidationOutput failure(List<ValidationIssue> issues) {
        return new TechnicalValidationOutput(null, issues);
    }

    /**
     * Returns true if output contains at least one ERROR issue.
     */
    public boolean hasErrors() {
        return issues != null && issues.stream().anyMatch(i -> i.getSeverity() == Severity.ERROR);
    }

    /**
     * Returns true when invoice exists and there are no ERROR issues.
     */
    public boolean isValid() {
        return canonicalInvoice != null && !hasErrors();
    }
}