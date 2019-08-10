package com.ax9k.algo.features;

import com.ax9k.core.event.Event;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;

@FunctionalInterface
public interface Feature<T extends Event> extends Function<T, Double>, ToDoubleFunction<T> {
    @Override
    default Double apply(T source) {
        return calculate(source);
    }

    double calculate(T source);

    @Override
    default double applyAsDouble(T source) {
        return calculate(source);
    }
}
