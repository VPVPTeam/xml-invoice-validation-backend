package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ValidationBatch {
    /**
     * Batch id (upload/package id).
     */
    private final String batchId;

    /**
     * Result creation timestamp.
     */
    private final OffsetDateTime createdAt;

    /**
     * Unique seller ids found in this batch.
     */
    private List<String> listOfVendorIds = new ArrayList<>();

    /**
     * Unique invoice ids.<br>
     * Format: sellerTaxId + "|" + invoiceNumber
     */
    private List<String> listOfInvoiceIds = new ArrayList<>();

    public ValidationBatch() {
        this.batchId = UUID.randomUUID().toString();
        this.createdAt = OffsetDateTime.now();
    }

    public ValidationBatch(String batchId, OffsetDateTime createdAt) {
        this.batchId = batchId;
        this.createdAt = createdAt;
    }
}
