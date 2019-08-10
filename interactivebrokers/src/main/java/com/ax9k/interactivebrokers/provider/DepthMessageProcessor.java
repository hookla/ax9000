package com.ax9k.interactivebrokers.provider;

import com.ax9k.core.marketmodel.BidAsk;

import java.time.Instant;

class DepthMessageProcessor {
    private static final int UNINITIALISED = -999;

    private final LevelUpdateEventConsumer updateCallback;
    private final Runnable onUpdateEvent;

    private int lastLevel;
    private BidAsk lastSide;
    private Instant lastTimestamp;

    private double lastPrice;
    private double lastQuantity;

    private int eventCount;

    DepthMessageProcessor(LevelUpdateEventConsumer updateCallback, Runnable onUpdateEvent) {
        this.updateCallback = updateCallback;
        this.onUpdateEvent = onUpdateEvent;
    }

    void processUpdate(Instant timestamp, int level, BidAsk side, double price, double quantity) {
        if (isNewEvent(level, side)) {
            sendUpdateEvent(lastTimestamp, lastLevel, lastSide, lastPrice, lastQuantity);
            updateValues(timestamp, level, side, price, quantity);
            return;
        }

        price = price > 0 ? price : lastPrice;
        quantity = quantity > 0 ? quantity : lastQuantity;
        if (isSecondEvent()) {
            sendUpdateEvent(timestamp, level, side, price, quantity);
            resetValues();
        } else {
            updateValues(timestamp, level, side, price, quantity);
        }
    }

    private boolean isNewEvent(int level, BidAsk side) {
        if (lastLevel == UNINITIALISED || lastSide == null) {
            return false;
        }
        return level != lastLevel || side != lastSide;
    }

    private void sendUpdateEvent(Instant timeStamp, int level, BidAsk side, double price, double quantity) {
        updateCallback.send(timeStamp, level, side, price, (int) quantity);
        resetValues();
        onUpdateEvent.run();
    }

    private void resetValues() {
        lastSide = null;
        lastLevel = UNINITIALISED;
        lastPrice = lastQuantity = 0;
        eventCount = 0;
    }

    private void updateValues(Instant timestamp, int level, BidAsk side, double price, double quantity) {
        lastSide = side;
        lastLevel = level;
        lastPrice = price;
        lastQuantity = quantity;
        lastTimestamp = timestamp;
        eventCount = 1;
    }

    private boolean isSecondEvent() {
        return ++eventCount == 2;
    }
}
