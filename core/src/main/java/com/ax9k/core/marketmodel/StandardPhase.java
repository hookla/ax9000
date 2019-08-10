package com.ax9k.core.marketmodel;

import com.ax9k.utils.compare.ComparableUtils;
import com.ax9k.utils.json.JsonUtils;

import java.time.LocalTime;
import java.util.Objects;

import static org.apache.commons.lang3.Validate.notNull;

public final class StandardPhase implements Phase {
    private final Milestone start;
    private final Milestone end;
    private final String name;
    private final boolean tradingSession;
    private final boolean marketIsOpen;
    private final boolean afterMarketClose;
    private final boolean wrapsIntoNewDay;

    public StandardPhase(Milestone start,
                         Milestone end,
                         String name,
                         boolean tradingSession,
                         boolean marketIsOpen,
                         boolean afterMarketClose) {
        this.start = notNull(start, "start");
        this.end = notNull(end, "end");
        this.name = notNull(name, "name");
        this.tradingSession = tradingSession;
        this.marketIsOpen = marketIsOpen;
        this.afterMarketClose = afterMarketClose;

        wrapsIntoNewDay = ComparableUtils.lessThan(end.getTime(), start.getTime());
    }

    @Override
    public boolean includes(LocalTime time) {
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
        return name;
    }

    @Override
    public boolean isTradingSession() {
        return tradingSession;
    }

    @Override
    public boolean isMarketOpen() {
        return marketIsOpen;
    }

    @Override
    public boolean isAfterMarketClose() {
        return afterMarketClose;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        StandardPhase that = (StandardPhase) o;
        return tradingSession == that.tradingSession &&
               marketIsOpen == that.marketIsOpen &&
               afterMarketClose == that.afterMarketClose &&
               wrapsIntoNewDay == that.wrapsIntoNewDay &&
               Objects.equals(start, that.start) &&
               Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, name, tradingSession, marketIsOpen, afterMarketClose, wrapsIntoNewDay);
    }
}