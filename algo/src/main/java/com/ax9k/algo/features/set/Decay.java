package com.ax9k.algo.features.set;

import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.Parameters;
import com.ax9k.core.event.Event;
import com.ax9k.core.history.Source;

public class Decay implements SetFeature {
    private static final double DEFAULT_DECAY_FACTOR = 0.25;

    @Override
    public <T extends Event> double calculate(Feature<T> feature,
                                              Source<T> history,
                                              Parameters parameters) {
        double factor = parameters.getDouble("decayFactor").orElse(DEFAULT_DECAY_FACTOR);

        long lastTimestamp = 0;
        double lastResult = 0;
        for (T datum : history) {
            //TODO convert this to the proper time format
            long timestamp = datum.getTimestamp().toEpochMilli();
            if (lastTimestamp == 0) { lastTimestamp = timestamp; }

            long timeDifference = Math.subtractExact(timestamp, lastTimestamp);
            double featureResult = feature.calculate(datum);

            lastResult = featureResult * StrictMath.pow(factor, truncate(timeDifference));
            lastTimestamp = timestamp;
        }

        return lastResult;
    }

    private static int truncate(long value) {
        return Math.min((int) value, 1);
    }
}
