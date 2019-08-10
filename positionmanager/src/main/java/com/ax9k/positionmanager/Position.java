package com.ax9k.positionmanager;

import com.ax9k.broker.OrderRecord;
import com.ax9k.core.event.Event;
import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.time.Time;
import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ax9k.positionmanager.Order.PositionAction.ENTER;
import static com.ax9k.positionmanager.Order.PositionAction.EXIT;
import static java.lang.Math.abs;

public class Position {

    static final List<Order> ALREADY_FILLED = List.of();
    static final List<Order> FILL_ERROR = List.of(Order.ERROR);

    private static final Logger ERROR_LOG = LogManager.getLogger("error");
    private static final Logger OUR_TRADE_LOGGER = LogManager.getLogger("ourTradeLogger");

    private final Map<String, List<Order>> orders = new HashMap<>(1000);

    private Order latestOrder;
    private double contractPosition;
    private double enterPositionPrice;
    private boolean positionInitialised;

    List<Order> fillOrder(Instant fillTimestamp, int orderId, double avgFillPrice, double quantity) {
        List<Order> toFill = orders.get(String.valueOf(orderId));
        if (toFill == null) {
            ERROR_LOG.error("Order ID not found: {}", orderId);
            return FILL_ERROR;
        } else if (totalRemainingQuantity(toFill) == 0) {
            ERROR_LOG.error("Order {} is already filled.", orderId);
            return ALREADY_FILLED;
        } else if (anyCancelled(toFill)) {
            ERROR_LOG.warn("Filling a cancelled order: {}", toFill);
        }

        boolean successfulFill = fill(toFill, orderId, fillTimestamp, avgFillPrice, quantity);

        if (successfulFill) {
            if (isExitAndEnterTrade(toFill)) {
                enterPositionPrice = toFill.get(1).getAverageFillPrice();
            } else if (toFill.get(0).getPositionAction() == ENTER) {
                enterPositionPrice = toFill.get(0).getAverageFillPrice();
            }
            toFill.forEach(OUR_TRADE_LOGGER::info);
        } else {
            return FILL_ERROR;
        }
        return toFill;
    }

    private static boolean isExitAndEnterTrade(List<Order> orders) {
        return orders.size() > 1;
    }

    private static double totalRemainingQuantity(List<Order> orders) {
        return orders.stream().mapToDouble(Order::getRemainingQuantity).sum();
    }

    private static boolean anyCancelled(List<Order> orders) {
        return orders.stream().map(Order::getStatus).anyMatch(status -> status == Order.Status.CANCELLED);
    }

    private boolean fill(List<Order> orders, int baseId, Instant timestamp, double averageFillPrice, double quantity) {
        Validate.notEmpty(orders, "No orders to fill. ID: %s", baseId);

        if (quantity == -1) {
            for (Order order : orders) {
                order.fillAll(timestamp, averageFillPrice);
            }
            return true;
        }

        double totalOrderedQuantity = orders.stream().mapToDouble(Order::getOrderedQuantity).sum();
        Validate.validState(totalOrderedQuantity <= quantity,
                            "Cannot fill more than ordered. Order ID: %s, Filled Quantity: %s, Ordered Quantity: %s",
                            orders.get(0).getUniqueOrderId(),
                            quantity,
                            totalOrderedQuantity);

        double remaining = quantity;
        for (Order order : orders) {
            double ordered = order.getOrderedQuantity();
            if (remaining <= ordered) {
                order.fill(timestamp, remaining, averageFillPrice);
                return true;
            } else {
                order.fill(timestamp, ordered, averageFillPrice);
                remaining -= ordered;
            }
        }
        return true;
    }

    Order cancelOrder(int orderId) {
        String id = String.valueOf(orderId);
        List<Order> toCancel = orders.get(id);
        if (toCancel == null) {
            ERROR_LOG.error("Cannot cancel order. ID not found: {}", orderId);
        } else if (toCancel.isEmpty()) {
            ERROR_LOG.error("Cannot cancel order. No orders under ID: {}", orderId);
        } else {
            orders.remove(id);
            return toCancel.get(0);
        }
        return null;
    }

    public Order initialisePosition(double position, double entryPrice, int contractMultiplier) {
        Order initialOrder = makeInitialOrder(position, entryPrice, contractMultiplier);
        this.enterPositionPrice = entryPrice;
        orders.put(initialOrder.getId(), List.of(initialOrder));
        positionInitialised = true;

        OUR_TRADE_LOGGER.info(initialOrder);

        return initialOrder;
    }

    private Order makeInitialOrder(double position, double entryPrice, int contractMultiplier) {

        Order initialOrder = new Order("EXISTING_POSITION_ORDER",
                                       Time.now(),
                                       null,
                                       null,
                                       "0",
                                       entryPrice,
                                       abs(position),
                                       (position > 0) ? BidAsk.BID : BidAsk.ASK,
                                       Order.Type.MARKET_ORDER,
                                       (position > 0) ? Order.PositionAction.ENTER : Order.PositionAction.NEITHER,
                                       contractMultiplier,
                                       entryPrice,
                                       -1);

        if (position != 0) {
            initialOrder.fill(Time.now(), Math.abs(position), entryPrice);
        }

        latestOrder = initialOrder;
        return initialOrder;
    }

    public boolean getPositionInitialised() {
        return positionInitialised;
    }

