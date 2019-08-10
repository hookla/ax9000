package com.ax9k.positionmanager;

import com.ax9k.core.event.Event;
import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.time.Time;
import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.util.stream.Collectors.averagingDouble;
import static org.apache.commons.lang3.Validate.notNull;

@JsonPropertyOrder({
                           "localisedCreatedTimestamp", "status", "orderPnL", "triggeringEvent", "pending",
                           "orderCreationDelay", "fillDelay", "totalDelay", "side",
                           "positionAction", "orderedQuantity", "orderedPrice", "filledQuantity"
                   })
public class Order {
    static final Order EMPTY = new Order("DUMMY-ORDER",
                                         Instant.EPOCH,
                                         null,
                                         null,
                                         "EMPTY",
                                         0,
                                         0,
                                         BidAsk.NONE,
                                         Order.Type.UNKNOWN_ORDER_TYPE,
                                         PositionAction.NEITHER,
                                         0,
                                         0,
                                         0);
    static final Order ERROR = new Order("DUMMY-ORDER",
                                         Instant.EPOCH,
                                         null,
                                         null,
                                         "ERROR",
                                         0,
                                         0,
                                         BidAsk.NONE,
                                         Order.Type.UNKNOWN_ORDER_TYPE,
                                         PositionAction.NEITHER,
                                         0,
                                         0,
                                         0);

    private static final Logger LOGGER = LogManager.getLogger();
    private final Deque<Fill> fills = new ArrayDeque<>();
    private final Instant recordedTimestamp;
    private final Instant sentTimestamp;
    private final Event triggeringEvent;
    private final BidAsk side;
    private final Order.Type type;
    private final String id;
    private final Duration orderCreationDelay;
    private final double orderedQuantity;
    private final double orderedPrice;
    private final PositionAction positionAction;
    private final double contractMultiplier;
    private final String source;
    private Fill lastFill = Fill.EMPTY;
    private double filledQuantity;
    private double enterPositionPrice;
    private double exitPositionPrice;
    private double orderPnl;
    private double stopPrice;

    Order(String source,
          Instant recordedTimestamp,
          Instant sentTimestamp, Event triggeringEvent,
          String id,
          double price,
          double quantity,
          BidAsk side,
          Type type,
          PositionAction positionAction,
          double contractMultiplier,
          double enterPositionPrice,
          double stopPrice) {
        this.source = notNull(source);
        this.recordedTimestamp = notNull(recordedTimestamp);
        this.sentTimestamp = sentTimestamp;
        this.triggeringEvent = triggeringEvent;
        this.id = notNull(id);
        this.orderedPrice = price;
        this.orderedQuantity = quantity;
        this.side = notNull(side);
        this.type = notNull(type);
        this.positionAction = notNull(positionAction);
        this.contractMultiplier = contractMultiplier;
        if (positionAction == PositionAction.EXIT) {
            this.enterPositionPrice = enterPositionPrice;
            if (quantity != 0 && enterPositionPrice == 0) {
                throw new IllegalStateException(" enter position can not be 0 when exiting");
            }
        } else {
            this.enterPositionPrice = 0;
        }

        if (stopPrice == -1) {
            this.stopPrice = -1;
        } else if (stopPrice >= 100_000) {
            this.stopPrice = 100_000;
        } else if (stopPrice <= 0) {
            this.stopPrice = 0;
        } else {
            this.stopPrice = stopPrice;
        }

        orderCreationDelay = triggeringEvent != null ?
                             Duration.between(triggeringEvent.getTimestamp(), recordedTimestamp) :
                             Duration.ZERO;
    }

    @JsonIgnore
    Instant getRecordedTimestamp() {
        return recordedTimestamp;
    }

    public LocalDateTime getLocalisedCreatedTimestamp() {
        return Time.localise(recordedTimestamp);
    }

    public LocalDateTime getLocalisedLastFillTimestamp() {
        return lastFill.getFillTime() != Instant.EPOCH ? Time.localise(lastFill.getFillTime()) : null;
    }

    public Duration getRecordDelay() {
        return sentTimestamp != null ? Duration.between(sentTimestamp, recordedTimestamp) : Duration.ZERO;
    }

    public Duration getTotalDelay() {
        if (lastFill.getFillTime() == Instant.EPOCH) {
            return Duration.ZERO;
        } else if (triggeringEvent == null) {
            return getFillDelay();
        }
        return Duration.between(triggeringEvent.getTimestamp(), lastFill.getFillTime());
    }

    public Duration getFillDelay() {
        Instant startTimestamp = sentTimestamp != null ? sentTimestamp : recordedTimestamp;
        return (lastFill.getFillTime() == Instant.EPOCH) ? Duration.ZERO :
               Duration.between(startTimestamp, lastFill.getFillTime());
    }

