package com.ax9k.algo.features.set;

import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.Parameters;
import com.ax9k.core.event.Event;
import com.ax9k.core.history.Source;

public class Close implements SetFeature {
    @Override
    public <T extends Event> double calculate(Feature<T> feature, Source<T> history, Parameters ignored) {
        return history.getLatest().map(feature).orElse(0d);
    }
}
