package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public final class ValidationBatch {
    private final String batchId;
    private final OffsetDateTime createdAt;
    private List<String> listOfVendorIds = new ArrayList<>();
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
