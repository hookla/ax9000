package com.ax9k.algo.features.set;

import com.ax9k.core.event.Event;
import com.ax9k.core.history.Source;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

public class IncrementingListSource<T extends Event> implements Source<T> {
    private final List<T> delegate;
    private final OptionalInt intendedSize;

    private int index;

    IncrementingListSource(List<T> delegate, OptionalInt intendedSize) {
        this.delegate = List.copyOf(delegate);
        this.intendedSize = intendedSize;
    }

    @Override
    public Stream<T> stream() {
        return delegate.stream();
    }

    @Override
    public OptionalInt getIntendedSize() {
        return intendedSize;
    }

    @Override
    public int getSize() {
        return delegate.size();
    }

    @Override
    public Duration getDuration() {
        Optional<Instant> latestTimestamp = getLatest().map(Event::getTimestamp);
        if (!latestTimestamp.isPresent()) {
            return Duration.ZERO;
        }
        return getEarliest()
                .map(earliest ->
                             Duration.between(earliest.getTimestamp(), latestTimestamp.get()))
                .orElse(Duration.ZERO);
    }

    @Override
    public Optional<T> getEarliest() {
        if (isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(delegate.get(0));
    }

    @Override
    public Optional<T> getLatest() {
        if (isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(delegate.get(index++));
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }
}
