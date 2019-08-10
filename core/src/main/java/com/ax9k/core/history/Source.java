package com.ax9k.core.history;

import com.ax9k.core.event.Event;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

public interface Source<T extends Event> extends Iterable<T> {
    Stream<T> stream();

    Optional<T> getEarliest();

    Optional<T> getLatest();

    int getSize();

    OptionalInt getIntendedSize();

    Duration getDuration();

    boolean isEmpty();
}
