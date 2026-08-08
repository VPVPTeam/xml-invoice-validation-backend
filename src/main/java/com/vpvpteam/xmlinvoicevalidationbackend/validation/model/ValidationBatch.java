package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Identity and contents of one validation batch.
 * Immutable: the id lists are fixed at the moment the batch is completed.
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

    public static ValidationBatch created(List<String> listOfVendorIds, List<String> listOfInvoiceIds) {
        return new ValidationBatch(
                UUID.randomUUID().toString(),
                OffsetDateTime.now(),
                listOfVendorIds,
                listOfInvoiceIds
        );
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