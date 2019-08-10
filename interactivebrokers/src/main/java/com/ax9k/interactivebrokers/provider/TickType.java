package com.ax9k.interactivebrokers.provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum TickType {

    BID_SIZE, BID_PRICE, ASK_SIZE, ASK_PRICE, LAST_PRICE, LAST_SIZE, HIGH, LOW, VOLUME, LAST_TIME_STAMP, CLOSE_PRICE,
    OPEN_PRICE, INVALID_TICK_TYPE, HALTED;
    //TODO why cant i put the int number like in BIDASK ?.
    // LAST_PRICE(4), LAST_SIZE (5), LAST_TIME_STAMP(45);

    static final Logger LOGGER = LogManager.getLogger();

    public static TickType fromCode(int code) {
        switch (code) {
            case 0:
                return BID_SIZE;
            case 1:
                return BID_PRICE;
            case 2:
                return ASK_SIZE;
            case 3:
                return ASK_PRICE;
            case 4:
                return LAST_PRICE;
            case 5:
                return LAST_SIZE;
            case 6:
                return HIGH;
            case 7:
                return LOW;
            case 8:
                return VOLUME;
            case 9:
                return CLOSE_PRICE;
            case 14:
                return OPEN_PRICE;
            case 45:
                return LAST_TIME_STAMP;
            case 49:
                return HALTED;
            default:
                LOGGER.error("unhandled tick type {}", code);
                return INVALID_TICK_TYPE;
        }
    }
}
