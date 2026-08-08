package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.InvoiceHeader;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.UnsupportedFieldPathException;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.message.BusinessIssueMessages;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.InvoiceId;
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
        InvoiceHeader header = invoice.getHeader();
        InvoiceId invoiceId = new InvoiceId(header.getSeller().getTaxId(), header.getInvoiceNumber());

        List<BusinessRuleEntity> rules = businessRuleRepository.findByVendor_TaxId(invoiceId.sellerTaxId());
        List<ValidationIssue> issues = new ArrayList<>();

        for (BusinessRuleEntity rule : rules) {
            String actualValue;

            try {
                actualValue = fieldValueExtractor.extract(invoice, rule.getFieldPath());
            } catch (UnsupportedFieldPathException ex) {
                issues.add(ValidationIssue.businessWarning(
                        fileName,
                        invoiceId,
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
                issues.add(ValidationIssue.businessWarning(
                        fileName,
                        invoiceId,
                        rule.getRuleKey(),
                        rule.getFieldPath(),
                        BusinessIssueMessages.ruleViolation(
                                rule.getRuleKey(),
                                rule.getFieldPath(),
                                rule.getOperator(),
                                rule.getExpectedValue(),
                                actualValue
                        )
                ));
            }
        }

        return BusinessValidationOutput.of(issues);
    }
}