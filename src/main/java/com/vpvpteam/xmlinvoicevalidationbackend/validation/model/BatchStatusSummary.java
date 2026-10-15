package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.BatchStatus;

import java.time.OffsetDateTime;

public record BatchStatusSummary(String batchId, BatchStatus status, OffsetDateTime createdAt) {
}