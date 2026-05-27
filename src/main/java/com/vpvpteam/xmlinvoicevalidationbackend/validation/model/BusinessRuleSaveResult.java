package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

public record BusinessRuleSaveResult(
        BusinessRule savedRule,
        BusinessRule replacedRule
) {
    public boolean hadReplacement() {
        return replacedRule != null;
    }
}