package com.ax9k.backtesting;

import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.MarketEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

import static com.ax9k.core.event.EventType.ADD_ORDER;
import static com.ax9k.core.event.EventType.CALCULATE_OPENING_PRICE;
import static com.ax9k.core.event.EventType.DELETE_ORDER;
import static com.ax9k.core.event.EventType.MODIFY_ORDER;
import static com.ax9k.core.event.EventType.ORDER_BOOK_CLEAR;
import static com.ax9k.core.event.EventType.ORDER_EVENT;
import static com.ax9k.core.event.EventType.TRADE;

public class MarketEventParser {
    private static final DateTimeFormatter FORMATTER =
            new DateTimeFormatterBuilder().appendPattern("yyyyMMddHHmmss")
                                          .appendValue(ChronoField.MILLI_OF_SECOND, 3).toFormatter();
    private static final ZoneOffset offset = ZoneOffset.of("+08:00");

    private MarketEventParser() {
        throw new AssertionError("not instantiable");
    }

    static MarketEvent parse(String line) {
        if (line == null || line.isEmpty()) {
            throw new IllegalArgumentException("null or empty market order string");
        }

        try {
            final String[] values = line.split(",", -1);
            if (values.length < 6) {
                throw new IllegalArgumentException("not enough fields in the input line: " + line);
            }
            Instant eventTimeStamp = parseDateTimeToEpoch(values[0]);

            EventType messageType = fromCode(Short.parseShort(values[1]));

            double price = values[4].length() > 0 ? Double.parseDouble(values[4]) : -999;
            int quantity = values[5].length() > 0 ? Integer.parseInt(values[5]) : -999;
            BidAsk side = values[6].length() > 0 ? BidAsk.fromCode(Short.parseShort(values[6])) : BidAsk.NONE;

            long orderId = 0;
            if (messageType == TRADE || messageType == ADD_ORDER || messageType == DELETE_ORDER) {
                orderId = Long.parseLong(values[3]);
            }
            return new MarketEvent(eventTimeStamp, messageType, orderId, price, quantity, side,
                                   MarketEvent.Type.UNKNOWN_ORDER_TYPE
            );
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new IllegalArgumentException("invalid market order string: ".concat(line));
        }
    }

    private static Instant parseDateTimeToEpoch(String s) {
        if (s.length() != 17) {
            throw new IllegalArgumentException("given string to parse that is not the right length: " + s);
        }
        return LocalDateTime.parse(s, FORMATTER)
                            .toInstant(offset);
    }

    private static EventType fromCode(short code) {
        switch (code) {
            case 3000:
                return ORDER_EVENT;
            case 330:
                return ADD_ORDER;
            case 331:
                return MODIFY_ORDER;
            case 332:
                return DELETE_ORDER;
            case 335:
                return ORDER_BOOK_CLEAR;
            case 350:
                return TRADE;
            case 364:
                return CALCULATE_OPENING_PRICE;
            default:
                throw new IllegalArgumentException("unsupported event type: " + code);
        }
    }
}
