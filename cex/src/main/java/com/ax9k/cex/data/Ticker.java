package com.ax9k.cex.data;

import com.ax9k.core.event.Event;
import com.ax9k.core.time.Time;
import org.apache.commons.lang3.Validate;

import java.time.Instant;

public final class Ticker implements Event {
    private final double price;
    private final Instant timestamp;

    public Ticker(double price) {
        Validate.notNaN(price);
        Validate.finite(price);
        Validate.isTrue(price > 0, "price must be greater than zero");
        this.price = price;
        timestamp = Time.now();
    }

    public double getPrice() {
        return price;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        Ticker ticker = (Ticker) o;
        return Double.compare(ticker.price, price) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(price);
    }

    @Override
    public String toString() {
        return String.valueOf(price);
    }
}
