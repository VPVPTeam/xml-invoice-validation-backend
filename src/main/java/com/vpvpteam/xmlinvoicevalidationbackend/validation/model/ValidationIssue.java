package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.util.ExceptionUtils;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Validation issue item.
 * Stores what failed and where during validation process
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class ValidationIssue {
    private String fileName;
    private String invoiceNumber;
    private String sellerTaxId;
    private ValidationStage stage;
    private Severity severity;
    private String ruleKey;
    private String fieldPath;
    private String message;
}