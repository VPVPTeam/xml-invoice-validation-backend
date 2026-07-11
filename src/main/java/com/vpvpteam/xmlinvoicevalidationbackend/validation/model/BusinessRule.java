package com.vpvpteam.xmlinvoicevalidationbackend.validation.model;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Vendor tax ID must not be blank")
    @Size(max = 50)
    private String vendorTaxId;

    @NotBlank(message = "Rule key must not be blank")
    @Size(max = 100)
    private String ruleKey;

    @NotBlank(message = "Field path must not be blank")
    @Size(max = 100)
    private String fieldPath;

    @NotNull(message = "Operator must not be blank")
    private RuleOperator operator;

    @NotBlank(message = "Expected value must not be blank")
    @Size(max = 255)
    private String expectedValue;

    @Size(max = 100)
    private String createdBy;
}