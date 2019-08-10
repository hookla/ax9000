package com.ax9k.algo.features.set;

import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.Parameters;
import com.ax9k.algo.features.store.Key;
import com.ax9k.algo.features.store.ResultStore;
import com.ax9k.algo.features.store.ResultStoreRegistry;
import com.ax9k.core.event.Event;
import com.ax9k.core.history.Source;

import java.util.function.Supplier;

import static com.ax9k.utils.math.MathUtils.round;

public final class ExponentialWeightedMovingAverage implements SetFeature {
    private static final Supplier<IllegalArgumentException> NO_INTENDED_PERIODS_ERROR = () ->
            new IllegalArgumentException("history must have intended size");

    private final ResultStoreRegistry storeRegistry = new ResultStoreRegistry();

    @Override
    public <T extends Event> double calculate(Feature<T> feature, Source<T> history, Parameters parameters) {
        if (history.isEmpty()) {
            return INVALID_RESULT;
        }

        int roundingPrecision = parameters.getInt("precision").orElse(3);
        int intendedPeriods = history.getIntendedSize().orElseThrow(NO_INTENDED_PERIODS_ERROR);

        Key parameterKey = Key.create(feature, history, parameters);
        ResultStore resultStore = storeRegistry.registerAndGet(parameterKey);

        double latestValue = history.getLatest().map(feature).orElseThrow();

        if (!resultStore.hasPreviousResult()) {
            resultStore.storeFeatureResult(latestValue);
            return history.getSize() >= intendedPeriods ? latestValue : INVALID_RESULT;
        }

        double previousResult = resultStore.getPreviousResult().orElseThrow();
        double multiplier = 2.0 / (intendedPeriods + 1);
        double ema = (latestValue - previousResult) * multiplier + previousResult;

        resultStore.storeFeatureResult(ema);

        return history.getSize() >= intendedPeriods ? round(ema, roundingPrecision) : INVALID_RESULT;
    }
}