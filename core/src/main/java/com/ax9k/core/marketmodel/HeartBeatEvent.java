package com.ax9k.core.marketmodel;

import com.ax9k.core.event.Event;
import com.ax9k.core.event.EventType;

import java.time.Instant;

public final class HeartBeatEvent implements Event {
    private final Instant timestamp;

    HeartBeatEvent(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public EventType getType() {
        return EventType.HEARTBEAT;
    }
}
