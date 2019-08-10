package com.ax9k.algo.features.set;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public interface SetFeatureSpecification {
    @Test
    default void shouldProduceCorrectResults() {
        SetFeatureTestCase[] testCases = testCases();
        for (var testCase : testCases) {
            testCase.test(setFeature());
        }
    }

    SetFeatureTestCase[] testCases();

    SetFeature setFeature();

    @Test
    default void shouldProduceConsistentResults() {
        SetFeatureTestCase[] testCases = testCases();
        for (var testCase : testCases) {
            assertProducesConsistentOutput(testCase);
        }
    }

    private void assertProducesConsistentOutput(SetFeatureTestCase testCase) {
        SetFeature setFeature = setFeature();

        double firstResult = testCase.calculate(setFeature);
        double secondResult = testCase.calculate(setFeature);

        assertEquals(firstResult, secondResult);
    }

    default SetFeatureTestCase testCase(double[] singleFeatureResults, double expectedOutput) {
        return new BasicSetFeatureTestCase(singleFeatureResults,
                                           OptionalInt.empty(),
                                           resultEqualTo(expectedOutput));
    }

    default Predicate<Double> resultEqualTo(double value) {
        return new Predicate<>() {
            @Override
            public boolean test(Double result) {
                return Double.compare(result, value) == 0;
            }

            @Override
            public String toString() {
                return "ResultEqualToPredicate[" + value + "]";
            }
        };
    }

    default SetFeatureTestCase incrementalTestCase(double[] singleFeatureResults,
                                                   int mockIntendedSize,
                                                   Predicate<Double> predicate) {
        return new IncrementalSetFeatureTestCase(singleFeatureResults,
                                                 OptionalInt.of(mockIntendedSize),
                                                 predicate);
    }

    default Predicate<Double> resultNotEqualTo(double value) {
        return new Predicate<>() {
            @Override
            public boolean test(Double result) {
                return Double.compare(result, value) != 0;
            }

            @Override
            public String toString() {
                return "ResultNotEqualToPredicate[" + value + "]";
            }
        };
    }

    default Predicate<Double> resultGreaterThan(double value) {
        return new Predicate<>() {
            @Override
            public boolean test(Double result) {
                return Double.compare(result, value) > 0;
            }

            @Override
            public String toString() {
                return "ResultGreaterThanPredicate[" + value + "]";
            }
        };
    }
}