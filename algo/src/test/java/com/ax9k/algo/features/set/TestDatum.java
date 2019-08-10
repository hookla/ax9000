package com.ax9k.algo.features.set;

import com.ax9k.algo.features.Feature;
import com.ax9k.core.event.Event;
import com.ax9k.core.event.EventType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.DoubleStream;

import static java.util.stream.Collectors.toList;

public final class TestDatum implements Event {
    static final Feature<TestDatum> EXTRACT_VALUE = datum -> datum.value;

    private final double value;

    static List<TestDatum> wrapAll(double... values) {
        return DoubleStream.of(values)
                           .mapToObj(TestDatum::new)
                           .collect(toList());
    }

    private TestDatum(double value) {
        this.value = value;
    }

    @Override
    public LocalDateTime getLocalisedTimestamp() {
        return LocalDateTime.ofInstant(getTimestamp(), ZoneOffset.UTC);
    }

    @Override
    public Instant getTimestamp() {
        return Instant.EPOCH;
    }

    @Override
    public EventType getType() {
        return EventType.UNKNOWN;
    }
}
