package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import java.time.OffsetDateTime;

public record BatchSummary(String batchId, OffsetDateTime createdAt) {
}