package com.ax9k.app.contract;

import com.ax9k.core.marketmodel.Milestone;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;

public final class JsonMilestone implements Milestone {
    private final LocalTime time;

    @JsonCreator
    private JsonMilestone(@JsonProperty(value = "time", required = true) String time) {
        this(LocalTime.parse(time));
    }

    private JsonMilestone(LocalTime time) {
        this.time = time;
    }

    @Override
    public LocalTime getTime() {
        return time;
    }

    @Override
    public String toString() {
        return time.toString();
    }
}
