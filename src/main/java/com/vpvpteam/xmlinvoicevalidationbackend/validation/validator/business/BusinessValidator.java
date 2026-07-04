package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.UnsupportedFieldPathException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.BusinessIssueMessages;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.BusinessRuleRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public final class BusinessValidator {
    private final BusinessRuleRepository businessRuleRepository;
    private final FieldValueExtractor fieldValueExtractor;
    private final OperatorEvaluator operatorEvaluator;

    public BusinessValidationOutput validate(CanonicalInvoice invoice, String fileName) {
        List<ValidationIssue> issues = new ArrayList<>();

        String sellerTaxId = invoice.getHeader().getSeller().getTaxId();
        String invoiceNumber = invoice.getHeader().getInvoiceNumber();

        List<BusinessRuleEntity> rules = businessRuleRepository.findByVendor_TaxId(sellerTaxId);

        if (rules.isEmpty()) {
            return BusinessValidationOutput.of(issues);
        }

        for (BusinessRuleEntity rule : rules) {
            String actualValue;

            try {
                actualValue = fieldValueExtractor.extract(invoice, rule.getFieldPath());
            } catch (UnsupportedFieldPathException e) {
                issues.add(new ValidationIssue(
                        fileName,
                        invoiceNumber,
                        sellerTaxId,
                        ValidationStage.BUSINESS,
                        Severity.WARNING,
                        rule.getRuleKey(),
                        rule.getFieldPath(),
                        BusinessIssueMessages.unsupportedFieldPath(rule.getRuleKey(), rule.getFieldPath())
                ));
                continue;
            }

            boolean passed = operatorEvaluator.evaluate(
                    actualValue,
                    rule.getExpectedValue(),
                    rule.getOperator()
            );

            if (!passed) {
                String message = BusinessIssueMessages.ruleViolation(
                        rule.getRuleKey(),
                        rule.getFieldPath(),
                        rule.getOperator(),
                        rule.getExpectedValue(),
                        actualValue
                );

                issues.add(new ValidationIssue(
                        fileName,
                        invoiceNumber,
                        sellerTaxId,
                        ValidationStage.BUSINESS,
                        Severity.WARNING,
                        rule.getRuleKey(),
                        rule.getFieldPath(),
                        message
                ));
            }
        }

        return BusinessValidationOutput.of(issues);
    }
}