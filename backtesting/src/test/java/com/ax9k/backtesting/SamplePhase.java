package com.ax9k.backtesting;

import com.ax9k.core.marketmodel.Milestone;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.StandardTradingSchedule;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.utils.compare.ComparableUtils;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.ax9k.backtesting.SampleMilestone.AFTERNOON_SESSION_START;
import static com.ax9k.backtesting.SampleMilestone.AFTER_HOURS_SESSION_END;
import static com.ax9k.backtesting.SampleMilestone.AFTER_HOURS_SESSION_START;
import static com.ax9k.backtesting.SampleMilestone.CLOSE;
import static com.ax9k.backtesting.SampleMilestone.LUNCH_START;
import static com.ax9k.backtesting.SampleMilestone.MORNING_SESSION_START;

public enum SamplePhase implements Phase {
    PRE_OPEN(AFTER_HOURS_SESSION_END, MORNING_SESSION_START),
    MORNING_SESSION(MORNING_SESSION_START, LUNCH_START),
    LUNCH(LUNCH_START, AFTERNOON_SESSION_START),
    AFTERNOON_SESSION(AFTERNOON_SESSION_START, CLOSE),
    CLOSED(CLOSE, AFTER_HOURS_SESSION_START),
    AFTER_HOURS_SESSION(AFTER_HOURS_SESSION_START, AFTER_HOURS_SESSION_END);

    private static final Set<SamplePhase> TRADING_SESSIONS =
            EnumSet.of(MORNING_SESSION, AFTERNOON_SESSION, AFTER_HOURS_SESSION);
    private static final Set<SamplePhase> OPEN_PHASES =
            EnumSet.of(MORNING_SESSION, LUNCH, AFTERNOON_SESSION, AFTER_HOURS_SESSION);

    private final Milestone start, end;
    private final boolean wrapsIntoNewDay;

    SamplePhase(Milestone start, Milestone end) {
        this.start = start;
        this.end = end;
        if (start != null && end != null) {
            wrapsIntoNewDay = ComparableUtils.lessThan(end.getTime(), start.getTime());
        } else {
            wrapsIntoNewDay = false;
        }
    }

    public static TradingSchedule tradingSchedule() {
        return StandardTradingSchedule.wrap(List.copyOf(EnumSet.allOf(SamplePhase.class)), ZoneOffset.ofHours(8));
    }

    @Override
    public boolean includes(LocalTime time) {
        if (start == null || end == null) {
            return false;
        }
        if (wrapsIntoNewDay) {
            return isBefore(time, end) || isAfterInclusive(time, start);
        }
        return isBetween(time, start, end);
    }

    private static boolean isBefore(LocalTime time, Milestone milestone) {
        return time.isBefore(milestone.getTime());
    }

    private static boolean isBetween(LocalTime time, Milestone beginning, Milestone end) {
        return isAfterInclusive(time, beginning) &&
               isBefore(time, end);
    }

    private static boolean isAfterInclusive(LocalTime time, Milestone milestone) {
        return ComparableUtils.greaterThanOrEqual(time, milestone.getTime());
    }

    @Override
    public Milestone getStart() {
        return start;
    }

    @Override
    public Milestone getEnd() {
        return end;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public boolean isTradingSession() {
        return TRADING_SESSIONS.contains(this);
    }

    @Override
    public boolean isMarketOpen() {
        return OPEN_PHASES.contains(this);
    }

    @Override
    public boolean isAfterMarketClose() {
        return this == CLOSED || this == AFTER_HOURS_SESSION;
    }
}
