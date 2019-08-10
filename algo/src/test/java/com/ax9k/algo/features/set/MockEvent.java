package com.ax9k.algo.features.set;

import com.ax9k.core.event.Event;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public final class MockEvent implements Event {
    private final Instant timestamp;

    private MockEvent(Instant timestamp) {
        this.timestamp = requireNonNull(timestamp);
    }

    public static Event wrapMillisSinceEpoch(int millis) {
        return wrap(Instant.EPOCH.plusMillis(millis));
    }

    public static Event wrap(Instant timestamp) {
        return new MockEvent(timestamp);
    }

    public static List<Event> wrapAll(Instant... timestamps) {
        return Arrays.stream(timestamps)
                     .map(MockEvent::wrap)
                     .collect(Collectors.toList());
    }

    public static List<Event> listWithMillisAfterEpoch(int... values) {
        return streamWithMillisAfterEpoch(values).collect(Collectors.toList());
    }

    public static Stream<Event> streamWithMillisAfterEpoch(int... values) {
        return Arrays.stream(values)
                     .mapToObj(Instant.EPOCH::plusMillis)
                     .map(MockEvent::wrap);
    }

    @Override
    public LocalDateTime getLocalisedTimestamp() {
        return LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC);
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public int hashCode() {
        return timestamp.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }

        MockEvent that = (MockEvent) other;
        return timestamp.equals(that.timestamp);
    }

    @Override
    public String toString() {
        return timestamp.toString();
    }
}
