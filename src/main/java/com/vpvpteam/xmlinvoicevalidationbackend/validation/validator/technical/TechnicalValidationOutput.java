package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.technical;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.InvoiceId;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import lombok.Getter;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Output of the technical validation step.
 * Immutable: either a canonical invoice (mapping succeeded) or a list of technical issues.
 */
@Getter
public final class TechnicalValidationOutput {
    private final InvoiceId invoiceId;
    private final CanonicalInvoice canonicalInvoice;
    private final List<ValidationIssue> issues;

    private TechnicalValidationOutput(InvoiceId invoiceId,
                                      CanonicalInvoice canonicalInvoice,
                                      List<ValidationIssue> issues) {
        this.invoiceId = requireNonNull(invoiceId, "invoiceId must not be null");
        this.canonicalInvoice = canonicalInvoice;
        this.issues = List.copyOf(requireNonNull(issues, "issues must not be null"));
    }

    public static TechnicalValidationOutput success(InvoiceId invoiceId, CanonicalInvoice canonicalInvoice) {
        return new TechnicalValidationOutput(
                invoiceId,
                requireNonNull(canonicalInvoice, "canonicalInvoice must not be null"),
                List.of()
        );
    }

    public static TechnicalValidationOutput failure(InvoiceId invoiceId, List<ValidationIssue> issues) {
        return new TechnicalValidationOutput(invoiceId, null, issues);
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> issue.getSeverity() == Severity.ERROR);
    }
}