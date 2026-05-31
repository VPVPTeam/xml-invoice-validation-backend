package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class BusinessRule {
    private Long id;
    private String vendorTaxId;
    private String ruleKey;
    private String fieldPath;
    private RuleOperator operator;
    private String expectedValue;
    private String createdBy;
}