package com.ax9k.core.marketmodel;

import java.time.ZoneId;

public interface Contract {
    ZoneId getTimeZone();

    String getExchange();

    String getLocalSymbol();

    double getCostPerTrade();

    int getMultiplier();

    TradingSchedule getTradingSchedule();
}
