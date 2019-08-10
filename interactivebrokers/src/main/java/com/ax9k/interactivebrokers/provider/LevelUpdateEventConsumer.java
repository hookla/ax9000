package com.ax9k.interactivebrokers.provider;

import com.ax9k.core.marketmodel.BidAsk;

import java.time.Instant;

public interface LevelUpdateEventConsumer {
    void send(Instant timeStamp, int level, BidAsk side, double price, int quantity);
}
