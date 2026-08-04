package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.technical;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.RuleKeys;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.TechnicalIssueMessages;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.InvoiceId;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects technical issues found while validating a single file.
 * Holds the file name and the invoice identity, so that each check only has to
 * state its own condition and field path.
 */
final class TechnicalIssueCollector {
    private final String fileName;
    private final InvoiceId invoiceId;
    private final List<ValidationIssue> issues = new ArrayList<>();

    TechnicalIssueCollector(String fileName, InvoiceId invoiceId) {
        this.fileName = fileName;
        this.invoiceId = invoiceId;
    }

    void checkRequired(boolean condition, String fieldPath) {
        if (!condition) {
            addMissingField(fieldPath);
        }
    }

    void addMissingField(String fieldPath) {
        issues.add(ValidationIssue.technicalError(
                fileName,
                invoiceId,
                RuleKeys.TECH_MISSING_REQUIRED_FIELD,
                fieldPath,
                TechnicalIssueMessages.missingRequiredField(fieldPath)
        ));
    }

    boolean hasErrors() {
        return issues.stream().anyMatch(issue -> issue.getSeverity() == Severity.ERROR);
    }

    List<ValidationIssue> issues() {
        return List.copyOf(issues);
    }
}