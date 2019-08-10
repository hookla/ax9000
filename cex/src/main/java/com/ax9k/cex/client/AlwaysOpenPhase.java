package com.ax9k.cex.client;

import com.ax9k.core.marketmodel.Milestone;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.StandardMilestone;

import java.time.LocalTime;

import static org.apache.commons.lang3.Validate.notNull;

public enum AlwaysOpenPhase implements Phase {
    INSTANCE;

    private static final Milestone MIDNIGHT = StandardMilestone.wrap(LocalTime.MIDNIGHT);

    @Override
    public boolean includes(LocalTime time) {
        notNull(time);
        return true;
    }

    @Override
    public Milestone getStart() {
        return MIDNIGHT;
    }

    @Override
    public Milestone getEnd() {
        return MIDNIGHT;
    }

    @Override
    public String getName() {
        return "trading_session";
    }

    @Override
    public boolean isTradingSession() {
        return true;
    }

    @Override
    public boolean isMarketOpen() {
        return true;
    }

    @Override
    public boolean isAfterMarketClose() {
        return false;
    }
}