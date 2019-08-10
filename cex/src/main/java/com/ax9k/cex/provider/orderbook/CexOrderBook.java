package com.ax9k.cex.provider.orderbook;

import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import com.ax9k.core.marketmodel.orderbook.OrderBookLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@JsonIgnoreProperties({ "e", "oid", "ok" })
public final class CexOrderBook {
    public static final int DEPTH = 5;
    private static final Logger ERROR_LOG = LogManager.getLogger("error");
    private final OrderBookLevels asks, bids;

    private Instant timestamp;
    private int id;

    private CexOrderBook(Instant timestamp, OrderBookLevel[] asks, OrderBookLevel[] bids, int id) {
        this.asks = new OrderBookLevels(BookLevelComparators.ASKS, DEPTH);
        this.bids = new OrderBookLevels(BookLevelComparators.BIDS, DEPTH);
        this.timestamp = timestamp;
        this.id = id;
        Validate.notNull(asks);
        Validate.notNull(bids);
        Validate.notNull(timestamp);
        Validate.isTrue(asks.length == bids.length, "Different number of ask and bid levels: %s, %s", asks, bids);
        this.asks.set(asks);
        this.bids.set(bids);
    }

    public static CexOrderBook of(JsonNode dataNode) {
        return new CexOrderBook(Instant.ofEpochSecond(dataNode.get("timestamp").asInt()),
                                mapToBookLevels(dataNode.get("asks")),
                                mapToBookLevels(dataNode.get("bids")),
                                dataNode.get("id").asInt());
    }

    private static OrderBookLevel[] mapToBookLevels(JsonNode levelsNode) {
        List<OrderBookLevel> levels = new ArrayList<>(10);

        Iterator<JsonNode> elements = levelsNode.elements();
        while (elements.hasNext()) {
            JsonNode levelNode = elements.next();
            double price = levelNode.get(0).asDouble();
            double quantity = levelNode.get(1).asDouble();
            levels.add(new OrderBookLevel(price, quantity));
        }

        return levels.toArray(new OrderBookLevel[0]);
    }

    public void update(JsonNode newData) {
        int newId = newData.get("id").asInt();
        if (newId != id + 1) {
            ERROR_LOG.warn("An order book update message was missed. Presentation of the " +
                           "order book is now inconsistent with CEX. Previous book ID: {}, Current ID: {}",
                           id, newId);
        }
        id = newId;
        timestamp = Instant.ofEpochMilli(newData.get("time").asLong());
        addLevels(asks, newData.get("asks"));
        addLevels(bids, newData.get("bids"));
    }

    private void addLevels(OrderBookLevels levels, JsonNode newLevels) {
        Iterator<JsonNode> elements = newLevels.elements();
        while (elements.hasNext()) {
            JsonNode levelNode = elements.next();
            double price = levelNode.get(0).asDouble();
            double quantity = levelNode.get(1).asDouble();
            levels.add(new OrderBookLevel(price, quantity));
        }
    }

    public OrderBook toImmutableBook(EventType type) {
        return new OrderBook(timestamp,
                             type,
                             asks.toArray(),
                             bids.toArray());
    }
}
