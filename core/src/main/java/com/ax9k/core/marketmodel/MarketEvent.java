package com.ax9k.core.marketmodel;

import com.ax9k.core.event.EventType;
import com.ax9k.core.time.Time;
import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Objects;

public class MarketEvent {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final DateTimeFormatter FORMAT;

    static {
        FORMAT = new DateTimeFormatterBuilder()
                .appendPattern("yyyyMMddHHmmss")
                .appendValue(ChronoField.MILLI_OF_SECOND, 3)
                .toFormatter();
    }

    private final Instant createdTimeStamp;
    private final Instant eventTimestamp;
    private final BidAsk side;
    private final EventType messageType;
    private final Type orderType;
    private final long orderId;
    private final double orderQuantity;
    private final double price;
    private double filledQuantity;

    public MarketEvent(Instant eventTimestamp,
                       EventType messageType,
                       long orderId,
                       double price,
                       double quantity,
                       BidAsk side,
                       Type type) {
        this.eventTimestamp = eventTimestamp;
        this.messageType = messageType;
        this.orderId = orderId;
        this.price = price;
        this.side = side;
        orderQuantity = quantity;
        createdTimeStamp = Instant.now();
        orderType = type;
    }

    private static Instant parseDateTimeToEpoch(String s) {
        return Time.internationalise(LocalDateTime.parse(s, FORMAT));
    }

    @JsonIgnore
    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    @SuppressWarnings("unused") /* Jackson */
    public String getEventTime() {
        return Time.toTimeString(getLocalisedEventTimestamp());
    }

    @JsonIgnore
    public LocalDateTime getLocalisedEventTimestamp() {
        return Time.localise(eventTimestamp);
    }

    public EventType getMessageType() {
        return messageType;
    }

    public long getOrderId() {
        return orderId;
    }

    public double getPrice() {
        return price;
    }

    public double getOrderQuantity() {
        return orderQuantity;
    }

    public BidAsk getSide() {
        return side;
    }

    public void fill(double newFilledQuantity) {
        filledQuantity += newFilledQuantity;
        double remaining = getRemainingQuantity();

        if (remaining < 0) {
            LOGGER.error(
                    "new Order Quantity should not be less than 0. something wrong. Order ID: {} new Quantity {}",
                    getUniqueOrderId(),
                    newFilledQuantity
            );
        } else if (remaining > 0) {
            LOGGER.trace("processed a partial fill");
        } else if (remaining == 0) {
            LOGGER.trace("processed a full fill");
        }
    }

    public double getRemainingQuantity() {
        return orderQuantity - filledQuantity;
    }

    /**
     * orderIDs are not unique across bids and asks so need to make one of them -ve.
     *
     * @return makes bids +ve and asks -ve
     */
    public long getUniqueOrderId() {
        long uniqueOrderId;
        if (side == BidAsk.BID) {
            uniqueOrderId = orderId;
        } else if (side == BidAsk.ASK) {
            uniqueOrderId = -orderId;
        } else {
            uniqueOrderId = 0;
        }

        return uniqueOrderId;
    }

    public double getValue() {
        return getRemainingQuantity() * price;
    }

    public long getLag() {
        return Duration.between(eventTimestamp, createdTimeStamp).toMillis();
    }

    public Type getOrderType() {
        return orderType;
    }

    @Override
    public int hashCode() {
        int result;
        result = Objects.hashCode(eventTimestamp);
        result = 31 * result + Objects.hashCode(side);
        result = 31 * result + Objects.hashCode(messageType);
        result = 31 * result + Objects.hashCode(orderType);
        result = 31 * result + Long.hashCode(orderId);
        result = 31 * result + Double.hashCode(orderQuantity);
        result = 31 * result + Double.hashCode(price);
        result = 31 * result + Double.hashCode(filledQuantity);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        MarketEvent that = (MarketEvent) other;
        return orderId == that.orderId &&
               Double.compare(that.orderQuantity, orderQuantity) == 0 &&
               Double.compare(that.price, price) == 0 &&
               Double.compare(that.filledQuantity, filledQuantity) == 0 &&
               Objects.equals(eventTimestamp, that.eventTimestamp) &&
               side == that.side &&
               messageType == that.messageType &&
               orderType == that.orderType;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }

    public enum Type {
        MARKET_ORDER, LIMIT_ORDER, UNKNOWN_ORDER_TYPE
    }
}
