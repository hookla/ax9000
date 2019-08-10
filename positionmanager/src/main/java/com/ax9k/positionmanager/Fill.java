package com.ax9k.positionmanager;

import java.time.Instant;

final class Fill {
    final static Fill EMPTY = new Fill(0, 0, Instant.EPOCH);

    private final double quantity;
    private final double averagePrice;
    private final Instant fillTime;

    Fill(double quantity, double averagePrice, Instant fillTime) {
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.fillTime = fillTime;
    }

    double getQuantity() {
        return quantity;
    }

    double getAveragePrice() {
        return averagePrice;
    }

    double getValue() {
        return quantity * averagePrice;
    }

    Instant getFillTime() {return fillTime;}
}
