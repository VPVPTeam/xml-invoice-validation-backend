package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import lombok.Getter;

import java.util.List;

@Getter
public final class BusinessValidationOutput {
    private final List<ValidationIssue> issues;

    private BusinessValidationOutput(List<ValidationIssue> issues) {
        this.issues = issues;
    }

    public static BusinessValidationOutput of(List<ValidationIssue> issues) {
        return new BusinessValidationOutput(issues);
    }
}