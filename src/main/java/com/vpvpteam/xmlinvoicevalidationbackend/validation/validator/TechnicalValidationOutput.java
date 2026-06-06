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

    private CanonicalInvoice canonicalInvoice;
    private List<ValidationIssue> issues = new ArrayList<>();
    private String sellerTaxId;
    private String invoiceNumber;

    private TechnicalValidationOutput(CanonicalInvoice canonicalInvoice, List<ValidationIssue> issues) {
        this.canonicalInvoice = canonicalInvoice;
        this.issues = Objects.requireNonNullElse(issues, new ArrayList<>());
    }

    public static TechnicalValidationOutput success(CanonicalInvoice canonicalInvoice) {
        return new TechnicalValidationOutput(canonicalInvoice, List.of());
    }

    public static TechnicalValidationOutput failure(List<ValidationIssue> issues) {

        return new TechnicalValidationOutput(null, issues);
    }

    public boolean hasErrors() {
        return issues != null && issues
                                    .stream()
                                    .anyMatch(i -> i.getSeverity() == Severity.ERROR);
    }

    public boolean isValid() {
        return canonicalInvoice != null && !hasErrors();
    }
}