package com.ax9k.algo.features.set;

import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.Parameters;
import com.ax9k.core.event.Event;
import com.ax9k.core.history.Source;

public class Open implements SetFeature {
    @Override
    public <T extends Event> double calculate(Feature<T> feature, Source<T> history, Parameters ignored) {
        return history.getEarliest().map(feature).orElse(0d);
    }
}
