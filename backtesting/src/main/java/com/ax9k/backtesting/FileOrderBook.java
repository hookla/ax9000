package com.ax9k.backtesting;

import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.MarketEvent;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import com.ax9k.core.marketmodel.orderbook.OrderBookLevel;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static com.ax9k.core.marketmodel.BidAsk.ASK;
import static com.ax9k.core.marketmodel.BidAsk.BID;

class FileOrderBook {
    private static final int BOOK_DEPTH = 5;

    private static final Comparator<Map.Entry<Double, Double>> LOWEST_TO_HIGHEST_PRICE =
            Comparator.comparingDouble(Map.Entry::getKey);
    private static final Comparator<Map.Entry<Double, Double>> HIGHEST_TO_LOWEST_PRICE =
            LOWEST_TO_HIGHEST_PRICE.reversed();

    private OrderBookLevel[] askLevels;
    private OrderBookLevel[] bidLevels;
    private Instant timestamp;

    private final Map<Double, Double> asks;
    private final Map<Double, Double> bids;

    FileOrderBook(Instant timestamp) {
        asks = new HashMap<>();
        bids = new HashMap<>();
        askLevels = new OrderBookLevel[BOOK_DEPTH];
        bidLevels = new OrderBookLevel[BOOK_DEPTH];
        Arrays.fill(askLevels, OrderBookLevel.EMPTY);
        Arrays.fill(bidLevels, OrderBookLevel.EMPTY);
        this.timestamp = timestamp;
    }

    void processTradeEvent(MarketEvent exchangeMessage, MarketEvent existingOrder) {
        timestamp = exchangeMessage.getEventTimestamp();
        double price = existingOrder.getPrice();
        double quantityDelta = -exchangeMessage.getOrderQuantity();
        Map<Double, Double> priceQuantityMap = priceQuantityMapFor(existingOrder);
        priceQuantityMap.merge(price, quantityDelta, Double::sum);

        updateBookLevels(priceQuantityMap, existingOrder.getSide());
    }

    private Map<Double, Double> priceQuantityMapFor(MarketEvent exchangeMessage) {
        return exchangeMessage.getSide() == BID ? bids : asks;
    }

    private void updateBookLevels(Map<Double, Double> priceQuantityMap, BidAsk side) {
        if (side == BID) {
            updateBids(priceQuantityMap);
        } else if (side == ASK) {
            updateAsks(priceQuantityMap);
        } else {
            throw new IllegalArgumentException("unexpected side: " + side);
        }
    }

    private void updateBids(Map<Double, Double> priceQuantityMap) {
        bidLevels = mapToLevels(priceQuantityMap, HIGHEST_TO_LOWEST_PRICE);
    }

    private OrderBookLevel[] mapToLevels(Map<Double, Double> priceQuantityMap,
                                         Comparator<Map.Entry<Double, Double>> order) {
        return priceQuantityMap.entrySet()
                               .stream()
                               .filter(entry -> entry.getValue() > 0)
                               .sorted(order)
                               .limit(BOOK_DEPTH)
                               .map(entry -> new OrderBookLevel(entry.getKey(), entry.getValue()))
                               .toArray(this::baseLevels);
    }

    private void updateAsks(Map<Double, Double> priceQuantityMap) {
        askLevels = mapToLevels(priceQuantityMap, LOWEST_TO_HIGHEST_PRICE);
    }

    void processAddOrderEvent(MarketEvent exchangeMessage) {
        timestamp = exchangeMessage.getEventTimestamp();
        double price = exchangeMessage.getPrice();
        double quantityDelta = exchangeMessage.getOrderQuantity();
        Map<Double, Double> priceQuantityMap = priceQuantityMapFor(exchangeMessage);
        priceQuantityMap.merge(price, quantityDelta, Double::sum);

        updateBookLevels(priceQuantityMap, exchangeMessage.getSide());
    }

    void processDeleteOrder(MarketEvent existingOrder, MarketEvent exchangeMessage) {
        timestamp = exchangeMessage.getEventTimestamp();
        double price = existingOrder.getPrice();
        Map<Double, Double> priceQuantityMap = priceQuantityMapFor(existingOrder);
        priceQuantityMap.remove(price);
        updateBookLevels(priceQuantityMap, existingOrder.getSide());
    }

    @SuppressWarnings("unused")
    private OrderBookLevel[] baseLevels(int ignore) {
        OrderBookLevel[] result = new OrderBookLevel[BOOK_DEPTH];
        java.util.Arrays.fill(result, OrderBookLevel.EMPTY);
        return result;
    }

    double getBidPrice(int level) {
        return bidLevels[level].getPrice();
    }

    double getBidSize(int level) {
        return bidLevels[level].getQuantity();
    }

    double getAskPrice(int level) {
        return askLevels[level].getPrice();
    }

    double getAskSize(int level) {
        return askLevels[level].getQuantity();
    }

    OrderBook toOrderBook(EventType type) {
        return new OrderBook(timestamp, type, askLevels, bidLevels);
    }

    OrderBookLevel[] getAsks() {
        return askLevels;
    }

    OrderBookLevel[] getBids() {
        return bidLevels;
    }
}