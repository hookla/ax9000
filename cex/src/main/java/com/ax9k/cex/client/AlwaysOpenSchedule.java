package com.ax9k.cex.client;

import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Predicate;

import static org.apache.commons.lang3.Validate.notNull;

public enum AlwaysOpenSchedule implements TradingSchedule {
    INSTANCE;

    private static final List<Phase> SINGLE_PHASE = List.of(AlwaysOpenPhase.INSTANCE);

    @Override
    public Phase phaseForTime(Instant timestamp) {
        notNull(timestamp);
        return AlwaysOpenPhase.INSTANCE;
    }

    @Override
    public ZoneId getTimeZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Phase phaseForTime(LocalTime time) {
        notNull(time);
        return AlwaysOpenPhase.INSTANCE;
    }

    @Override
    public List<Phase> getPhases() {
        return SINGLE_PHASE;
    }

    @Override
    public Phase phaseForTime(LocalDateTime timestamp) {
        notNull(timestamp);
        return AlwaysOpenPhase.INSTANCE;
    }

    @Override
    public Phase marketClose() {
        return null;
    }

    @Override
    public Phase firstMatchingPhase(Predicate<Phase> filter) {
        return lastMatchingPhase(filter);
    }

    @Override
    public Phase lastMatchingPhase(Predicate<Phase> filter) {
        return filter.test(AlwaysOpenPhase.INSTANCE) ? AlwaysOpenPhase.INSTANCE : null;
    }

    @Override
    public Phase marketOpen() {
        return AlwaysOpenPhase.INSTANCE;
    }

    @Override
    public Phase firstTradingSession() {
        return AlwaysOpenPhase.INSTANCE;
    }

    @Override
    public Phase lastTradingSession() {
        return AlwaysOpenPhase.INSTANCE;
    }

    @Override
    public int getPhaseIndex(Phase phase, Predicate<Phase> filter) {
        return filter.test(AlwaysOpenPhase.INSTANCE) && phase == AlwaysOpenPhase.INSTANCE ? 0 : -1;
    }

    @Override
    public int getPhaseIndex(Phase phase) {
        return phase == AlwaysOpenPhase.INSTANCE ? 0 : -1;
    }

    @Override
    public Phase phaseForIndex(int index, Predicate<Phase> filter) {
        return filter.test(phaseForIndex(index)) ? AlwaysOpenPhase.INSTANCE : null;
    }

    @Override
    public Phase phaseForIndex(int index) throws ArrayIndexOutOfBoundsException {
        if (index != 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return AlwaysOpenPhase.INSTANCE;
    }

    @Override
    public Phase phaseForIdentifier(String identifier) {
        return AlwaysOpenPhase.INSTANCE.getName().contains(identifier) ? AlwaysOpenPhase.INSTANCE : null;
    }

    @Override
    public Phase previousPhase(Phase phase) {
        return null;
    }

    @Override
    public Phase nextPhase(Phase phase) {
        return null;
    }

    @Override
    @JsonValue
    public String toString() {
        return "ALWAYS_OPEN_SCHEDULE";
    }
}
