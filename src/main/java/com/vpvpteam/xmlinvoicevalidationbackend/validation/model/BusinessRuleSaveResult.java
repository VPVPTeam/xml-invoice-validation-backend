package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

// TODO: ДОБАВИТЬ В БУДУЩИХ ВЕРСИЯХ
public record BusinessRuleSaveResult(BusinessRule savedRule, BusinessRule replacedRule) {
    public boolean hadReplacement() {
        return replacedRule != null;
    }
}