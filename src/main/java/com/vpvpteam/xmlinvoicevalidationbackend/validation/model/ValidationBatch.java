package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Identity and contents of one validation batch.
 * The identity (batchId, createdAt) is assigned when the batch is submitted;
 * the id lists are fixed once its validation is complete.
 */
@Getter
public final class ValidationBatch {
    private final String batchId;
    private final OffsetDateTime createdAt;
    private final List<String> listOfVendorIds;
    private final List<String> listOfInvoiceIds;

    private ValidationBatch(String batchId,
                            OffsetDateTime createdAt,
                            List<String> listOfVendorIds,
                            List<String> listOfInvoiceIds) {
        this.batchId = batchId;
        this.createdAt = createdAt;
        this.listOfVendorIds = List.copyOf(requireNonNull(listOfVendorIds, "listOfVendorIds must not be null"));
        this.listOfInvoiceIds = List.copyOf(requireNonNull(listOfInvoiceIds, "listOfInvoiceIds must not be null"));
    }

    public static ValidationBatch restored(String batchId,
                                           OffsetDateTime createdAt,
                                           List<String> listOfVendorIds,
                                           List<String> listOfInvoiceIds) {
        return new ValidationBatch(
                requireNonNull(batchId, "batchId must not be null"),
                createdAt,
                listOfVendorIds,
                listOfInvoiceIds
        );
    }
}