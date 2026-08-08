package com.vpvpteam.xmlinvoicevalidationbackend.validation.service;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.InvoiceId;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationBatch;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationOutput;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Accumulates the result of one batch while its files are being validated:
 * issues, seen identifiers and the duplicate counter.
 * Not thread-safe: one instance belongs to one batch.
 */
final class BatchAccumulator {
    private final List<ValidationIssue> issues = new ArrayList<>();
    private final Set<String> vendorIds = new LinkedHashSet<>();
    private final Set<String> invoiceIds = new LinkedHashSet<>();
    private int duplicateInvoices = 0;

    /**
     * Remembers the invoice and its vendor.
     * Returns false if the same invoice has already been seen in this batch.
     */
    boolean registerInvoice(InvoiceId invoiceId) {
        vendorIds.add(invoiceId.sellerTaxId());

        return invoiceIds.add(invoiceId.value());
    }

    void addIssue(ValidationIssue issue) {
        issues.add(issue);
    }

    void addIssues(List<ValidationIssue> newIssues) {
        issues.addAll(newIssues);
    }

    void addDuplicateIssue(ValidationIssue issue) {
        issues.add(issue);
        duplicateInvoices++;
    }

    ValidationOutput toOutput() {
        ValidationBatch batch = ValidationBatch.created(
                new ArrayList<>(vendorIds),
                new ArrayList<>(invoiceIds)
        );

        return ValidationOutput.of(batch, issues, duplicateInvoices);
    }
}