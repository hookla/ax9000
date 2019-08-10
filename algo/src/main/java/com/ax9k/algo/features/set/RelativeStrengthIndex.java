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

public class RelativeStrengthIndex implements SetFeature {
    private static final Supplier<IllegalArgumentException> NO_INTENDED_PERIODS_ERROR = () ->
            new IllegalArgumentException("history must have intended size");

    private final ResultStoreRegistry storeRegistry = new ResultStoreRegistry();

    @Override
    public <T extends Event> double calculate(Feature<T> feature, Source<T> history, Parameters parameters) {
        int roundingPrecision = parameters.getInt("precision").orElse(3);

        /*
        TODO leaving this bit commented out keeps this feature functionally equivalent to the code
        taken from the intraday momentum algos. However, is it really desirable to have the feature
        calculate with little to none of the intended history?
        if (periods == 0 || history.getSize() == 0 || history.getSize() != periods) {
            return INVALID_RESULT;
        }
        */
        Key parameterKey = Key.create(feature, history, parameters);
        ResultStore resultStore = storeRegistry.registerAndGet(parameterKey);

        double latestValue = history.getLatest().map(feature).orElse(0d);
        double previousValue = resultStore.getPreviousResult().orElse(latestValue);

        double change = latestValue - previousValue;

        double gain = Math.max(change, 0);
        double loss = Math.max(-change, 0);

        double previousAverageGain = resultStore.get("averageGain").orElse(0d);
        double previousAverageLoss = resultStore.get("averageLoss").orElse(0d);

        int periods = history.getIntendedSize().orElseThrow(NO_INTENDED_PERIODS_ERROR);
        double averageGain = round((previousAverageGain * (periods - 1) + gain) / periods, roundingPrecision);
        double averageLoss = round((previousAverageLoss * (periods - 1) + loss) / periods, roundingPrecision);
        double relativeStrength = averageGain / Math.max(averageLoss, Double.MIN_VALUE);
        double rsi = round(100 - 100 / (1 + relativeStrength), roundingPrecision);

        resultStore.storeFeatureResult(latestValue);
        resultStore.store("averageGain", averageGain);
        resultStore.store("averageLoss", averageLoss);

        return rsi;
    }
}
