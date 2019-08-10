package com.ax9k.core.history;

import com.ax9k.core.event.Event;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface History<T extends Event> extends Iterable<T> {
    void record(T datum);

    boolean isEmpty();

    Optional<T> getLatest();

    Optional<T> getPrevious();

    Collection<T> asCollection();

    Source<T> asSource();

    Source<T> asSource(Duration relevantPeriod);

    Source<T> asSource(int numberOfEntries);

    Source<T> asSource(Duration relevantPeriod, Predicate<T> filter);

    Source<T> asSource(int numberOfEntries, Predicate<T> filter);

    Stream<T> stream();

    int getSize();
}
