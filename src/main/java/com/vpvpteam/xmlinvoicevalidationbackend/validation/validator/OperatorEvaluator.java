package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class OperatorEvaluator {

    /**
     * Проверяет, проходит ли actualValue проверку по оператору.
     * Возвращает true = правило соблюдено, false = нарушение.
     */
    public boolean evaluate(RuleOperator operator, String actualValue, String expectedValue) {

        // Если фактическое значение null — проверка провалена для любого оператора.
        // Нельзя сравнить "ничего" с ожидаемым значением.
        if (actualValue == null) {
            return false;
        }

        return switch (operator) {
            case EQUALS      -> actualValue.equals(expectedValue);
            case NOT_EQUALS   -> !actualValue.equals(expectedValue);
            case GREATER_THAN -> compareNumbers(actualValue, expectedValue) > 0;
            case LESS_THAN    -> compareNumbers(actualValue, expectedValue) < 0;
            case BETWEEN      -> evaluateBetween(actualValue, expectedValue);
            case IN           -> parseList(expectedValue).contains(actualValue);
            case NOT_IN       -> !parseList(expectedValue).contains(actualValue);
            case CONTAINS     -> actualValue.contains(expectedValue);
        };
    }

    /**
     * Сравнивает два значения как числа (BigDecimal).
     * BigDecimal потому что фактуры содержат суммы с копейками (100.50, 9999.99).
     * compareTo возвращает: -1 (меньше), 0 (равно), 1 (больше).
     */
    private int compareNumbers(String actual, String expected) {
        BigDecimal actualNum = new BigDecimal(actual);
        BigDecimal expectedNum = new BigDecimal(expected);
        return actualNum.compareTo(expectedNum);
    }

    /**
     * BETWEEN: expected_value хранится как "min|max" (например "0|10000").
     * Проверяет: min <= actual <= max.
     */
    private boolean evaluateBetween(String actualValue, String expectedValue) {
        // Разделяем по "|" — получаем [min, max]
        String[] parts = expectedValue.split("\\|");

        // Если формат неправильный — правило провалено
        if (parts.length != 2) {
            return false;
        }

        BigDecimal actual = new BigDecimal(actualValue);
        BigDecimal min = new BigDecimal(parts[0].trim());
        BigDecimal max = new BigDecimal(parts[1].trim());

        // actual >= min AND actual <= max
        return actual.compareTo(min) >= 0 && actual.compareTo(max) <= 0;
    }

    /**
     * IN / NOT_IN: expected_value хранится как "PLN,EUR,USD" (через запятую).
     * Разбиваем в список и trim каждый элемент.
     */
    private List<String> parseList(String expectedValue) {
        return Arrays.stream(expectedValue.split(","))
                .map(String::trim)
                .toList();
    }
}