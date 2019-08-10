package com.ax9k.core.history;

import com.ax9k.core.event.Event;
import com.ax9k.core.time.Time;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterator.SUBSIZED;

public class FilteredIteratorSource<T extends Event> implements Source<T> {
    private static final int UNINITIALISED = -999;

    private final List<T> history;
    private final Predicate<T> filter;
    private final OptionalInt intendedSize;

    private T earliest;
    private T latest;
    private Duration duration;
    private int size = UNINITIALISED;
    private int startingIndex;

    private boolean empty;

    FilteredIteratorSource(List<T> history, int startingIndex, Duration duration, Predicate<T> filter) {
        this(history, startingIndex, filter);
        this.duration = duration;
    }

    FilteredIteratorSource(List<T> history, int startingIndex, Predicate<T> filter) {
        this.history = requireNonNull(history, "history");
        this.startingIndex = requirePositiveIndex(startingIndex);
        this.filter = requireNonNull(filter, "filter");
        intendedSize = OptionalInt.empty();
    }

    private static int requirePositiveIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }
        return index;
    }

    FilteredIteratorSource(List<T> history,
                           int startingIndex,
                           Predicate<T> filter,
                           int intendedSize) {
        this.history = requireNonNull(history, "history");
        this.startingIndex = requirePositiveIndex(startingIndex);
        this.filter = requireNonNull(filter, "filter");
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
        if (empty) {
            return new EmptyIterator<>();
        }

        return new UnmodifiableFilteredIterator<>(
                history.listIterator(startingIndex),
                filter
        );
    }

    @Override
    public int getSize() {
        if (size == UNINITIALISED) {
            size = calculateSize();
        }
        return size;
    }

    private int calculateSize() {
        int count = 0;
        for (int i = startingIndex; i < history.size(); i++) {
            T current = history.get(i);
            if (filter.test(current)) {
                count++;
            }
        }
        if (count == 0) {
            empty = true;
        }
        return count;
    }

    @Override
    public OptionalInt getIntendedSize() {
        return intendedSize;
    }

    public boolean isEmpty() {
        return empty;
    }

    @Override
    public int hashCode() {
        int result;
        result = Objects.hashCode(getEarliest());
        result = 31 * result + Objects.hashCode(getLatest());
        result = 31 * result + Objects.hashCode(getDuration());
        result = 31 * result + Integer.hashCode(getSize());
        result = 31 * result + Boolean.hashCode(empty);
        return result;
    }

    @Override
    public Optional<T> getEarliest() {
        if (!empty && earliest == null) {
            earliest = findEarliestValidValue();
        }

        return Optional.ofNullable(earliest);
    }

    private T findEarliestValidValue() {
        for (int i = startingIndex; i < history.size(); i++) {
            T current = history.get(i);
            if (filter.test(current)) {
                startingIndex = i;
                return current;
            }
        }
        empty = true;
        return null;
    }

    @Override
    public Optional<T> getLatest() {
        if (!empty && latest == null) {
            latest = findLatestValidValue();
        }

        return Optional.ofNullable(latest);
    }

    private T findLatestValidValue() {
        for (int i = history.size() - 1; i >= startingIndex; i--) {
            T current = history.get(i);
            if (filter.test(current)) {
                return current;
            }
        }
        empty = true;
        return null;
    }

    @Override
    public Duration getDuration() {
        if (duration == null) {
            T earliest = getEarliest().orElse(null);
            //noinspection ConstantConditions
            duration = empty ? Duration.ZERO :
                       Duration.between(earliest.getTimestamp(), Time.now());
        }
        return duration;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        FilteredIteratorSource<?> that = (FilteredIteratorSource<?>) other;
        return getSize() == that.getSize() &&
               empty == that.empty &&
               Objects.equals(getEarliest(), that.getEarliest()) &&
               Objects.equals(getLatest(), that.getLatest()) &&
               Objects.equals(getDuration(), that.getDuration());
    }

    private static final class UnmodifiableFilteredIterator<T extends Event> implements Iterator<T> {
        private final Iterator<T> backingIterator;
        private final Predicate<T> filter;

        private T current;

        private UnmodifiableFilteredIterator(Iterator<T> backingIterator, Predicate<T> filter) {
            this.backingIterator = backingIterator;
            this.filter = filter;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("cannot modify source data");
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("no more elements");
            }
            T result = current;
            current = null;
            return result;
        }

        @Override
        public boolean hasNext() {
            if (current != null) {
                return true;
            }
            current = lookAhead();
            return current != null;
        }

        private T lookAhead() {
            while (backingIterator.hasNext()) {
                T next = backingIterator.next();
                if (filter.test(next)) {
                    return next;
                }
            }
            return null;
        }
    }
}
