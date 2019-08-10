package com.ax9k.algo.features.set;

import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.Parameters;
import com.ax9k.core.event.Event;
import com.ax9k.core.history.Source;

public interface SetFeature {
    double INVALID_RESULT = -1.0;

    default <T extends Event> double calculate(Feature<T> feature, Source<T> history) {
        return calculate(feature, history, Parameters.NONE);
    }

    <T extends Event> double calculate(Feature<T> feature, Source<T> history, Parameters parameters);
}