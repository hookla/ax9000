package com.ax9k.cex.provider.orderbook;

import com.ax9k.core.marketmodel.orderbook.OrderBookLevel;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Comparator;

import static org.apache.commons.lang3.Validate.notNull;

class OrderBookLevels {
    private final Comparator<OrderBookLevel> order;
    private final int depth;
    private OrderBookLevel[] levels;

    OrderBookLevels(Comparator<OrderBookLevel> order, int depth) {
        Validate.isTrue(depth > 0, "depth must be positive");
        this.order = notNull(order);
        this.depth = depth;
        levels = new OrderBookLevel[depth * 2];
        Arrays.fill(levels, OrderBookLevel.EMPTY);
    }

    void add(OrderBookLevel newLevel) {
        if (Double.compare(newLevel.getQuantity(), 0.0d) == 0) {
            remove(newLevel.getPrice());
            return;
        }

        for (int i = 0; i < levels.length; i++) {
            if (newLevel.getPrice() == levels[i].getPrice()) {
                levels[i] = newLevel;
                return;
            }
            if (order.compare(newLevel, levels[i]) <= 0) {
                createSpace(levels, i);
                levels[i] = newLevel;
                return;
            }
        }
    }

    private void remove(double price) {
        for (int i = 0; i < levels.length; i++) {
            OrderBookLevel level = levels[i];
            if (Double.compare(level.getPrice(), price) == 0) {
                shiftOver(levels, i);
                return;
            }
        }
    }

    private static void shiftOver(OrderBookLevel[] levels, int index) {
        int i = index;
        for (; i < levels.length - 1; i++) {
            levels[i] = levels[i + 1];
        }
        levels[i] = OrderBookLevel.EMPTY;
    }

    private static void createSpace(OrderBookLevel[] levels, int spaceIndex) {
        int i = levels.length - 1;
        for (; i >= spaceIndex + 1; i--) {
            levels[i] = levels[i - 1];
        }
        levels[i] = OrderBookLevel.EMPTY;
    }

    void set(OrderBookLevel[] newLevels) {
        levels = new OrderBookLevel[depth * 2];
        for (int i = 0; i < levels.length; i++) {
            if (i >= newLevels.length || newLevels[i] == null) {
                levels[i] = OrderBookLevel.EMPTY;
            } else {
                levels[i] = newLevels[i];
            }
        }
        Arrays.sort(newLevels, order);
    }

    OrderBookLevel[] toArray() {
        OrderBookLevel[] relevantLevels = new OrderBookLevel[depth];
        System.arraycopy(levels, 0, relevantLevels, 0, depth);
        return relevantLevels;
    }
}