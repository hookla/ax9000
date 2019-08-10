package com.ax9k.core.event;

public enum EventType {
    ORDER_EVENT,
    ADD_ORDER,
    MODIFY_ORDER,
    DELETE_ORDER,
    ORDER_BOOK_CLEAR,
    TRADE,
    CALCULATE_OPENING_PRICE,
    UNKNOWN,

    HEARTBEAT,
    OHLCV_BAR
}
