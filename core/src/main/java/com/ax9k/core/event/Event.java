package com.ax9k.core.event;

import com.ax9k.core.time.Time;

import java.time.Instant;
import java.time.LocalDateTime;

public interface Event {
    default LocalDateTime getLocalisedTimestamp() {
        return Time.localise(getTimestamp());
    }

    Instant getTimestamp();

    default EventType getType() { return EventType.UNKNOWN; }
}
