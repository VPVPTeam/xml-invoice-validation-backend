package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.RuleKeys;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BatchTotalsTest {

    private static final InvoiceId INVOICE_A = new InvoiceId("1111111111", "INV-A");
    private static final InvoiceId INVOICE_B = new InvoiceId("2222222222", "INV-B");
    private static final InvoiceId INVOICE_C = new InvoiceId("3333333333", "INV-C");

    @Test
    void compute_withEmptyBatch_returnsZeros() {
        BatchTotals totals = BatchTotals.compute(List.of(), List.of(), 0);

        assertThat(totals).isEqualTo(new BatchTotals(0, 0, 0, 0));
    }

    @Test
    void compute_withCleanInvoice_countsItAsValid() {
        BatchTotals totals = BatchTotals.compute(
                List.of(INVOICE_A.value()),
                List.of(),
                0
        );

        assertThat(totals).isEqualTo(new BatchTotals(1, 1, 0, 0));
    }

    @Test
    void compute_withTechnicalIssue_countsInvoiceAsFaulty() {
        BatchTotals totals = BatchTotals.compute(
                List.of(INVOICE_A.value()),
                List.of(technicalIssue(INVOICE_A)),
                0
        );

        assertThat(totals).isEqualTo(new BatchTotals(1, 0, 1, 0));
    }

    @Test
    void compute_withSeveralIssuesOnSameInvoice_countsInvoiceOnce() {
        BatchTotals totals = BatchTotals.compute(
                List.of(INVOICE_A.value()),
                List.of(technicalIssue(INVOICE_A), technicalIssue(INVOICE_A), businessIssue(INVOICE_A)),
                0
        );

        assertThat(totals).isEqualTo(new BatchTotals(1, 0, 1, 0));
    }

    @Test
    void compute_withDuplicateInBatch_keepsInvoiceValid() {
        BatchTotals totals = BatchTotals.compute(
                List.of(INVOICE_A.value()),
                List.of(duplicateIssue(INVOICE_A, RuleKeys.DUPLICATE_IN_BATCH)),
                1
        );

        assertThat(totals).isEqualTo(new BatchTotals(1, 1, 0, 1));
    }

    @Test
    void compute_withDuplicateFromPreviousBatch_keepsInvoiceValid() {
        BatchTotals totals = BatchTotals.compute(
                List.of(INVOICE_A.value()),
                List.of(duplicateIssue(INVOICE_A, RuleKeys.DUPLICATE_CROSS_BATCH)),
                1
        );

        assertThat(totals).isEqualTo(new BatchTotals(1, 1, 0, 1));
    }

    @Test
    void compute_withDuplicateAndRealIssueOnSameInvoice_countsInvoiceAsFaulty() {
        BatchTotals totals = BatchTotals.compute(
                List.of(INVOICE_A.value()),
                List.of(duplicateIssue(INVOICE_A, RuleKeys.DUPLICATE_CROSS_BATCH), technicalIssue(INVOICE_A)),
                1
        );

        assertThat(totals).isEqualTo(new BatchTotals(1, 0, 1, 1));
    }

    @Test
    void compute_withIssueOnUnknownInvoice_ignoresIt() {
        BatchTotals totals = BatchTotals.compute(
                List.of(INVOICE_A.value()),
                List.of(technicalIssue(InvoiceId.NONE)),
                0
        );

        assertThat(totals).isEqualTo(new BatchTotals(1, 1, 0, 0));
    }

    @Test
    void compute_withRepeatedInvoiceIds_countsEachInvoiceOnce() {
        BatchTotals totals = BatchTotals.compute(
                List.of(INVOICE_A.value(), INVOICE_A.value(), INVOICE_B.value()),
                List.of(),
                0
        );

        assertThat(totals).isEqualTo(new BatchTotals(2, 2, 0, 0));
    }

    @Test
    void compute_withMixedBatch_countsEachAxisSeparately() {
        BatchTotals totals = BatchTotals.compute(
                List.of(INVOICE_A.value(), INVOICE_B.value(), INVOICE_C.value()),
                List.of(
                        technicalIssue(INVOICE_B),
                        duplicateIssue(INVOICE_A, RuleKeys.DUPLICATE_IN_BATCH),
                        businessIssue(INVOICE_C)
                ),
                1
        );

        assertThat(totals).isEqualTo(new BatchTotals(3, 1, 2, 1));
    }

    private static ValidationIssue technicalIssue(InvoiceId invoiceId) {
        return ValidationIssue.technicalError(
                "invoice.xml",
                invoiceId,
                RuleKeys.TECH_MISSING_REQUIRED_FIELD,
                "Fa.P_2",
                "Required field is missing"
        );
    }

    private static ValidationIssue businessIssue(InvoiceId invoiceId) {
        return ValidationIssue.businessWarning(
                "invoice.xml",
                invoiceId,
                "CURRENCY_MUST_BE_PLN",
                "totals.currencyCode",
                "Business rule violation"
        );
    }

    private static ValidationIssue duplicateIssue(InvoiceId invoiceId, String ruleKey) {
        return ValidationIssue.businessWarning(
                "invoice.xml",
                invoiceId,
                ruleKey,
                "invoiceId",
                "Duplicate invoice"
        );
    }
}