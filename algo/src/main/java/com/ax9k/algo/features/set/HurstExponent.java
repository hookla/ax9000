package com.ax9k.algo.features.set;

import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.Parameters;
import com.ax9k.core.event.Event;
import com.ax9k.core.history.Source;

public final class HurstExponent implements SetFeature {
    private static final int DEFAULT_MAX_LAGS = 20;

    @Override
    public <T extends Event> double calculate(Feature<T> feature, Source<T> history, Parameters parameters) {
        int maxLags = parameters.getInt("maxLags").orElse(DEFAULT_MAX_LAGS);
        if (maxLags <= 5) {
            throw new IllegalArgumentException("maxLags <= 5");
        }

        //TODO implement
        return 0;
    }
}
