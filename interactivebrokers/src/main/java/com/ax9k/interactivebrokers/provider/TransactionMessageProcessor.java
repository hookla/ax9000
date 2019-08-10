package com.ax9k.interactivebrokers.provider;

import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.Trade;

import java.time.Instant;

class TransactionMessageProcessor {
    private final TradeEventConsumer callback;

    private BidAsk side;
    private Instant lastTime;
    private double lastPrice;
    private double lastSize;

    TransactionMessageProcessor(TradeEventConsumer callback) {
        this.callback = callback;
    }

    void process(TickType valueType, double value, double midPrice) {
        boolean wasValidType = updateValue(valueType, value, midPrice);
        if (!wasValidType) { return; }

        sendTradeEvent();
    }

    private boolean updateValue(TickType valueType, double value, double midPrice) {
        switch (valueType) {
            case LAST_PRICE:
                lastPrice = value;
                if (midPrice >= lastPrice) {
                    side = BidAsk.BID;
                } else {
                    side = BidAsk.ASK;
                }
                break;
            case LAST_SIZE:
                lastSize = value;
                break;
            case LAST_TIME_STAMP:
                lastTime = Instant.ofEpochSecond((long) value);
                break;
            default:
                return false;
        }
        return true;
    }

    private void sendTradeEvent() {
        if (tradeDataComplete()) {
            Trade trade = new Trade(lastTime, lastPrice, lastSize, -999, side);
            callback.send(trade);
            resetValues();
        }
    }

    private boolean tradeDataComplete() {
        return lastTime != null && lastPrice != 0d && lastSize != 0d;
    }

    private void resetValues() {
        side = null;
        lastTime = null;
        lastPrice = lastSize = 0d;
    }
}
