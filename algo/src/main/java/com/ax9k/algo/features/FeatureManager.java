package com.ax9k.algo.features;

import com.ax9k.algo.features.set.SetFeature;
import com.ax9k.core.event.Event;
import com.ax9k.core.history.History;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class FeatureManager<T extends Event> {
    private final Supplier<History<T>> historySupplier;

    public FeatureManager(Supplier<History<T>> historySupplier) {
        this.historySupplier = historySupplier;
    }

    @SuppressWarnings("OptionalIsPresent")
    public double get(Feature<T> feature) {
        History<T> history = historySupplier.get();
        Optional<T> latest = history.getLatest();
        if (latest.isPresent()) {
            return feature.calculate(latest.get());
        }
        return -1;
    }

    public double get(SetFeature setFeature,
                      Feature<T> feature,
                      Duration relevantPeriod) {
        History<T> history = historySupplier.get();
        return setFeature.calculate(feature, history.asSource(relevantPeriod));
    }

    public double get(SetFeature setFeature,
                      Feature<T> feature,
                      Duration relevantPeriod,
                      Predicate<T> filter) {
        History<T> history = historySupplier.get();
        return setFeature.calculate(feature, history.asSource(relevantPeriod, filter));
    }

    public double get(SetFeature setFeature,
                      Feature<T> feature,
                      int numberOfEntries) {
        History<T> history = historySupplier.get();
        return setFeature.calculate(feature, history.asSource(numberOfEntries));
    }

    public double get(SetFeature setFeature,
                      Feature<T> feature,
                      int numberOfEntries,
                      Parameters parameters) {
        History<T> history = historySupplier.get();
        return setFeature.calculate(feature, history.asSource(numberOfEntries), parameters);
    }

    public double get(SetFeature setFeature,
                      Feature<T> feature,
                      int numberOfEntries,
                      Predicate<T> filter) {
        History<T> history = historySupplier.get();
        return setFeature.calculate(feature, history.asSource(numberOfEntries, filter));
    }

    public double get(SetFeature setFeature,
                      Feature<T> feature,
                      int numberOfEntries,
                      Predicate<T> filter,
                      Parameters parameters) {
        History<T> history = historySupplier.get();
        return setFeature.calculate(feature, history.asSource(numberOfEntries, filter), parameters);
    }

    public double get(SetFeature setFeature,
                      Feature<T> feature) {
        History<T> history = historySupplier.get();
        return setFeature.calculate(feature, history.asSource());
    }

    public double get(SetFeature setFeature,
                      Feature<T> feature,
                      Parameters parameters) {
        History<T> history = historySupplier.get();
        return setFeature.calculate(feature, history.asSource(), parameters);
    }
}
