package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ValidationResult {
    private String status; // "OK", "ERROR"
    private boolean technicalValid;
    private boolean businessValid;
    private List<ValidationIssue> issues = new ArrayList<>();

    public ValidationResult(String status,
                            boolean technicalValid,
                            boolean businessValid,
                            List<ValidationIssue> issues) {
        this.status = status;
        this.technicalValid = technicalValid;
        this.businessValid = businessValid;
        this.issues = issues == null ? new ArrayList<>() : new ArrayList<>(issues);
    }
}