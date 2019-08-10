package com.ax9k.backtesting;

import java.util.Optional;

enum ProcessingMode {
    BOOK_STATES, TRADES, ALL;

    static ProcessingMode fromOptionValue(Optional<?> value) {
        return fromOptionValue(value.orElse(null));
    }

    static ProcessingMode fromOptionValue(Object value) {
        if (value == null) {
            return ProcessingMode.ALL;
        }
        String identifier = value.toString().toLowerCase();

        if (identifier.contains("book")) {
            return BOOK_STATES;
        } else if (identifier.contains("trade")) {
            return TRADES;
        } else {
            return ALL;
        }
    }
}
