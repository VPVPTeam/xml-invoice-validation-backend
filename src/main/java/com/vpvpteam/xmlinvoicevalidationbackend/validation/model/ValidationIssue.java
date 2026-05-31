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
    private String fileName; // original name of XML file
    private String invoiceNumber;
    private String sellerTaxId;
    private ValidationStage stage; // "TECHNICAL", "BUSINESS"
    private Severity severity; // "ERROR", "WARNING"
    private String ruleKey; // e.g. "TECH_MISSING_REQUIRED_FIELD"
    private String fieldPath; // e.g. "seller.taxId", "Fa.P_2"
    private String message; // human-readable message
}