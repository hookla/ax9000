package com.ax9k.core.history;

import com.ax9k.core.event.Event;

import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

class EmptySource<T extends Event> implements Source<T> {
    private final Duration period;
    private final OptionalInt intendedSize;

    EmptySource(Duration intendedPeriod) {
        period = requireNonNull(intendedPeriod, "EmptySource intendedPeriod");
        intendedSize = OptionalInt.empty();
    }

    EmptySource(int intendedSize) {
        period = Duration.ZERO;
        this.intendedSize = OptionalInt.of(intendedSize);
    }

    EmptySource() {
        period = Duration.ZERO;
        intendedSize = OptionalInt.empty();
    }

    @Override
    public Iterator<T> iterator() {
        return new EmptyIterator<>();
    }

    @Override
    public void forEach(Consumer<? super T> action) {}

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.emptySpliterator();
    }

    @Override
    public Stream<T> stream() {
        return Stream.empty();
    }

    @Override
    public Optional<T> getEarliest() {
        return Optional.empty();
    }

    @Override
    public Optional<T> getLatest() {
        return Optional.empty();
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public OptionalInt getIntendedSize() {
        return intendedSize;
    }

    @Override
    public Duration getDuration() {
        return period;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int hashCode() {
        return 31 * period.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        EmptySource<?> that = (EmptySource<?>) other;
        return period.equals(that.period);
    }
}
