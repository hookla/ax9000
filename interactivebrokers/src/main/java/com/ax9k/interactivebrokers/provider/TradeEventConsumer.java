package com.ax9k.interactivebrokers.provider;

import com.ax9k.core.marketmodel.Trade;

@FunctionalInterface
public interface TradeEventConsumer {
    void send(Trade trade);
}
