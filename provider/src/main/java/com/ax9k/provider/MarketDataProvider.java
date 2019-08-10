package com.ax9k.provider;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

public interface MarketDataProvider {
    boolean isConnected();

    void startRequest(boolean delayUntilMarketOpen);

    void stopRequest();

    String getSource();

    LocalDate getDate();

    String getSymbol();

    default Collection<Class<?>> getExtraDataTypes() {
        return Collections.emptyList();
    }
}
