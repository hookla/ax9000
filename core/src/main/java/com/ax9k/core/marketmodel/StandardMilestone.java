package com.ax9k.core.marketmodel;

import java.time.LocalTime;
import java.util.Objects;

import static org.apache.commons.lang3.Validate.notNull;

public class StandardMilestone implements Milestone {
    private final LocalTime time;

    private StandardMilestone(LocalTime time) {
        this.time = notNull(time);
    }

    public static Milestone wrap(LocalTime time) {
        return new StandardMilestone(time);
    }

    @Override
    public LocalTime getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        StandardMilestone that = (StandardMilestone) o;
        return Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time);
    }

    @Override
    public String toString() {
        return time.toString();
    }
}
