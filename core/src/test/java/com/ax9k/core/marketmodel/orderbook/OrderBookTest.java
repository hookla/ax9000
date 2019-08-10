package com.ax9k.core.marketmodel.orderbook;

import com.ax9k.core.event.EventType;
import com.ax9k.util.EqualsAndHashCodeSpecification;
import org.junit.jupiter.api.Nested;

import java.time.Instant;

class OrderBookTest {
    @Nested
    class WhenEqualsAndHashCodeMethodsCalled implements EqualsAndHashCodeSpecification<OrderBook> {
        @Override
        public OrderBook getValue() {
            return new OrderBook(
                    Instant.EPOCH,
                    EventType.UNKNOWN,
                    new OrderBookLevel[] { new OrderBookLevel(2, 2) },
                    new OrderBookLevel[] { new OrderBookLevel(1, 1) }
            );
        }

        @Override
        public OrderBook getEqualValue() {
            return new OrderBook(
                    Instant.EPOCH,
                    EventType.UNKNOWN,
                    new OrderBookLevel[] { new OrderBookLevel(2, 2) },
                    new OrderBookLevel[] { new OrderBookLevel(1, 1) }
            );
        }

        @Override
        public OrderBook getUnequalValue() {
            return new OrderBook(
                    Instant.EPOCH,
                    EventType.UNKNOWN,
                    new OrderBookLevel[] { new OrderBookLevel(3, 4) },
                    new OrderBookLevel[] { new OrderBookLevel(2, 2) }
            );
        }

        @Override
        public OrderBook getSecondEqualValue() {
            return new OrderBook(
                    Instant.EPOCH,
                    EventType.UNKNOWN,
                    new OrderBookLevel[] { new OrderBookLevel(2, 2) },
                    new OrderBookLevel[] { new OrderBookLevel(1, 1) }
            );
        }
    }
}