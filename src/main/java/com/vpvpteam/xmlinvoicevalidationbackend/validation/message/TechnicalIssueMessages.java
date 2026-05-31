package com.vpvpteam.xmlinvoicevalidationbackend.validation.message;

import com.vpvpteam.xmlinvoicevalidationbackend.util.ExceptionUtils;

public final class TechnicalIssueMessages {

    private TechnicalIssueMessages() {}

    public static String emptyBatch() {
        return "ERROR\n" +
                "Technical validation failed: batch is missing/invalid or cannot be read.\n" +
                "Batch has no XML files to validate (rule: TECH_EMPTY_BATCH)";
    }

    public static String xmlParseError(String fileName, Exception ex) {
        return "ERROR\n" +
                "File name: " + fileName +
                "\nTechnical validation failed: field 'xml' is missing/invalid or cannot be read.\n" +
                "Cannot parse XML:\n" + ExceptionUtils.safeMessage(ex) + "\nrule: TECH_XML_PARSE_ERROR";
    }

    public static String canonicalMappingFailed(String sellerTaxId,
                                                String invoiceNumber,
                                                String fileName,
                                                Exception ex) {
        return "ERROR\n" +
                "File name: " + fileName +
                "\nSeller Tax ID: " + sellerTaxId + "; Invoice Number: " + invoiceNumber +
                "\nTechnical validation failed: cannot create CanonicalInvoice.\n" + ExceptionUtils.safeMessage(ex);
    }

    public static String missingRequiredField(String sellerTaxId,
                                              String invoiceNumber,
                                              String fieldPath) {
        return "ERROR\n" +
                "Seller Tax ID: " + sellerTaxId + "; Invoice Number: " + invoiceNumber +
                "\nTechnical validation failed: field '" + fieldPath + "' is missing or invalid.";
    }
}