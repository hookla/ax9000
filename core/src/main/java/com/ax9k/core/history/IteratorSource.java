package com.ax9k.core.history;

import com.ax9k.core.event.Event;
import com.ax9k.core.time.Time;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterator.SUBSIZED;

public class IteratorSource<T extends Event> implements Source<T> {
    public static final int UNINITIALISED = -999;

    private final List<T> history;
    private final OptionalInt intendedSize;
    private final int startingIndex;

    private T earliest;
    private T latest;
    private Duration duration;
    private int size = UNINITIALISED;

    IteratorSource(List<T> history, int startingIndex, Duration duration) {
        this(history, startingIndex);
        this.duration = duration;
    }

    IteratorSource(List<T> history, int startingIndex) {
        this.history = requireNonNull(history, "history");
        this.startingIndex = requirePositiveIndex(startingIndex);
        this.intendedSize = OptionalInt.empty();
    }

    private static int requirePositiveIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }
        return index;
    }

    IteratorSource(List<T> history, int startingIndex, int intendedSize) {
        this.history = requireNonNull(history, "history");
        this.startingIndex = requirePositiveIndex(startingIndex);
        this.intendedSize = OptionalInt.of(intendedSize);
    }

    @Override
    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliterator(iterator(), getSize(),
                                        SIZED | SUBSIZED | IMMUTABLE | ORDERED
        );
    }

    @Override
    public Iterator<T> iterator() {
        return new UnmodifiableIterator<>(history.listIterator(startingIndex));
    }

    @Override
    public int getSize() {
        if (size == UNINITIALISED) {
            size = history.size() - startingIndex;
        }
        return size;
    }

    @Override
    public OptionalInt getIntendedSize() {
        return intendedSize;
    }

    public boolean isEmpty() {
        return false;
    }

    @Override
    public int hashCode() {
        int result;
        result = Objects.hashCode(getEarliest());
        result = 31 * result + Objects.hashCode(getLatest());
        result = 31 * result + Objects.hashCode(getDuration());
        result = 31 * result + Integer.hashCode(getSize());
        return result;
    }

    @Override
    public Optional<T> getEarliest() {
        if (earliest == null) {
            earliest = history.get(startingIndex);
        }

        return Optional.of(earliest);
    }

    @Override
    public Optional<T> getLatest() {
        if (latest == null) {
            latest = history.get(history.size() - 1);
        }

        return Optional.of(latest);
    }

    @Override
    public Duration getDuration() {
        if (duration == null) {
            Instant earliestTimestamp = getEarliest().orElseThrow().getTimestamp();
            duration = Duration.between(earliestTimestamp, Time.now());
        }
        return duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        IteratorSource<?> that = (IteratorSource<?>) o;
        return getSize() == that.getSize() &&
               Objects.equals(getEarliest(), that.getEarliest()) &&
               Objects.equals(getLatest(), that.getLatest()) &&
               Objects.equals(getDuration(), that.getDuration());
    }

    private static final class UnmodifiableIterator<T extends Event> implements Iterator<T> {
        private final Iterator<T> backingIterator;

        private UnmodifiableIterator(Iterator<T> backingIterator) {
            this.backingIterator = backingIterator;
        }

        @Override
        public boolean hasNext() {
            return backingIterator.hasNext();
        }

        @Override
        public T next() {
            return backingIterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("cannot modify source data");
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            backingIterator.forEachRemaining(action);
        }
    }
}
