package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public final class OperatorEvaluator {
    public boolean evaluate(String actualValue, String expectedValue, RuleOperator operator) {
        if (actualValue == null) {
            return false;
        }

        return switch (operator) {
            case EQUALS       -> actualValue.equals(expectedValue);
            case NOT_EQUALS   -> !actualValue.equals(expectedValue);
            case GREATER_THAN -> compareNumbers(actualValue, expectedValue) > 0;
            case LESS_THAN    -> compareNumbers(actualValue, expectedValue) < 0;
            case BETWEEN      -> evaluateBetween(actualValue, expectedValue);
            case IN           -> parseList(expectedValue).contains(actualValue);
            case NOT_IN       -> !parseList(expectedValue).contains(actualValue);
            case CONTAINS     -> actualValue.contains(expectedValue);
        };
    }

    private int compareNumbers(String actual, String expected) {
        BigDecimal actualNum = new BigDecimal(actual);
        BigDecimal expectedNum = new BigDecimal(expected);
        return actualNum.compareTo(expectedNum);
    }

    // TODO: Подумать можно ли заменить делимитер
    private boolean evaluateBetween(String actualValue, String expectedValue) {
        String[] parts = expectedValue.split("\\|");

        if (parts.length != 2) {
            return false;
        }

        BigDecimal actual = new BigDecimal(actualValue);
        BigDecimal min = new BigDecimal(parts[0].strip());
        BigDecimal max = new BigDecimal(parts[1].strip());

        return actual.compareTo(min) >= 0 && actual.compareTo(max) <= 0;
    }

    private List<String> parseList(String expectedValue) {
        return Arrays.stream(expectedValue.split(","))
                .map(String::strip)
                .toList();
    }
}