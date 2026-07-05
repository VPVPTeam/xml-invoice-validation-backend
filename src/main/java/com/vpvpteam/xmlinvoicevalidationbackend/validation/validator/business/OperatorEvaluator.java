package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public final class OperatorEvaluator {
    private static final String BETWEEN_DELIMITER = ";";

    public boolean evaluate(String actualValue, String expectedValue, RuleOperator operator) {
        if (actualValue == null) {
            return false;
        }

        return switch (operator) {
            case EQUALS       -> actualValue.equals(expectedValue);
            case NOT_EQUALS   -> !actualValue.equals(expectedValue);
            case GREATER_THAN -> compareNumbers(actualValue, expectedValue).map(cmp -> cmp > 0).orElse(false);
            case LESS_THAN    -> compareNumbers(actualValue, expectedValue).map(cmp -> cmp < 0).orElse(false);
            case BETWEEN      -> evaluateBetween(actualValue, expectedValue);
            case IN           -> parseList(expectedValue).contains(actualValue);
            case NOT_IN       -> !parseList(expectedValue).contains(actualValue);
            case CONTAINS     -> actualValue.contains(expectedValue);
        };
    }

    public boolean isExpectedValueValid(RuleOperator operator, String expectedValue) {
        return switch (operator) {
            case GREATER_THAN, LESS_THAN -> tryParseBigDecimal(expectedValue).isPresent();
            case BETWEEN -> isValidBetweenRange(expectedValue);
            case EQUALS, NOT_EQUALS, IN, NOT_IN, CONTAINS -> true;
        };
    }

    private Optional<Integer> compareNumbers(String actual, String expected) {
        Optional<BigDecimal> actualNum = tryParseBigDecimal(actual);
        Optional<BigDecimal> expectedNum = tryParseBigDecimal(expected);

        if (actualNum.isEmpty() || expectedNum.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(actualNum.get().compareTo(expectedNum.get()));
    }

    private boolean evaluateBetween(String actualValue, String expectedValue) {
        Optional<NumericRange> range = parseBetweenRange(expectedValue);
        Optional<BigDecimal> actual = tryParseBigDecimal(actualValue);

        if (range.isEmpty() || actual.isEmpty()) {
            return false;
        }

        return actual.get().compareTo(range.get().min()) >= 0
                && actual.get().compareTo(range.get().max()) <= 0;
    }

    private Optional<NumericRange> parseBetweenRange(String expectedValue) {
        String[] parts = expectedValue.split(Pattern.quote(BETWEEN_DELIMITER), -1);

        if (parts.length != 2) {
            return Optional.empty();
        }

        Optional<BigDecimal> min = tryParseBigDecimal(parts[0].strip());
        Optional<BigDecimal> max = tryParseBigDecimal(parts[1].strip());

        if (min.isEmpty() || max.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new NumericRange(min.get(), max.get()));
    }

    private Optional<BigDecimal> tryParseBigDecimal(String value) {
        try {
            return Optional.of(new BigDecimal(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private List<String> parseList(String expectedValue) {
        return Arrays.stream(expectedValue.split(","))
                .map(String::strip)
                .toList();
    }

    private boolean isValidBetweenRange(String expectedValue) {
        return parseBetweenRange(expectedValue)
                .map(range -> range.min().compareTo(range.max()) <= 0)
                .orElse(false);
    }

    private record NumericRange(BigDecimal min, BigDecimal max) {}
}