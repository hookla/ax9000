package com.ax9k.interactivebrokers.provider;

import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.spy;

class TransactionMessageProcessorTest {
    private static final int FOR_BID = Integer.MAX_VALUE;
    private static final int FOR_ASK = Integer.MIN_VALUE;

    private EventHolder result = new EventHolder();
    private TransactionMessageProcessor testProcessor = spy(new TransactionMessageProcessor(result));

    @BeforeEach
    void reset() {
        result = new EventHolder();
        testProcessor = spy(new TransactionMessageProcessor(result));
    }

    private void processFullTradeEvent(Trade trade) {
        processFullTradeEvent(
                trade.getTimestamp(),
                trade.getPrice(),
                trade.getQuantity(),
                trade.getSide()
        );
    }

    private void processFullTradeEvent(Instant timestamp, double price, double quantity, BidAsk side) {
        processTimestamp(timestamp);
        processPrice(price, side);
        processQuantity(quantity);
    }

    private void processTimestamp(Instant timestamp) {
        testProcessor.process(TickType.LAST_TIME_STAMP, timestamp.getEpochSecond(), 0);
    }

    private void processPrice(double price, BidAsk side) {
        int mid = side == BidAsk.BID ? FOR_BID : FOR_ASK;
        testProcessor.process(TickType.LAST_PRICE, price, mid);
    }

    private void processQuantity(double quantity) {
        testProcessor.process(TickType.LAST_SIZE, quantity, 0);
    }

    private static class EventHolder implements TradeEventConsumer {
        private final Deque<Trade> held = new ArrayDeque<>();

        Trade retrieve() {
            return held.pollFirst();
        }

        int size() {
            return held.size();
        }

        List<Trade> retrieveAll() {
            List<Trade> result = List.copyOf(held);
            held.clear();
            return result;
        }

        @Override
        public void send(Trade trade) {
            hold(trade);
        }

        void hold(Trade trade) {
            held.addLast(trade);
        }
    }

    @Nested
    class WhenProcessingSingleFullTradeEvent {
        private final Trade original = new Trade(
                Instant.ofEpochSecond(5), 1234, 567, -999, BidAsk.BID
        );

        private Trade sent;

        @BeforeEach
        void processSingleFullTradeEvent() {
            processFullTradeEvent(original);
            sent = result.retrieve();
        }

        @Test
        void shouldSentEventOnce() {
            assertNotNull(sent);
        }

        @Test
        void shouldMaintainGivenTradePrice() {
            assertPropertiesEqual(Trade::getPrice);
        }

        private void assertPropertiesEqual(Function<Trade, ?> getter) {
            assertEquals(getter.apply(original), getter.apply(sent));
        }

        @Test
        void shouldMaintainGivenTradeQuantity() {
            assertPropertiesEqual(Trade::getQuantity);
        }

        @Test
        void shouldMaintainGivenTradeTimestamp() {
            assertPropertiesEqual(Trade::getTimestamp);
        }

        @Test
        void shouldCorrectlyGleanTradeSide() {
            assertPropertiesEqual(Trade::getSide);
        }
    }

    @Nested
    class WhenProcessingSinglePartialTradeEvent {
        private final Instant timestamp = Instant.ofEpochSecond(190);
        private final BidAsk side = BidAsk.ASK;

        @Test
        void shouldNotSendEventAfterProcessingOnlyTimestampData() {
            processTimestamp(timestamp);
            assertNoEvents();
        }

        void assertNoEvents() {
            assertNull(result.retrieve());
        }

        @Test
        void shouldNotSendEventAfterProcessingOnlyPriceData() {
            double price = 654;
            processPrice(price, side);
            assertNoEvents();
        }

        @Test
        void shouldNotSentEventAfterProcessingOnlyQuantityData() {
            double quantity = 321;
            processQuantity(quantity);
            assertNoEvents();
        }
    }

    @Nested
    class WhenProcessingMultipleFullTradeEvents {
        private final List<Trade> original;

        private List<Trade> sent;

        WhenProcessingMultipleFullTradeEvents() {
            original = List.of(
                    new Trade(Instant.ofEpochSecond(0xa), 12, 45, -999, BidAsk.BID),
                    new Trade(Instant.ofEpochSecond(0xb), 23, 56, -999, BidAsk.ASK),
                    new Trade(Instant.ofEpochSecond(0xc), 78, 910, -999, BidAsk.BID),
                    new Trade(Instant.ofEpochSecond(0xd), 1112, 1314, -999, BidAsk.BID),
                    new Trade(Instant.ofEpochSecond(0xe), 15, 16, -999, BidAsk.ASK)
            );
        }

        @BeforeEach
        void processMultipleFullTradeEvents() {
            for (Trade trade : original) {
                processFullTradeEvent(trade);
            }

            sent = result.retrieveAll();
        }

        @Test
        void shouldSentEventForEachTrade() {
            assertEquals(original.size(), sent.size());
        }

        @Test
        void shouldMaintainGivenTradePrices() {
            assertAllValuesEqual(Trade::getPrice);
        }

        private void assertAllValuesEqual(Function<Trade, ?> getter) {
            for (int i = 0; i < original.size(); i++) {
                Object originalProperty = getter.apply(original.get(i));
                Object sentProperty = getter.apply(sent.get(i));
                assertEquals(originalProperty, sentProperty);
            }
        }

        @Test
        void shouldMaintainGivenTradeQuantitys() {
            assertAllValuesEqual(Trade::getQuantity);
        }

        @Test
        void shouldMaintainGivenTradeTimestamps() {
            assertAllValuesEqual(Trade::getTimestamp);
        }

        @Test
        void shouldCorrectlyGleanAllTradeSides() {
            assertAllValuesEqual(Trade::getSide);
        }
    }
}