package com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.exceptions;

public class KsefMappingException extends RuntimeException {
    private final String ruleKey;
    private final String fieldPath;

    public KsefMappingException(String ruleKey, String fieldPath, String message) {
        super(message);
        this.ruleKey = ruleKey;
        this.fieldPath = fieldPath;
    }

    public static KsefMappingException missingRequired(String fieldPath) {
        return new KsefMappingException(
                "TECH_MISSING_REQUIRED_FIELD",
                fieldPath,
                "Missing required field: " + fieldPath
        );
    }

    public static KsefMappingException invalidValue(String fieldPath, String details) {
        return new KsefMappingException(
                "TECH_INVALID_FIELD_VALUE",
                fieldPath,
                "Invalid value at '" + fieldPath + "': " + details
        );
    }
}
