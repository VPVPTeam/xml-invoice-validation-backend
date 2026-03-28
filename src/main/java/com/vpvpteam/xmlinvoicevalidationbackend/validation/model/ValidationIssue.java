package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationIssue {
    private String invoiceId; // sellerTaxId + "|" + invoiceNumber
    private Severity severity; // "ERROR", "WARNING"
    private ValidationStage stage; // "TECHNICAL", "BUSINESS"
    private String ruleKey; // e.g. "TECH_MISSING_REQUIRED_FIELD"
    private String message; // human-readable message
    private String fieldPath; // e.g. "seller.taxId", "Fa.P_2"
}