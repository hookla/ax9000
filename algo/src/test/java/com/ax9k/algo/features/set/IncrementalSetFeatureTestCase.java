package com.ax9k.algo.features.set;

import com.ax9k.core.history.Source;
import org.opentest4j.AssertionFailedError;

import java.util.OptionalInt;
import java.util.function.Predicate;

import static java.lang.String.format;

public class IncrementalSetFeatureTestCase implements SetFeatureTestCase {
    private final Source<TestDatum> singleFeatureResults;
    private final Predicate<Double> predicate;

    IncrementalSetFeatureTestCase(double[] singleFeatureResults,
                                  OptionalInt mockIntendedSize,
                                  Predicate<Double> predicate) {
        this.singleFeatureResults = new IncrementingListSource<>(TestDatum.wrapAll(singleFeatureResults),
                                                                 mockIntendedSize);
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
        double latest = setFeature.calculate(TestDatum.EXTRACT_VALUE, singleFeatureResults);
        for (int i = 1; i < singleFeatureResults.getSize(); i++) {
            latest = setFeature.calculate(TestDatum.EXTRACT_VALUE, singleFeatureResults);
        }
        return latest;
    }
}