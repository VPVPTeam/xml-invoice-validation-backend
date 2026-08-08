package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Validation issue item.
 * Stores what failed and where during validation process.
 * Created only through the factory methods, which enforce the stage/severity pairing.
 */
@Getter
@Setter
@NoArgsConstructor
public final class ValidationIssue {
    private String fileName;
    private String invoiceNumber;
    private String sellerTaxId;
    private ValidationStage stage;
    private Severity severity;
    private String ruleKey;
    private String fieldPath;
    private String message;

    private ValidationIssue(String fileName,
                            InvoiceId invoiceId,
                            ValidationStage stage,
                            Severity severity,
                            String ruleKey,
                            String fieldPath,
                            String message) {
        this.fileName = fileName;
        this.sellerTaxId = invoiceId.sellerTaxId();
        this.invoiceNumber = invoiceId.invoiceNumber();
        this.stage = stage;
        this.severity = severity;
        this.ruleKey = ruleKey;
        this.fieldPath = fieldPath;
        this.message = message;
    }

    public static ValidationIssue technicalError(String fileName,
                                                 InvoiceId invoiceId,
                                                 String ruleKey,
                                                 String fieldPath,
                                                 String message) {
        return new ValidationIssue(fileName, invoiceId,
                ValidationStage.TECHNICAL, Severity.ERROR,
                ruleKey, fieldPath, message);
    }

    public static ValidationIssue businessWarning(String fileName,
                                                  InvoiceId invoiceId,
                                                  String ruleKey,
                                                  String fieldPath,
                                                  String message) {
        return new ValidationIssue(fileName, invoiceId,
                ValidationStage.BUSINESS, Severity.WARNING,
                ruleKey, fieldPath, message);
    }
}