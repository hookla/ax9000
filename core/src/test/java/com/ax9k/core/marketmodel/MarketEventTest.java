package com.ax9k.core.marketmodel;

import com.ax9k.core.event.EventType;
import com.ax9k.util.EqualsAndHashCodeSpecification;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static com.ax9k.core.event.EventType.ADD_ORDER;
import static com.ax9k.core.marketmodel.BidAsk.ASK;
import static com.ax9k.core.marketmodel.BidAsk.BID;
import static com.ax9k.core.marketmodel.BidAsk.NONE;
import static com.ax9k.core.marketmodel.MarketEvent.Type.UNKNOWN_ORDER_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketEventTest {
    private static final Instant TIMESTAMP = ZonedDateTime.of(LocalDate.of(2016, Month.NOVEMBER, 9),
                                                              LocalTime.of(5,
                                                                     6,
                                                                     30,
                                                                     653_000),
                                                              ZoneOffset.ofHours(8)).toInstant();

    private static final MarketEvent ADD_ASK = new MarketEvent(TIMESTAMP,
                                                               ADD_ORDER,
                                                               123,
                                                               10,
                                                               1,
                                                               ASK,
                                                               UNKNOWN_ORDER_TYPE);

    private static final MarketEvent ADD_BID = new MarketEvent(TIMESTAMP,
                                                               ADD_ORDER,
                                                               124,
                                                               10,
                                                               1,
                                                               BID,
                                                               UNKNOWN_ORDER_TYPE);

    private static final MarketEvent TRADE = new MarketEvent(TIMESTAMP,
                                                             EventType.TRADE,
                                                             123,
                                                             10,
                                                             1,
                                                             NONE,
                                                             UNKNOWN_ORDER_TYPE);

    private static final MarketEvent DELETE_ORDER = new MarketEvent(TIMESTAMP,
                                                                    EventType.DELETE_ORDER,
                                                                    124,
                                                                    10,
                                                                    1,
                                                                    NONE,
                                                                    UNKNOWN_ORDER_TYPE);

    private static MarketEvent testEvent;

    @Test
    void testExchangeMessageAddAskOrder() {
        testEvent = copy(ADD_ASK);
        assertDoublesEqual(10, testEvent.getPrice());
        assertDoublesEqual(1, testEvent.getOrderQuantity());
        assertSame(testEvent.getSide(), ASK);
    }

    private static MarketEvent copy(MarketEvent event) {
        return new MarketEvent(event.getEventTimestamp(),
                               event.getMessageType(),
                               event.getOrderId(),
                               event.getPrice(),
                               event.getOrderQuantity(),
                               event.getSide(),
                               event.getOrderType());
    }

    private static void assertDoublesEqual(double expected, double actual) {
        String message = String.format("expected <%s> but was <%s>", expected, actual);
        assertEquals(0, Double.compare(expected, actual), message);
    }

    @Test
    void testExchangeMessageAddBidOrder() {
        testEvent = copy(ADD_BID);
        assertTrue(testEvent.getPrice() > 0);
        assertTrue(testEvent.getOrderQuantity() > 0);
        assertSame(testEvent.getSide(), BID);
    }

    @Test
    void testExchangeMessageDeleteOrder() {
        testEvent = copy(DELETE_ORDER);
        assertTrue(testEvent.getOrderId() > 0);
    }

    @Test
    void testExchangeMessageTrade() {
        testEvent = copy(TRADE);
        assertEquals(NONE, testEvent.getSide());
        assertTrue(testEvent.getPrice() > 0);
        assertTrue(testEvent.getOrderQuantity() > 0);
    }

    @Test
    void testGetUniqueOrderId() {
        testEvent = copy(TRADE);
        assertEquals(0, testEvent.getUniqueOrderId());

        testEvent = copy(ADD_BID);
        assertEquals(testEvent.getOrderId(), testEvent.getUniqueOrderId());

        testEvent = copy(ADD_ASK);
        assertEquals(testEvent.getOrderId() * -1, testEvent.getUniqueOrderId());
    }

    @Nested
    class WhenEqualsAndHashCodeMethodsCalled implements EqualsAndHashCodeSpecification<MarketEvent> {
        @Override
        public MarketEvent getSecondEqualValue() {
            return copy(ADD_ASK);
        }

        @Override
        public MarketEvent getValue() {
            return copy(ADD_ASK);
        }

        @Override
        public MarketEvent getEqualValue() {
            return copy(ADD_ASK);
        }

        @Override
        public MarketEvent getUnequalValue() {
            return copy(ADD_BID);
        }
    }
}
