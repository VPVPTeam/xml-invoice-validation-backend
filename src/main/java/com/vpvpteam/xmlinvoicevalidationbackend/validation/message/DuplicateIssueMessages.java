package com.vpvpteam.xmlinvoicevalidationbackend.validation.message;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class DuplicateIssueMessages {
    public static final String RULE_DUPLICATE_IN_BATCH = "DUPLICATE_IN_BATCH";
    public static final String RULE_DUPLICATE_CROSS_BATCH = "DUPLICATE_CROSS_BATCH";

    private DuplicateIssueMessages() {}

    public static String duplicateInBatch(String sellerTaxId, String invoiceNumber) {
        return "WARNING\n" +
                "Seller Tax ID: " + sellerTaxId + "; Invoice Number: " + invoiceNumber +
                "\nDuplicate invoice in the same batch (rule: DUPLICATE_IN_BATCH)";
    }

    public static String duplicateCrossBatch(String sellerTaxId,
                                             String invoiceNumber,
                                             Map<String, OffsetDateTime> batchesByInvoiceId) {
        if (batchesByInvoiceId == null || batchesByInvoiceId.isEmpty()) {
            return "WARNING\n" +
                    "Seller Tax ID: " + sellerTaxId + "; Invoice Number: " + invoiceNumber +
                    "\nDuplicate invoice found in previous batch (rule: DUPLICATE_CROSS_BATCH)";
        }
        Map.Entry<String, OffsetDateTime> latest = batchesByInvoiceId.entrySet().iterator().next();
        String latestBatchId = latest.getKey();
        OffsetDateTime latestCreatedAt = latest.getValue();
        String latestCreatedAtText = latestCreatedAt == null
                ? "UNKNOWN"
                : latestCreatedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return "WARNING\n" +
                "Seller Tax ID: " + sellerTaxId + "; Invoice Number: " + invoiceNumber +
                "\nDuplicate invoice found in previous batch(es)." +
                "\nFound in " + batchesByInvoiceId.size() + " batch(es)." +
                "\nLatest batch: " + latestBatchId + " at " + latestCreatedAtText +
                "\n(rule: DUPLICATE_CROSS_BATCH)";
    }
}