package com.ax9k.backtesting;

import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.MarketEvent;

import java.time.Instant;

public class TestMarketOrders {
    private static final int MAXIMUM_CACHE_PRICE = 50;
    private static final int MAXIMUM_CACHE_QUANTITY = 50;
    private static final MarketEvent[][] BID = createTestArray(BidAsk.BID);
    private static final MarketEvent[][] ASK = createTestArray(BidAsk.ASK);

    private TestMarketOrders() { throw new AssertionError(); }

    private static MarketEvent[][] createTestArray(BidAsk side) {
        MarketEvent[][] result = new MarketEvent[MAXIMUM_CACHE_QUANTITY][MAXIMUM_CACHE_PRICE];

        for (int quantity = 0; quantity < MAXIMUM_CACHE_QUANTITY; quantity++) {
            for (int price = 0; price < MAXIMUM_CACHE_PRICE; price++) {
                result[quantity][price] = createMarketOrder(side, quantity, price);
            }
        }
        return result;
    }

    private static MarketEvent createMarketOrder(BidAsk side, int quantity, int price) {
        int dummyValue = Integer.valueOf(quantity + "" + price);
        return new MarketEvent(
                Instant.EPOCH,
                EventType.UNKNOWN,
                dummyValue,
                price,
                quantity,
                side,
                MarketEvent.Type.MARKET_ORDER
        );
    }

    public static MarketEvent testBidOrder(int quantity, int price) {
        if (inTestOrdersRange(quantity, price)) {
            return BID[quantity][price];
        }
        return createMarketOrder(BidAsk.BID, quantity, price);
    }

    private static boolean inTestOrdersRange(int quantity, int price) {
        return inRange(quantity, MAXIMUM_CACHE_QUANTITY) &&
               inRange(price, MAXIMUM_CACHE_PRICE);
    }

    private static boolean inRange(int value, int max) {
        return value >= 0 && value < max;
    }

    public static MarketEvent testAskOrder(int quantity, int price) {
        if (inTestOrdersRange(quantity, price)) {
            return ASK[quantity][price];
        }
        return createMarketOrder(BidAsk.ASK, quantity, price);
    }
}
