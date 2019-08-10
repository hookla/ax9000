package com.ax9k.cex.provider.orderbook;

import com.ax9k.core.marketmodel.orderbook.OrderBookLevel;
import org.apache.commons.lang3.Validate;

import java.util.Comparator;

import static com.ax9k.core.marketmodel.orderbook.OrderBookLevel.EMPTY;
import static java.util.Comparator.comparingDouble;

final class BookLevelComparators {
    static final Comparator<OrderBookLevel> ASKS = new EmptyElementsLastComparator(
            comparingDouble(OrderBookLevel::getPrice));
    static final Comparator<OrderBookLevel> BIDS = new EmptyElementsLastComparator(
            comparingDouble(OrderBookLevel::getPrice).reversed());

    private BookLevelComparators() {
        throw new AssertionError("not instantiable");
    }

    private static final class EmptyElementsLastComparator implements Comparator<OrderBookLevel> {
        private final Comparator<OrderBookLevel> base;

        private EmptyElementsLastComparator(Comparator<OrderBookLevel> base) {
            this.base = base;
        }

        @Override
        public int compare(OrderBookLevel a, OrderBookLevel b) {
            Validate.notNull(a);
            Validate.notNull(b);
            if (a.equals(EMPTY)) {
                return (b.equals(EMPTY)) ? 0 : 1;
            } else if (b.equals(EMPTY)) {
                return -1;
            } else {
                return base.compare(a, b);
            }
        }
    }
}
