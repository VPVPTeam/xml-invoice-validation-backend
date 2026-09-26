package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalFieldRegistry;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.InvoiceHeader;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.InvoiceTotals;
import com.vpvpteam.xmlinvoicevalidationbackend.canonical.Party;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessValidatorTest {

    private static final String FILE_NAME = "invoice.xml";
    private static final String SELLER_TAX_ID = "5211146938";
    private static final String INVOICE_NUMBER = "5860135336";

    private final BusinessValidator validator =
            new BusinessValidator(new FieldValueExtractor(), new OperatorEvaluator());

    @Test
    void validate_withNoRules_returnsNoIssues() {
        BusinessValidationOutput output = validator.validate(invoice(), FILE_NAME, List.of());

        assertThat(output.getIssues()).isEmpty();
    }

    @Test
    void validate_withPassingRule_returnsNoIssues() {
        BusinessRuleEntity rule = rule(
                "CURRENCY_MUST_BE_PLN", CanonicalFieldRegistry.TOTALS_CURRENCY_CODE, RuleOperator.EQUALS, "PLN");

        BusinessValidationOutput output = validator.validate(invoice(), FILE_NAME, List.of(rule));

        assertThat(output.getIssues()).isEmpty();
    }

    @Test
    void validate_withViolatedRule_returnsOneWarning() {
        BusinessRuleEntity rule = rule(
                "TOTALS_LESS_THAN", CanonicalFieldRegistry.TOTALS_TOTAL_GROSS, RuleOperator.LESS_THAN, "1000");

        BusinessValidationOutput output = validator.validate(invoice(), FILE_NAME, List.of(rule));

        assertThat(output.getIssues()).hasSize(1);

        ValidationIssue issue = output.getIssues().get(0);
        assertThat(issue.getRuleKey()).isEqualTo("TOTALS_LESS_THAN");
        assertThat(issue.getFieldPath()).isEqualTo(CanonicalFieldRegistry.TOTALS_TOTAL_GROSS);
        assertThat(issue.getStage()).isEqualTo(ValidationStage.BUSINESS);
        assertThat(issue.getSeverity()).isEqualTo(Severity.WARNING);
        assertThat(issue.getFileName()).isEqualTo(FILE_NAME);
        assertThat(issue.getSellerTaxId()).isEqualTo(SELLER_TAX_ID);
        assertThat(issue.getInvoiceNumber()).isEqualTo(INVOICE_NUMBER);
        assertThat(issue.getMessage()).contains("3201.10");
    }

    @Test
    void validate_withUnsupportedFieldPath_returnsWarning() {
        BusinessRuleEntity rule = rule(
                "UNKNOWN_FIELD_RULE", "header.unknownField", RuleOperator.EQUALS, "whatever");

        BusinessValidationOutput output = validator.validate(invoice(), FILE_NAME, List.of(rule));

        assertThat(output.getIssues()).hasSize(1);

        ValidationIssue issue = output.getIssues().get(0);
        assertThat(issue.getRuleKey()).isEqualTo("UNKNOWN_FIELD_RULE");
        assertThat(issue.getFieldPath()).isEqualTo("header.unknownField");
        assertThat(issue.getStage()).isEqualTo(ValidationStage.BUSINESS);
        assertThat(issue.getSeverity()).isEqualTo(Severity.WARNING);
        assertThat(issue.getMessage()).contains("header.unknownField");
    }

    @Test
    void validate_withMixedRules_reportsOnlyViolatedOne() {
        List<BusinessRuleEntity> rules = List.of(
                rule("CURRENCY_MUST_BE_PLN", CanonicalFieldRegistry.TOTALS_CURRENCY_CODE, RuleOperator.EQUALS, "PLN"),
                rule("TOTALS_LESS_THAN", CanonicalFieldRegistry.TOTALS_TOTAL_GROSS, RuleOperator.LESS_THAN, "1000")
        );

        BusinessValidationOutput output = validator.validate(invoice(), FILE_NAME, rules);

        assertThat(output.getIssues())
                .hasSize(1)
                .extracting(ValidationIssue::getRuleKey)
                .containsExactly("TOTALS_LESS_THAN");
    }

    private static BusinessRuleEntity rule(String ruleKey,
                                           String fieldPath,
                                           RuleOperator operator,
                                           String expectedValue) {
        BusinessRuleEntity rule = new BusinessRuleEntity();
        rule.setRuleKey(ruleKey);
        rule.setFieldPath(fieldPath);
        rule.setOperator(operator);
        rule.setExpectedValue(expectedValue);

        return rule;
    }

    private static CanonicalInvoice invoice() {
        Party seller = new Party();
        seller.setTaxId(SELLER_TAX_ID);

        InvoiceHeader header = new InvoiceHeader();
        header.setInvoiceNumber(INVOICE_NUMBER);
        header.setSeller(seller);

        InvoiceTotals totals = new InvoiceTotals();
        totals.setCurrencyCode("PLN");
        totals.setTotalGross(new BigDecimal("3201.10"));

        CanonicalInvoice invoice = new CanonicalInvoice();
        invoice.setHeader(header);
        invoice.setTotals(totals);

        return invoice;
    }
}