package com.ax9k.broker;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public final class OrderRecord {
    private final Instant timestamp;
    private final int id;

    public OrderRecord(Instant timestamp, int id) {
        this.timestamp = requireNonNull(timestamp, "timestamp");
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getId() {
        return id;
    }
}
