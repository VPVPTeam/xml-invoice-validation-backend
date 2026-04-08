package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.util.ExceptionUtils;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Validation issue item.
 * Stores what failed and where during validation process
 */
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

    /**
     * Factory method for creating ValidationIssue.
     */
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

    /**
     * Message for an empty batch input.
     */
    public static String messageTechEmptyBatch() {

        return "ERROR\n" +
                "Seller Tax ID: UNKNOWN; Invoice Number: UNKNOWN\n" +
                "Technical validation failed: batch is missing/invalid or cannot be read.\n" +
                "Batch has no XML files to validate (rule: TECH_EMPTY_BATCH)";
    }

    /**
     * Message for XML parse failure.
     */
    public static String messageTechXmlParseError(Exception ex) {

        return "ERROR\n" +
                "Seller Tax ID: UNKNOWN; Invoice Number: UNKNOWN\n" +
                "Technical validation failed: field 'xml' is missing/invalid or cannot be read.\n" +
                "Cannot parse XML:\n" + ExceptionUtils.safeMessage(ex) + "\nrule: TECH_XML_PARSE_ERROR";
    }

    /**
     * Message for KSeF when Canonical mapping failure occurred.
     */
    public static String messageKsefCanonicalMappingFailed(String sellerTaxId,
                                                           String invoiceNumber,
                                                           Exception ex) {

        return "ERROR\n" +
                "Seller Tax ID: " + sellerTaxId + "; Invoice Number: " + invoiceNumber +
                "\nTechnical validation failed: cannot create CanonicalInvoice.\n" + ExceptionUtils.safeMessage(ex);
    }

    /**
     * Message for duplicate invoice inside a batch.
     */
    public static String messageDuplicateInBatch(String sellerTaxId, String invoiceNumber) {
        return "WARNING\n" +
                "Seller Tax ID: " + sellerTaxId + "; Invoice Number: " + invoiceNumber +
                "\nDuplicate invoice in the same batch (rule: DUPLICATE_IN_BATCH)";
    }

    /**
     * Message for missing/invalid required KSeF field.
     */
    public static String messageKsefMissingRequiredField(String sellerTaxId,
                                                         String invoiceNumber,
                                                         String fieldPath) {
        return "ERROR\n" +
                "Seller Tax ID: " + sellerTaxId + "; Invoice Number: " + invoiceNumber +
                "\nTechnical validation failed: field '" + fieldPath + "' is missing or invalid.";
    }
}