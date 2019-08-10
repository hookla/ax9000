package com.ax9k.backtesting;

import com.ax9k.core.marketmodel.Milestone;

import java.time.LocalTime;

public enum SampleMilestone implements Milestone {
    START_OF_DAY(0, 0),
    AFTER_HOURS_SESSION_END(1, 0),
    PRE_OPENING_START(8, 45),
    MORNING_SESSION_START(9, 15),
    LUNCH_START(12, 0),
    AFTERNOON_SESSION_START(13, 0),
    CLOSE(16, 30),
    AFTER_HOURS_SESSION_START(17, 30);

    private final LocalTime time;

    SampleMilestone(int hours, int minutes) {
        time = LocalTime.of(hours, minutes);
    }

    @Override
    public LocalTime getTime() {
        return time;
    }
}
