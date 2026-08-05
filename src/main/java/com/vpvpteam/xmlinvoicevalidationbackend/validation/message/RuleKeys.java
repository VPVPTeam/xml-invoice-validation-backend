package com.vpvpteam.xmlinvoicevalidationbackend.validation.message;

/**
 * Rule keys produced by the system itself.
 * Business rule keys are defined by users and stored in the database, so they are not listed here.
 */
public final class RuleKeys {
    public static final String TECH_XML_PARSE_ERROR = "TECH_XML_PARSE_ERROR";
    public static final String TECH_MISSING_REQUIRED_FIELD = "TECH_MISSING_REQUIRED_FIELD";
    public static final String TECH_CANONICAL_MAPPING_FAILED = "TECH_CANONICAL_MAPPING_FAILED";

    public static final String DUPLICATE_IN_BATCH = "DUPLICATE_IN_BATCH";
    public static final String DUPLICATE_CROSS_BATCH = "DUPLICATE_CROSS_BATCH";

    private RuleKeys() {}
}