package com.vpvpteam.xmlinvoicevalidationbackend.validation.message;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class DuplicateIssueMessages {

    private DuplicateIssueMessages() {}

    public static String duplicateInBatch() {
        return "Duplicate invoice within the same batch.";
    }

    public static String duplicateCrossBatch(Map<String, OffsetDateTime> batchesByInvoiceId) {
        if (batchesByInvoiceId == null || batchesByInvoiceId.isEmpty()) {
            return "Duplicate invoice found in a previous batch.";
        }

        Map.Entry<String, OffsetDateTime> latest = batchesByInvoiceId.entrySet().iterator().next();
        String latestCreatedAt = latest.getValue() == null
                ? "UNKNOWN"
                : latest.getValue().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return "Duplicate invoice found in " + batchesByInvoiceId.size()
                + " previous batch(es); latest: " + latest.getKey() + " at " + latestCreatedAt + ".";
    }
}