package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.RuleKeys;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Invoice counters of a single validation batch.
 * Duplicates are a separate axis and never reduce the number of valid invoices.
 */
public record BatchTotals(int totalInvoices,
                          int validInvoices,
                          int invoicesWithIssues,
                          int duplicateInvoices) {

    public static BatchTotals compute(List<String> invoiceIds,
                                      List<ValidationIssue> issues,
                                      int duplicateInvoices) {
        Set<String> distinctInvoiceIds = new HashSet<>(invoiceIds);

        Set<String> invoiceIdsWithRealIssues = issues.stream()
                .filter(issue -> !isDuplicateIssue(issue))
                .map(BatchTotals::invoiceIdOf)
                .filter(distinctInvoiceIds::contains)
                .collect(Collectors.toSet());

        int totalInvoices = distinctInvoiceIds.size();
        int invoicesWithIssues = invoiceIdsWithRealIssues.size();

        return new BatchTotals(
                totalInvoices,
                totalInvoices - invoicesWithIssues,
                invoicesWithIssues,
                duplicateInvoices
        );
    }

    private static boolean isDuplicateIssue(ValidationIssue issue) {
        return RuleKeys.DUPLICATE_IN_BATCH.equals(issue.getRuleKey())
                || RuleKeys.DUPLICATE_CROSS_BATCH.equals(issue.getRuleKey());
    }

    private static String invoiceIdOf(ValidationIssue issue) {
        return new InvoiceId(issue.getSellerTaxId(), issue.getInvoiceNumber()).value();
    }
}