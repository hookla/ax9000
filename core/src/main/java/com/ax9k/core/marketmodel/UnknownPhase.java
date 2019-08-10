package com.ax9k.core.marketmodel;

import java.time.LocalTime;

public enum UnknownPhase implements Phase {
    INSTANCE;

    @Override
    public boolean includes(LocalTime time) {
        return false;
    }

    @Override
    public Milestone getStart() {
        throw phaseNotKnownError();
    }

    private static RuntimeException phaseNotKnownError() {
        return new UnsupportedOperationException("Unknown phase");
    }

    @Override
    public Milestone getEnd() {
        throw phaseNotKnownError();
    }

    @Override
    public String getName() {
        return "UNKNOWN";
    }

    @Override
    public boolean isTradingSession() {
        return false;
    }

    @Override
    public boolean isMarketOpen() {
        return false;
    }

    @Override
    public boolean isAfterMarketClose() {
        return false;
    }
}
