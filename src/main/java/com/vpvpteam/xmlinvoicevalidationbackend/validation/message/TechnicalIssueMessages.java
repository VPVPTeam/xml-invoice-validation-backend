package com.vpvpteam.xmlinvoicevalidationbackend.validation.message;

import com.vpvpteam.xmlinvoicevalidationbackend.util.ExceptionUtils;

public final class TechnicalIssueMessages {
    private TechnicalIssueMessages() {}

    public static String emptyBatch() {
        return "Batch has no XML files to validate.";
    }

    public static String xmlParseError(String fileName, Exception ex) {
        return "Cannot parse XML file '" + fileName + "': " + ExceptionUtils.safeMessage(ex);
    }

    public static String canonicalMappingFailed(String fileName, Exception ex) {
        return "Cannot build canonical invoice from file '" + fileName + "': " + ExceptionUtils.safeMessage(ex);
    }

    public static String missingRequiredField(String fieldPath) {
        return "Required field '" + fieldPath + "' is missing or invalid.";
    }
}