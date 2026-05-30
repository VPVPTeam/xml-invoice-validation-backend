package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator;

import com.vpvpteam.xmlinvoicevalidationbackend.canonical.CanonicalInvoice;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.entity.BusinessRuleEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.Severity;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.ValidationStage;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.model.ValidationIssue;
import com.vpvpteam.xmlinvoicevalidationbackend.validation.repository.BusinessRuleRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class BusinessValidator {

    private final BusinessRuleRepository businessRuleRepository;
    private final FieldValueExtractor fieldValueExtractor;
    private final OperatorEvaluator operatorEvaluator;

    /**
     * Запускает бизнес-валидацию фактуры по правилам вендора.
     * Возвращает список нарушений. Пустой список = все правила соблюдены.
     */
    public List<ValidationIssue> validate(CanonicalInvoice invoice, String fileName) {

        List<ValidationIssue> issues = new ArrayList<>();

        // 1) Достаём sellerTaxId из фактуры — по нему ищем правила вендора.
        String sellerTaxId = invoice.getHeader().getSeller().getTaxId();
        String invoiceNumber = invoice.getHeader().getInvoiceNumber();

        // 2) Загружаем все бизнес-правила для этого вендора из БД.
        //    Если у вендора нет правил — возвращаем пустой список (нет нарушений).
        List<BusinessRuleEntity> rules = businessRuleRepository.findByVendor_TaxId(sellerTaxId);

        if (rules.isEmpty()) {
            return issues;
        }

        // 3) Проверяем каждое правило.
        for (BusinessRuleEntity rule : rules) {

            // 3.1) Извлекаем фактическое значение поля из фактуры по field_path.
            //      Например: "totals.currencyCode" → "EUR"
            String actualValue = fieldValueExtractor.extract(invoice, rule.getFieldPath());

            // 3.2) Сравниваем фактическое значение с ожидаемым по оператору.
            //      Например: "EUR" EQUALS "PLN" → false (нарушение)
            boolean passed = operatorEvaluator.evaluate(
                    rule.getOperator(),
                    actualValue,
                    rule.getExpectedValue()
            );

            // 3.3) Если проверка не прошла — создаём ValidationIssue.
            if (!passed) {
                issues.add(ValidationIssue.buildIssue(
                        fileName,
                        invoiceNumber,
                        sellerTaxId,
                        ValidationStage.BUSINESS,
                        Severity.ERROR,
                        rule.getRuleKey(),
                        rule.getFieldPath(),
                        buildMessage(rule, actualValue)
                ));
            }
        }

        return issues;
    }

    /**
     * Формирует человекочитаемое сообщение об ошибке.
     */
    //
    private String buildMessage(BusinessRuleEntity rule, String actualValue) {
        return "Business rule violation: " + rule.getRuleKey()
                + ". Field '" + rule.getFieldPath() + "'"
                + " expected " + rule.getOperator() + " '" + rule.getExpectedValue() + "'"
                + ", got '" + actualValue + "'";
    }
}