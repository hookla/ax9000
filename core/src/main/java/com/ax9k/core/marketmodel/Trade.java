package com.ax9k.core.marketmodel;

import com.ax9k.core.event.Event;
import com.ax9k.core.event.EventType;
import com.ax9k.core.time.Time;
import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

@JsonPropertyOrder("eventTime")
public final class Trade implements Event {
    public static final Trade EMPTY = new Trade(Instant.EPOCH, 0, 0, 0, BidAsk.NONE);

    private final Instant timestamp;
    private final BidAsk side;
    private final long uniqueOrderId;
    private final double price;
    private final double quantity;

    public Trade(Instant timestamp, double price, double quantity, long orderId, BidAsk side) {
        this.price = price;
        this.timestamp = timestamp;
        this.quantity = quantity;
        this.side = side;
        uniqueOrderId = orderId;
    }

    public double getPrice() {
        return price;
    }

    public String getEventTime() {
        return Time.toTimeString(getLocalisedTimestamp());
    }

    @Override
    @JsonIgnore
    public LocalDateTime getLocalisedTimestamp() {
        return Time.localise(timestamp);
    }

    @Override
    @JsonIgnore
    public EventType getType() {
        return EventType.TRADE;
    }

    @Override
    @JsonIgnore
    public Instant getTimestamp() {
        return timestamp;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPriceXQuantity() {
        return quantity * price;
    }

    public BidAsk getSide() {
        return side;
    }

    public long getUniqueOrderId() {
        return uniqueOrderId;
    }

    @Override
    public int hashCode() {
        int result;
        result = Objects.hashCode(timestamp);
        result = 31 * result + Long.hashCode(uniqueOrderId);
        result = 31 * result + Double.hashCode(price);
        result = 31 * result + Double.hashCode(quantity);
        result = 31 * result + Objects.hashCode(side);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        Trade trade = (Trade) o;
        return uniqueOrderId == trade.uniqueOrderId &&
               Double.compare(trade.price, price) == 0 &&
               Double.compare(trade.quantity, quantity) == 0 &&
               Objects.equals(timestamp, trade.timestamp) &&
               side == trade.side;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }
}
