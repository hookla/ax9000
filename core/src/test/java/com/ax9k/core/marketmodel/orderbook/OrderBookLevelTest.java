package com.ax9k.core.marketmodel.orderbook;

import com.ax9k.util.EqualsAndHashCodeSpecification;
import org.junit.jupiter.api.Nested;

class OrderBookLevelTest {
    @Nested
    class WhenEqualsAndHashCodeMethodsCalled implements EqualsAndHashCodeSpecification<OrderBookLevel> {
        @Override
        public OrderBookLevel getSecondEqualValue() {
            return new OrderBookLevel(1, 2.0);
        }

        @Override
        public OrderBookLevel getValue() {
            return new OrderBookLevel(1, 2.0);
        }

        @Override
        public OrderBookLevel getEqualValue() {
            return new OrderBookLevel(1, 2.0);
        }

        @Override
        public OrderBookLevel getUnequalValue() {
            return new OrderBookLevel(2.0, 3.0);
        }
    }
}