    public double getOrderedQuantity() {
        return orderedQuantity;
    }

    public PositionAction getPositionAction() {
        return positionAction;
    }

    boolean fillAll(Instant timestamp, double averagePrice) {
        return fill(timestamp, orderedQuantity, averagePrice);
    }

    boolean fill(Instant timestamp, double filledQuantity, double averagePrice) {
        Validate.isTrue(averagePrice != 0, "Cannot fill with a price of 0");
        Validate.isTrue(filledQuantity >= 0, "Filled quantity (%s) must be > 0", filledQuantity);
        Validate.isTrue(filledQuantity <= getRemainingQuantity(),
                        "Cannot fill more than remaining. Order ID: %s, Filled Quantity: %s, Remaining Quantity: %s",
                        getUniqueOrderId(),
                        filledQuantity,
                        getRemainingQuantity());

        Fill fill = new Fill(filledQuantity, averagePrice, timestamp);
        this.filledQuantity += filledQuantity;
        this.lastFill = fill;
        fills.push(fill);
        if (positionAction == PositionAction.ENTER) {
            enterPositionPrice = calculateAverageFillPrice();
        } else if (positionAction == PositionAction.EXIT) {
            exitPositionPrice = calculateAverageFillPrice();

            orderPnl = calculatePnl();
            if (side == BidAsk.BID) {
                orderPnl = orderPnl * -1;
            }
        }

        double remaining = getRemainingQuantity();

        if (remaining > 0) {
            LOGGER.trace("processed a partial fill");
        } else if (remaining == 0) {
            LOGGER.trace("processed a full fill");
        }

        return true;
    }

    private double calculateAverageFillPrice() {
        double totalQuantity = 0, totalPrice = 0;

        for (Fill fill : fills) {
            totalQuantity += fill.getQuantity();
            totalPrice += fill.getAveragePrice() * fill.getQuantity();
        }

        return totalPrice / totalQuantity;
    }

    public double getRemainingQuantity() {
        return orderedQuantity - filledQuantity;
    }

    public String getUniqueOrderId() {
        if (getSide() == BidAsk.BID) {
            return getId();
        } else if (getSide() == BidAsk.ASK) {
            return "-".concat(getId());
        } else {
            return "0";
        }
    }

    public BidAsk getSide() {
        return side;
    }

    @JsonIgnore
    public String getId() {
        return id;
    }

    private double calculatePnl() {
        double result = 0;
        for (Fill fill : fills) {
            result += (fill.getAveragePrice() - enterPositionPrice) * (fill.getQuantity() * contractMultiplier);
        }

        return result;
    }

    boolean isFilled() {
        return getRemainingQuantity() == 0;
    }

    public double getSlippage() {
        return fills.stream()
                    .mapToDouble(Fill::getAveragePrice)
                    .map(filledPrice -> filledPrice - orderedPrice)
                    .sum();
    }

    public double getAverageFillPrice() {
        return fills.stream()
                    .collect(averagingDouble(Fill::getAveragePrice));
    }

    public double getOrderedPrice() {
        return orderedPrice;
    }

    public Type getType() {
        return type;
    }

    public Event getTriggeringEvent() {
        return triggeringEvent;
    }

    public double getEnterPositionPrice() {
        return enterPositionPrice;
    }

    public double getExitPositionPrice() {
        return exitPositionPrice;
    }

    public double getFilledQuantity() {
        return filledQuantity;
    }

    public double getExpectedOrderValue() {
        return orderedPrice * orderedQuantity;
    }

    public double getFilledOrderValue() {
        double result = 0;
        for (Fill fill : fills) {
            result += fill.getValue();
        }
        return result;
    }

    public Duration getOrderCreationDelay() {
        return orderCreationDelay;
    }

    public double getOrderPnl() {
        return orderPnl;
    }

    public double getStopPrice() {
        return stopPrice;
    }

    public boolean isPending() {
        Status status = getStatus();
        return status != Status.FILLED && status != Status.CANCELLED;
    }

    public Status getStatus() {
        if (getRemainingQuantity() == 0) {
            return Status.FILLED;
        } else if (filledQuantity != 0 && orderedQuantity != filledQuantity) {
            return Status.PARTIAL_FILL;
        } else {
            return Status.OPEN;
        }
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }

    public enum Type {MARKET_ORDER, LIMIT_ORDER, UNKNOWN_ORDER_TYPE}

    public enum Status {OPEN, FILLED, PARTIAL_FILL, CANCELLED, OTHER}

    public enum PositionAction {ENTER, EXIT, NEITHER}
}

