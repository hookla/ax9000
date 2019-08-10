package com.ax9k.core.marketmodel;

import java.time.LocalTime;
import java.util.function.Predicate;

public interface Phase {
    Predicate<Phase> IS_TRADING_SESSION = Phase::isTradingSession;
    Predicate<Phase> MARKET_IS_OPEN = Phase::isMarketOpen;
    Predicate<Phase> IS_AFTER_MARKET_CLOSE = Phase::isAfterMarketClose;

    boolean includes(LocalTime time);

    Milestone getStart();

    Milestone getEnd();

    String getName();

    boolean isTradingSession();

    boolean isMarketOpen();

    boolean isAfterMarketClose();

    default boolean isOneOf(Phase... phases) {
        for (Phase phase : phases) {
            if (this.equals(phase)) {
                return true;
            }
        }
        return false;
    }

    default boolean isNoneOf(Phase... phases) {
        for (Phase phase : phases) {
            if (this.equals(phase)) {
                return false;
            }
        }
        return true;
    }
}
