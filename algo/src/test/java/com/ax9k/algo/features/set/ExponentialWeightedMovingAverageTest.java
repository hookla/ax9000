package com.ax9k.algo.features.set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ExponentialWeightedMovingAverageTest implements SetFeatureSpecification {
    @Override
    public SetFeatureTestCase[] testCases() {
        return new SetFeatureTestCase[] {
            incrementalTestCase(new double[] { 1, 2, 3, 4, }, 4, resultEqualTo(2.824)),
            incrementalTestCase(new double[] { 0, 0, 0, 0, }, 4, resultEqualTo(0)),
            incrementalTestCase(new double[] { 1, 2, 3 }, 4, resultEqualTo(-1)),
            incrementalTestCase(new double[] { 1, 2, 3 }, 3, resultNotEqualTo(-1)),
            incrementalTestCase(new double[] {}, 3, resultEqualTo(-1)),
        };
    }

    @Override
    public SetFeature setFeature() {
        return new ExponentialWeightedMovingAverage();
    }

    @Override
    @Disabled("Not applicable")
    public void shouldProduceConsistentResults() { }

    @Test
    void assertFailsWhenIntendedSizeNotGiven() {
        SetFeatureTestCase testCase = testCase(new double[] { 2, 3, 4, }, -1);
        assertThrows(IllegalArgumentException.class, () -> testCase.calculate(new ExponentialWeightedMovingAverage()));
    }
}