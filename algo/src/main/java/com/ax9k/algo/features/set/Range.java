package com.ax9k.algo.features.set;

import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.Parameters;
import com.ax9k.core.event.Event;
import com.ax9k.core.history.Source;

import java.util.DoubleSummaryStatistics;

public class Range implements SetFeature {
    @Override
    public <T extends Event> double calculate(Feature<T> feature, Source<T> history, Parameters ignored) {
        if (history.isEmpty()) { return 0; }

        DoubleSummaryStatistics summary = history.stream()
                                                 .mapToDouble(feature)
                                                 .summaryStatistics();

        return summary.getMax() - summary.getMin();
    }
}
