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
    private String invoiceNumber;
    private String sellerTaxId;
    private Severity severity; // "ERROR", "WARNING"
    private ValidationStage stage; // "TECHNICAL", "BUSINESS"
    private String ruleKey; // e.g. "TECH_MISSING_REQUIRED_FIELD"
    private String message; // human-readable message
    private String fieldPath; // e.g. "seller.taxId", "Fa.P_2"

    // ValidationService + KsefTechnicalValidation
    static public ValidationIssue buildIssue(
            String invoiceNumber,
            String sellerTaxId,
            ValidationStage stage,
            Severity severity,
            String ruleKey,
            String fieldPath,
            String message
    ) {
        ValidationIssue issue = new ValidationIssue();
        issue.setInvoiceNumber(invoiceNumber);
        issue.setSellerTaxId(sellerTaxId);
        issue.setStage(stage);
        issue.setSeverity(severity);
        issue.setRuleKey(ruleKey);
        issue.setFieldPath(fieldPath);
        issue.setMessage(message);
        return issue;
    }

    // 1) Для ValidationService: TECHNICAL
    public String serviceTechnicalMessage() {
        return String.format(
                "%s | %s | %s | Technical validation failed: field '%s' is missing/invalid or cannot be read. %s (rule: %s)",
                severity, sellerTaxId, invoiceNumber, fieldPath, details, ruleKey
        );
    }

    // 2) Для ValidationService: BUSINESS
    public String serviceBusinessMessage() {
        return String.format(
                "%s | %s | %s | Field '%s' violates business rule. %s (rule: %s)",
                severity, sellerTaxId, invoiceNumber, fieldPath, details, ruleKey
        );
    }

    // 3) Для KsefTechnicalValidator: missing required field
    public String ksefMissingRequiredFieldMessage() {
        return "ERROR | " + sellerTaxId + " | " + invoiceNumber
                + " | Technical validation failed: field '" + fieldPath + "' is missing or invalid.";
    }

    // 4) Для KsefTechnicalValidator: canonical mapping failed
    public String ksefCanonicalMappingFailedMessage() {
        return "ERROR | " + sellerTaxId + " | " + invoiceNumber
                + " | Technical validation failed: cannot create CanonicalInvoice. "
                + ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }
}