package com.ax9k.core.marketmodel.orderbook;

import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("orderBookLevel")
public final class OrderBookLevel implements Comparable<OrderBookLevel> {
    public static final OrderBookLevel EMPTY = new OrderBookLevel(0.0, 0.0);
    private final double price;
    private final double quantity;

    public OrderBookLevel(double price, double quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public OrderBookLevel setPrice(double price) {
        return new OrderBookLevel(price, quantity);
    }

    public double getQuantity() {
        return quantity;
    }

    public OrderBookLevel setQuantity(double quantity) {
        return new OrderBookLevel(price, quantity);
    }

    @JsonIgnore
    public double getValue() {
        return quantity * price;
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(price) + Double.hashCode(quantity);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return compareTo((OrderBookLevel) other) == 0;
    }

    @Override
    public int compareTo(OrderBookLevel that) {
        int result = Double.compare(price, that.price);
        if (result == 0) {
            result = Double.compare(quantity, that.quantity);
        }
        return result;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }
}