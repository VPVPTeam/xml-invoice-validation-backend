package com.vpvpteam.xmlinvoicevalidationbackend.validation.validators;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TechnicalValidationOutput {

    private CanonicalInvoice canonicalInvoice;
    private List<ValidationIssue> issues = new ArrayList<>();

    public TechnicalValidationOutput(CanonicalInvoice canonicalInvoice, List<ValidationIssue> issues) {
        this.canonicalInvoice = canonicalInvoice;
        this.issues = issues == null ? new ArrayList<>() : new ArrayList<>(issues);
    }

    public static TechnicalValidationOutput success(CanonicalInvoice canonicalInvoice) {
        return new TechnicalValidationOutput(canonicalInvoice, List.of());
    }

    public static TechnicalValidationOutput failure(List<ValidationIssue> issues) {
        return new TechnicalValidationOutput(null, issues);
    }

    public boolean hasErrors() {
        return issues != null && issues.stream().anyMatch(i -> i.getSeverity() == Severity.ERROR);
    }

    public boolean isValid() {
        return canonicalInvoice != null && !hasErrors();
    }
}