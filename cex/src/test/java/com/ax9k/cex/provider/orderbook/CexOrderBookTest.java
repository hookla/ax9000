package com.ax9k.cex.provider.orderbook;

import com.ax9k.cex.client.AlwaysOpenSchedule;
import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import com.ax9k.core.marketmodel.orderbook.OrderBookLevel;
import com.ax9k.core.time.Time;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CexOrderBookTest {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private OrderBook testBook;

    @BeforeAll
    static void setTimeZone() {
        Time.setTradingSchedule(AlwaysOpenSchedule.INSTANCE);
    }

    @Nested
    class WhenGivenNoBidAndAskLevels {
        @BeforeEach
        void readInEmptyBook() throws IOException {
            String emptyOrderBookJson = "{\n" +
                                        "    \"timestamp\": 1435927929,\n" +
                                        "    \"bids\": [],\n" +
                                        "    \"asks\": [],\n" +
                                        "    \"pair\": \"BTC:USD\",\n" +
                                        "    \"id\": 67809\n" +
                                        "}";
            testBook = CexOrderBook.of(JSON_MAPPER.readTree(emptyOrderBookJson))
                                   .toImmutableBook(EventType.UNKNOWN);
        }

        @Test
        void shouldBeEmpty() {
            assertEquals(OrderBookLevel.EMPTY, testBook.getAsks()[0]);
            assertEquals(OrderBookLevel.EMPTY, testBook.getBids()[0]);
        }
    }

    @Nested
    class WhenGivenValidBookValues {
        private static final String SAMPLE_VALID_BOOK = "{\n" +
                                                        "    \"timestamp\": 1435927929,\n" +
                                                        "    \"bids\": [\n" +
                                                        "      [\n" +
                                                        "        241.947,\n" +
                                                        "        155.91626\n" +
                                                        "      ],\n" +
                                                        "      [\n" +
                                                        "        241,\n" +
                                                        "        981.1255\n" +
                                                        "      ]\n" +
                                                        "    ],\n" +
                                                        "    \"asks\": [\n" +
                                                        "      [\n" +
                                                        "        241.95,\n" +
                                                        "        15.4613\n" +
                                                        "      ],\n" +
                                                        "      [\n" +
                                                        "        241.99,\n" +
                                                        "        17.3303\n" +
                                                        "      ]\n" +
                                                        "    ],\n" +
                                                        "    \"pair\": \"BTC:USD\",\n" +
                                                        "    \"id\": 67809\n" +
                                                        "}";

        @BeforeEach
        void readInValidBook() throws IOException {
            testBook = CexOrderBook.of(JSON_MAPPER.readTree(SAMPLE_VALID_BOOK))
                                   .toImmutableBook(EventType.UNKNOWN);
        }

        @Test
        void shouldRecordTimestampCorrectly() {
            assertEquals(1435927929, testBook.getTimestamp().getEpochSecond());
        }

        @Test
        void shouldRecordBidLevelsCorrectly() {
            OrderBookLevel[] expectedBidLevels = {
                    new OrderBookLevel(241.947, 155.91626),
                    new OrderBookLevel(241, 981.1255),
                    OrderBookLevel.EMPTY,
                    OrderBookLevel.EMPTY,
                    OrderBookLevel.EMPTY
            };
            assertArrayEquals(expectedBidLevels, testBook.getBids());
        }

        @Test
        void shouldRecordAskLevelsCorrectly() {
            OrderBookLevel[] expectedAskLevels = {
                    new OrderBookLevel(241.95, 15.4613),
                    new OrderBookLevel(241.99, 17.3303),
                    OrderBookLevel.EMPTY,
                    OrderBookLevel.EMPTY,
                    OrderBookLevel.EMPTY
            };
            assertArrayEquals(expectedAskLevels, testBook.getAsks());
        }
    }
}