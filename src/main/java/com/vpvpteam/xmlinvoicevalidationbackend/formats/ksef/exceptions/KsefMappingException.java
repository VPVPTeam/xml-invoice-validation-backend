package com.vpvpteam.xmlinvoicevalidationbackend.formats.ksef.exceptions;

/**
 * Runtime exception for KSeF mapping problems.
 * We keep ruleKey + fieldPath inside the exception so validators/services
 * can build consistent validation issues later.
 */
public class KsefMappingException extends RuntimeException {
    private final String ruleKey;
    private final String fieldPath;

    public KsefMappingException(String ruleKey, String fieldPath, String message) {
        super(message);
        this.ruleKey = ruleKey;
        this.fieldPath = fieldPath;
    }

    /**
     * Factory method for "required field is missing" cases.
     */
    public static KsefMappingException missingRequired(String fieldPath) {
        return new KsefMappingException(
                "TECH_MISSING_REQUIRED_FIELD",
                fieldPath,
                "Missing required field: " + fieldPath
        );
    }

    /*
     * Factory method for "field exists, but value is wrong" cases.
     */
    public static KsefMappingException invalidValue(String fieldPath, String details) {
        return new KsefMappingException(
                "TECH_INVALID_FIELD_VALUE",
                fieldPath,
                "Invalid value at '" + fieldPath + "': " + details
        );
    }
}