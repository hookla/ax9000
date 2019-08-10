package com.ax9k.core.marketmodel;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Predicate;

import static org.apache.commons.lang3.Validate.notNull;

public interface TradingSchedule {
    default Phase phaseForTime(Instant timestamp) {
        return timestamp != null ? phaseForTime(LocalTime.ofInstant(timestamp, getTimeZone())) : UnknownPhase.INSTANCE;
    }

    ZoneId getTimeZone();

    default Phase phaseForTime(LocalTime time) {
        if (time == null) {
            return UnknownPhase.INSTANCE;
        }

        for (Phase phase : getPhases()) {
            if (phase.includes(time)) {
                return phase;
            }
        }

        return UnknownPhase.INSTANCE;
    }

    List<Phase> getPhases();

    default Phase phaseForTime(LocalDateTime timestamp) {
        return timestamp != null ? phaseForTime(timestamp.toLocalTime()) : UnknownPhase.INSTANCE;
    }

    default Phase marketClose() {
        return firstMatchingPhase(Phase.IS_AFTER_MARKET_CLOSE);
    }

    default Phase firstMatchingPhase(Predicate<Phase> filter) {
        for (Phase phase : getPhases()) {
            if (filter.test(phase)) {
                return phase;
            }
        }

        return null;
    }

    default Phase marketOpen() {
        return firstMatchingPhase(Phase.MARKET_IS_OPEN);
    }

    default Phase firstTradingSession() {
        return firstMatchingPhase(Phase.IS_TRADING_SESSION);
    }

    default Phase lastTradingSession() {
        return lastMatchingPhase(Phase.IS_TRADING_SESSION);
    }

    default Phase lastMatchingPhase(Predicate<Phase> filter) {
        List<Phase> allPhases = getPhases();

        for (int i = allPhases.size() - 1; i >= 0; i--) {
            Phase phase = allPhases.get(i);
            if (filter.test(phase)) {
                return phase;
            }
        }

        return null;
    }

    default int getPhaseIndex(Phase phase, Predicate<Phase> filter) {
        return ArrayUtils.indexOf(filterPhases(filter), phase);
    }

    private Phase[] filterPhases(Predicate<Phase> filter) {
        List<Phase> allPhases = getPhases();

        if (filter == null) {
            return allPhases.toArray(new Phase[0]);
        }

        Phase[] result = new Phase[allPhases.size()];
        int index = 0;
        for (Phase phase : allPhases) {
            if (filter.test(phase)) {
                result[index++] = phase;
            }
        }
        return result;
    }

    default int getPhaseIndex(Phase phase) {
        return getPhases().indexOf(phase);
    }

    default Phase phaseForIndex(int index, Predicate<Phase> filter) {
        Validate.isTrue(index >= 0, "index must be positive");
        Phase[] filtered = filterPhases(filter);

        return index < filtered.length ? filtered[index] : null;
    }

    default Phase phaseForIndex(int index) throws ArrayIndexOutOfBoundsException {
        return getPhases().get(index);
    }

    default Phase phaseForIdentifier(String identifier) {
        Phase[] identifiedPhases = filterPhases(phase -> StringUtils.containsIgnoreCase(phase.getName(), identifier));

        Validate.isTrue(!(identifiedPhases.length > 1 && identifiedPhases[1] != null),
                        "Multiple phases correspond to identifier '%s'. Try something more specific: %s",
                        identifier,
                        identifiedPhases);

        return identifiedPhases[0];
    }

    default Phase previousPhase(Phase phase) {
        notNull(phase);

        Phase previousPhase = null;
        for (Phase currentPhase : getPhases()) {
            if (currentPhase.equals(phase)) {
                return previousPhase;
            }
            previousPhase = currentPhase;
        }

        throw new IllegalArgumentException("Phase is not supported by the current contract: " + phase);
    }

    default Phase nextPhase(Phase phase) {
        notNull(phase);
        List<Phase> allPhases = getPhases();

        Phase nextPhase = null;
        for (int i = getPhases().size() - 1; i >= 0; i--) {
            Phase currentPhase = allPhases.get(i);
            if (currentPhase.equals(phase)) {
                return nextPhase;
            }
            nextPhase = currentPhase;
        }
        throw new IllegalArgumentException("Phase is not supported by the current contract: " + phase);
    }
}
