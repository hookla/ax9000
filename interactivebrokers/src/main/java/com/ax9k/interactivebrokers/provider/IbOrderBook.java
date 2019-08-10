package com.ax9k.interactivebrokers.provider;

import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import com.ax9k.core.marketmodel.orderbook.OrderBookLevel;

import java.time.Instant;
import java.util.Arrays;

import static com.ax9k.core.marketmodel.BidAsk.ASK;

public class IbOrderBook {
    private final OrderBookLevel[] askLevels;
    private final OrderBookLevel[] bidLevels;

    private Instant timestamp;

    public IbOrderBook(int depth) {
        timestamp = Instant.EPOCH;
        askLevels = new OrderBookLevel[depth];
        bidLevels = new OrderBookLevel[depth];
        Arrays.fill(bidLevels, OrderBookLevel.EMPTY);
        Arrays.fill(askLevels, OrderBookLevel.EMPTY);
    }

    public void directBookUpdate(Instant timestamp, int levelIndex, BidAsk side, double price, double quantity) {
        this.timestamp = timestamp;
        var newLevel = new OrderBookLevel(price, quantity);
        if (side == ASK) {
            askLevels[levelIndex] = newLevel;
        } else {
            bidLevels[levelIndex] = newLevel;
        }
    }

    public void clearLevel(int level, BidAsk side) {
        if (side == ASK) {
            askLevels[level] = OrderBookLevel.EMPTY;
        } else {
            bidLevels[level] = OrderBookLevel.EMPTY;
        }
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public OrderBook toImmutableOrderBook(EventType type) {
        return new OrderBook(timestamp, type, askLevels, bidLevels);
    }

    public double getBidPrice(int level) {
        return bidLevels[level].getPrice();
    }

    public double getBidSize(int level) {
        return bidLevels[level].getQuantity();
    }

    public double getAskPrice(int level) {
        return askLevels[level].getPrice();
    }

    public double getAskSize(int level) {
        return askLevels[level].getQuantity();
    }
}