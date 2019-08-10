package com.ax9k.broker;

import com.ax9k.core.marketmodel.BidAsk;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

import static java.lang.Double.isFinite;
import static java.lang.Double.isNaN;

public final class OrderRequest {
    private final double price, quantity;
    private final BidAsk side;

    private OrderRequest(double price, double quantity, BidAsk side) {
        Validate.isTrue(isFinite(price) && !isNaN(price) && price != 0.0d, "invalid price: %s", price);
        Validate.isTrue(isFinite(quantity) && !isNaN(quantity) && price != 0.0d, "invalid quantity: %s", quantity);
        Validate.isTrue(side == BidAsk.BID || side == BidAsk.ASK, "side can only be buy or sell: %s", side);

        this.price = price;
        this.quantity = quantity;
        this.side = side;
    }

    public static OrderRequest of(double price, double quantity, BidAsk side) {
        return new OrderRequest(price, quantity, side);
    }

    public double getPrice() {
        return price;
    }

    public double getQuantity() {
        return quantity;
    }

    public BidAsk getSide() {
        return side;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        OrderRequest that = (OrderRequest) o;
        return Double.compare(that.price, price) == 0 &&
               Double.compare(that.quantity, quantity) == 0 &&
               side == that.side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(price, quantity, side);
    }

    @Override
    public String toString() {
        return "OrderRequest{" +
               "price=" + price +
               ", quantity=" + quantity +
               ", side=" + side +
               '}';
    }
}
