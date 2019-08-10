package com.ax9k.core.marketmodel;

import com.ax9k.utils.json.JsonUtils;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.Validate.notNull;

public final class StandardTradingSchedule implements TradingSchedule {
    private final List<Phase> allPhases;
    private final ZoneId timeZone;

    private StandardTradingSchedule(List<? extends Phase> allPhases, ZoneId timeZone) {
        this.allPhases = unmodifiableList(notNull(allPhases, "allPhases"));
        this.timeZone = timeZone;
    }

    public static TradingSchedule wrap(List<? extends Phase> phases, ZoneId timeZone) {
        return new StandardTradingSchedule(phases, timeZone);
    }

    @Override
    public List<Phase> getPhases() {
        return allPhases;
    }

    @Override
    public ZoneId getTimeZone() {
        return timeZone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        StandardTradingSchedule that = (StandardTradingSchedule) o;
        return Objects.equals(allPhases, that.allPhases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allPhases);
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }
}
