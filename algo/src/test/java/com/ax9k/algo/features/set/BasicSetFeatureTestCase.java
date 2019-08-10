package com.ax9k.algo.features.set;

import com.ax9k.core.history.Source;
import org.opentest4j.AssertionFailedError;

import java.util.OptionalInt;
import java.util.function.Predicate;

import static java.lang.String.format;

final class BasicSetFeatureTestCase implements SetFeatureTestCase {
    private final Source<TestDatum> singleFeatureResults;
    private final Predicate<Double> predicate;

    BasicSetFeatureTestCase(double[] singleFeatureResults,
                            OptionalInt mockIntendedSize,
                            Predicate<Double> predicate) {
        this.singleFeatureResults = new ListSource<>(TestDatum.wrapAll(singleFeatureResults), mockIntendedSize);
        this.predicate = predicate;
    }

    @Override
    public void test(SetFeature setFeature) {
        double result = calculate(setFeature);
        if (!predicate.test(result)) {
            throw new AssertionFailedError(format("Predicate failed: %s. Actual result: %s",
                                                  predicate,
                                                  result));
        }
    }

    @Override
    public double calculate(SetFeature setFeature) {
        return setFeature.calculate(TestDatum.EXTRACT_VALUE, singleFeatureResults);
    }
}
