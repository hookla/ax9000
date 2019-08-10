package com.ax9k.backtesting;

import java.util.OptionalInt;

class Counter {
    private int count;
    private final OptionalInt checkpoint;

    Counter() {
        this.checkpoint = OptionalInt.empty();
    }

    Counter(int checkpoint) {
        this.checkpoint = OptionalInt.of(checkpoint);
    }

    void increment() {
        count++;
    }

    int getCount() {
        return count;
    }

    boolean checkpointReached() {
        if (checkpoint.isPresent() && count >= checkpoint.getAsInt()) {
            count = 0;
            return true;
        }
        return false;
    }
}
