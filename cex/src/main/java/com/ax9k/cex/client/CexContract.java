package com.ax9k.cex.client;

import com.ax9k.core.marketmodel.Contract;
import com.ax9k.core.marketmodel.TradingSchedule;

import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.apache.commons.lang3.Validate.notNull;

public final class CexContract implements Contract {
    private final Pair currencies;

    public CexContract(Pair currencies) {
        this.currencies = notNull(currencies);
    }

    public Pair getCurrencies() {
        return currencies;
    }

    @Override
    public ZoneId getTimeZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public String getExchange() {
        return "CEX.io";
    }

    @Override
    public String getLocalSymbol() {
        return currencies.toString();
    }

    @Override
    public double getCostPerTrade() {
        return 0;
    }

    @Override
    public int getMultiplier() {
        return 1;
    }

    @Override
    public TradingSchedule getTradingSchedule() {
        return AlwaysOpenSchedule.INSTANCE;
    }
}
