package com.vpvpteam.xmlinvoicevalidationbackend.validation.message;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;

public final class BusinessIssueMessages {
    private BusinessIssueMessages() {}

    public static String ruleViolation(String ruleKey,
                                       String fieldPath,
                                       RuleOperator operator,
                                       String expectedValue,
                                       String actualValue) {
        return "Business rule violation: " + ruleKey
                + ". Field '" + fieldPath + "'"
                + " expected " + operator + " '" + expectedValue + "'"
                + ", got '" + actualValue + "'";
    }
}