package com.ax9k.core.time;

import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.TradingSchedule;
import org.apache.commons.lang3.Validate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.Validate.notNull;

public final class Time {
    private static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static final AtomicReference<Instant> lastEvent = new AtomicReference<>(Instant.EPOCH);

    private static Boolean live;
    private static TradingSchedule schedule;
    private static ZoneId timeZone;

    private static Phase lastKnownPhase;

    private Time() {
        throw new AssertionError("Time is not instantiable");
    }

    @Deprecated
    public static void setLive(boolean liveTradingMode) {
        Validate.validState(live == null, "Live mode already set to: %s", live);
        live = liveTradingMode;
    }

    @Deprecated
    public static void setTradingSchedule(TradingSchedule tradingSchedule) {
        schedule = notNull(tradingSchedule);
        timeZone = notNull(tradingSchedule.getTimeZone());
    }

    public static void update(Instant timestamp) {
        lastEvent.set(timestamp);
    }

    public static LocalDateTime today() {
        return localise(now());
    }

    public static Instant now() {
        if (live == Boolean.TRUE) {
            return Instant.now();
        }
        return lastEvent.get();
    }

    public static LocalDateTime localise(Instant timestamp) {
        return LocalDateTime.ofInstant(timestamp, timeZone());
    }

    private static ZoneId timeZone() {
        Validate.validState(timeZone != null, "Time zone not initialised");
        return timeZone;
    }

    public static TradingSchedule schedule() {
        return schedule;
    }

    public static Phase currentPhase() {
        Validate.validState(schedule != null, "Trading schedule not initialised");

        LocalTime currentTime = currentTime();
        if (lastKnownPhase != null && lastKnownPhase.includes(currentTime)) {
            return lastKnownPhase;
        }

        return lastKnownPhase = schedule.phaseForTime(currentTime);
    }

    public static LocalTime currentTime() {
        return localiseTime(now());
    }

    public static LocalTime localiseTime(Instant timestamp) {
        return LocalTime.ofInstant(timestamp, timeZone());
    }

    public static Instant todayAt(LocalTime time) {
        return internationalise(LocalDateTime.of(currentDate(), time));
    }

    public static LocalDate currentDate() { return localiseDate(now()); }

    public static LocalDate localiseDate(Instant timestamp) {
        return LocalDate.ofInstant(timestamp, timeZone());
    }

    public static Instant internationalise(LocalDateTime dateTime) {
        return dateTime.atZone(timeZone()).toInstant();
    }

    public static String toTimeString(LocalDateTime dateTime) {
        return dateTime.format(TIME_ONLY);
    }
}
