package com.vpvpteam.xmlinvoicevalidationbackend.validation.validator.business;

import com.vpvpteam.xmlinvoicevalidationbackend.validation.enums.RuleOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorEvaluatorTest {

    private final OperatorEvaluator evaluator = new OperatorEvaluator();

    @ParameterizedTest
    @EnumSource(RuleOperator.class)
    void evaluate_withNullActualValue_returnsFalse(RuleOperator operator) {
        assertThat(evaluator.evaluate(null, "PLN", operator)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(RuleOperator.class)
    void evaluate_withNullExpectedValue_returnsFalse(RuleOperator operator) {
        assertThat(evaluator.evaluate("PLN", null, operator)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(RuleOperator.class)
    void isExpectedValueValid_withNullExpectedValue_returnsFalse(RuleOperator operator) {
        assertThat(evaluator.isExpectedValueValid(operator, null)).isFalse();
    }

    // EQUALS / NOT_EQUALS

    @ParameterizedTest
    @CsvSource({
            "PLN, PLN, true",
            "pln, PLN, true",
            "PLN, EUR, false"
    })
    void evaluate_equals_comparesIgnoringCase(String actualValue, String expectedValue, boolean expected) {
        assertThat(evaluator.evaluate(actualValue, expectedValue, RuleOperator.EQUALS)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "PLN, PLN, false",
            "pln, PLN, false",
            "PLN, EUR, true"
    })
    void evaluate_notEquals_comparesIgnoringCase(String actualValue, String expectedValue, boolean expected) {
        assertThat(evaluator.evaluate(actualValue, expectedValue, RuleOperator.NOT_EQUALS)).isEqualTo(expected);
    }

    // GREATER_THAN / LESS_THAN

    @ParameterizedTest
    @CsvSource({
            "1000.01, 1000, true",
            "1000, 1000, false",
            "999.99, 1000, false"
    })
    void evaluate_greaterThan_comparesNumerically(String actualValue, String expectedValue, boolean expected) {
        assertThat(evaluator.evaluate(actualValue, expectedValue, RuleOperator.GREATER_THAN)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "999.99, 1000, true",
            "1000, 1000, false",
            "1000.01, 1000, false"
    })
    void evaluate_lessThan_comparesNumerically(String actualValue, String expectedValue, boolean expected) {
        assertThat(evaluator.evaluate(actualValue, expectedValue, RuleOperator.LESS_THAN)).isEqualTo(expected);
    }

    @Test
    void evaluate_greaterThan_comparesAsNumbersNotAsText() {
        assertThat(evaluator.evaluate("9", "10", RuleOperator.GREATER_THAN)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "abc, 1000",
            "1000, abc"
    })
    void evaluate_greaterThan_withNonNumericValue_returnsFalse(String actualValue, String expectedValue) {
        assertThat(evaluator.evaluate(actualValue, expectedValue, RuleOperator.GREATER_THAN)).isFalse();
    }

    // BETWEEN

    @ParameterizedTest
    @CsvSource({
            "10, true",
            "55.5, true",
            "100, true",
            "9.99, false",
            "100.01, false"
    })
    void evaluate_between_includesBothBoundaries(String actualValue, boolean expected) {
        assertThat(evaluator.evaluate(actualValue, "10;100", RuleOperator.BETWEEN)).isEqualTo(expected);
    }

    @Test
    void evaluate_between_ignoresSpacesAroundDelimiter() {
        assertThat(evaluator.evaluate("50", " 10 ; 100 ", RuleOperator.BETWEEN)).isTrue();
    }

    @Test
    void evaluate_between_withReversedRange_returnsFalse() {
        assertThat(evaluator.evaluate("50", "100;10", RuleOperator.BETWEEN)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"10", "10;100;200", "10,100", "abc;100"})
    void evaluate_between_withMalformedRange_returnsFalse(String expectedValue) {
        assertThat(evaluator.evaluate("50", expectedValue, RuleOperator.BETWEEN)).isFalse();
    }

    // IN / NOT_IN

    @ParameterizedTest
    @CsvSource({
            "PLN, true",
            "pln, true",
            "USD, false"
    })
    void evaluate_in_matchesAnyListedValueIgnoringCase(String actualValue, boolean expected) {
        assertThat(evaluator.evaluate(actualValue, "PLN, EUR, GBP", RuleOperator.IN)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "PLN, false",
            "pln, false",
            "USD, true"
    })
    void evaluate_notIn_matchesNoListedValueIgnoringCase(String actualValue, boolean expected) {
        assertThat(evaluator.evaluate(actualValue, "PLN, EUR, GBP", RuleOperator.NOT_IN)).isEqualTo(expected);
    }

    // CONTAINS

    @ParameterizedTest
    @CsvSource({
            "Vendor Alpha, Alpha, true",
            "Vendor Alpha, alpha, true",
            "Vendor Alpha, Bravo, false"
    })
    void evaluate_contains_checksSubstringIgnoringCase(String actualValue, String expectedValue, boolean expected) {
        assertThat(evaluator.evaluate(actualValue, expectedValue, RuleOperator.CONTAINS)).isEqualTo(expected);
    }

    // isExpectedValueValid

    @ParameterizedTest
    @EnumSource(value = RuleOperator.class, names = {"GREATER_THAN", "LESS_THAN"})
    void isExpectedValueValid_forNumericOperators_requiresNumber(RuleOperator operator) {
        assertThat(evaluator.isExpectedValueValid(operator, "1000.50")).isTrue();
        assertThat(evaluator.isExpectedValueValid(operator, "abc")).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "10;100, true",
            "10 ; 100, true",
            "100;10, false",
            "10;10, true",
            "10, false",
            "10;100;200, false",
            "abc;100, false"
    })
    void isExpectedValueValid_forBetween_requiresTwoNumbersInOrder(String expectedValue, boolean expected) {
        assertThat(evaluator.isExpectedValueValid(RuleOperator.BETWEEN, expectedValue)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(value = RuleOperator.class, names = {"EQUALS", "NOT_EQUALS", "IN", "NOT_IN", "CONTAINS"})
    void isExpectedValueValid_forTextOperators_acceptsAnyValue(RuleOperator operator) {
        assertThat(evaluator.isExpectedValueValid(operator, "anything")).isTrue();
    }
}