    void addSellOrder(OrderRecord record,
                      String source,
                      Event triggeringEvent,
                      double quantity,
                      double price,
                      double stopPrice,
                      int contractMultiplier) {
        List<Order> newOrders;

        String id = String.valueOf(record.getId());
        if (sellingIntoNewPosition(quantity)) {
            double remaining = quantity - contractPosition;
            Order exitOrder = makeSellMarketOrder(source,
                                                  id.concat(".1"),
                                                  record.getTimestamp(),
                                                  triggeringEvent,
                                                  EXIT,
                                                  price,
                                                  contractPosition,
                                                  contractMultiplier,
                                                  stopPrice);
            Order newPositionOrder = makeSellMarketOrder(source,
                                                         id.concat(".2"),
                                                         record.getTimestamp(),
                                                         triggeringEvent,
                                                         ENTER,
                                                         price,
                                                         remaining,
                                                         contractMultiplier,
                                                         stopPrice);
            newOrders = List.of(exitOrder, newPositionOrder);
        } else {
            Order order = makeSellMarketOrder(source,
                                              id,
                                              record.getTimestamp(),
                                              triggeringEvent,
                                              determinePositionAction(-quantity),
                                              price,
                                              quantity,
                                              contractMultiplier,
                                              stopPrice);

            newOrders = List.of(order);
        }

        orders.put(id, newOrders);
    }

    private Order makeSellMarketOrder(String source,
                                      String id,
                                      Instant sentTimestamp,
                                      Event triggeringEvent,
                                      Order.PositionAction positionAction,
                                      double price,
                                      double quantity,
                                      double contractMultiplier,
                                      double stopPrice) {
        return latestOrder = new Order(
                source,
                Time.now(),
                sentTimestamp,
                triggeringEvent,
                id,
                price,
                quantity,
                BidAsk.ASK,
                Order.Type.MARKET_ORDER,
                positionAction,
                contractMultiplier,
                enterPositionPrice,
                stopPrice
        );
    }

    private boolean sellingIntoNewPosition(double quantity) {
        return contractPosition > 0 && quantity > contractPosition;
    }

    private Order.PositionAction determinePositionAction(double quantity) {
        if (contractPosition + quantity == 0) {
            return EXIT;
        } else {
            return ENTER;
        }
    }

    void addBuyOrder(OrderRecord record,
                     String source,
                     Event triggeringEvent,
                     double quantity,
                     double price,
                     double stopPrice,
                     int contractMultiplier) {
        List<Order> newOrders;

        String id = String.valueOf(record.getId());
        if (buyingIntoNewPosition(quantity)) {
            double quantityForExit = abs(contractPosition);
            double remaining = quantity - quantityForExit;
            Order exitOrder = makeBuyMarketOrder(source, id.concat(".1"),
                                                 record.getTimestamp(),
                                                 triggeringEvent, EXIT, price, quantityForExit,
                                                 contractMultiplier, stopPrice);
            Order newPositionOrder = makeBuyMarketOrder(source,
                                                        id.concat(".2"),
                                                        record.getTimestamp(),
                                                        triggeringEvent,
                                                        ENTER,
                                                        price,
                                                        remaining,
                                                        contractMultiplier,
                                                        stopPrice);
            newOrders = List.of(exitOrder, newPositionOrder);
        } else {
            Order order = makeBuyMarketOrder(source,
                                             id,
                                             record.getTimestamp(),
                                             triggeringEvent,
                                             determinePositionAction(quantity),
                                             price,
                                             quantity,
                                             contractMultiplier,
                                             stopPrice);

            newOrders = List.of(order);
        }

        orders.put(id, newOrders);
    }

    private boolean buyingIntoNewPosition(double quantity) {
        return contractPosition < 0 && quantity > abs(contractPosition);
    }

    private Order makeBuyMarketOrder(String source,
                                     String id,
                                     Instant sentTimestamp,
                                     Event triggeringEvent,
                                     Order.PositionAction positionAction,
                                     double price,
                                     double quantity,
                                     double contractMultiplier,
                                     double stopPrice) {
        return latestOrder = new Order(
                source,
                Time.now(),
                sentTimestamp,
                triggeringEvent,
                id,
                price,
                quantity,
                BidAsk.BID,
                Order.Type.MARKET_ORDER,
                positionAction,
                contractMultiplier,
                enterPositionPrice,
                stopPrice
        );
    }

    long getTradeCount(Duration period) {
        Instant earliestTimeStamp = Time.now().minus(period);
        return orders
                .values()
                .stream()
                .map(list -> list.get(0))
                .map(Order::getRecordedTimestamp)
                .filter(timestamp -> timestamp.isAfter(earliestTimeStamp))
                .count();
    }

    @JsonIgnore
    public String getOrdersJson() {
        return JsonUtils.toPrettyJsonString(orders);
    }

    public double getUnrealisedPnL(double bid0, double ask0, int contractMultiplier) {
        return (getCurrentValue(bid0, ask0) - getValueAtEntry()) * contractMultiplier;
    }

    private double getCurrentValue(double bid0, double ask0) {
        if (isShort()) {
            return ask0 * getContractPosition();
        } else if (isLong()) {
            return bid0 * getContractPosition();
        } else {
            return 0;
        }
    }

    public double getContractPosition() {
        return contractPosition;
    }

    public void setContractPosition(double quantity) {
        contractPosition = quantity;
    }

    public boolean isLong() {
        return contractPosition > 0;
    }

    public boolean isShort() {
        return contractPosition < 0;
    }

    public double getValueAtEntry() {
        return getContractPosition() * getEnterPositionPrice();
    }

    public double getEnterPositionPrice() {
        return enterPositionPrice;
    }

    public boolean hasPendingOrders() {
        if (latestOrder == null) {
            return false;
        }
        return latestOrder.isPending();
    }

    public boolean isLongOrNoPosition() {
        return isLong() || noPosition();
    }

    public boolean noPosition() {
        return !hasPosition();
    }

    public boolean hasPosition() {
        return getContractPosition() != 0;
    }

    public boolean isShortOrNoPosition() {
        return isShort() || noPosition();
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }
}
