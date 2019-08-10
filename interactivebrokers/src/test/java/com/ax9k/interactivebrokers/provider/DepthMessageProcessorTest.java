package com.ax9k.interactivebrokers.provider;

import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.BidAsk;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import com.ax9k.core.marketmodel.orderbook.OrderBookLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.verification.VerificationMode;

import java.time.Instant;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DepthMessageProcessorTest {
    private static final OrderBookLevel[] FILLED_ASKS, FILLED_BIDS;
    private static final VerificationMode NEVER = times(0);
    private static final VerificationMode ONCE = times(1);
    private static final Instant TIME_STAMP = Instant.now();

    static {
        FILLED_ASKS = new OrderBookLevel[] {
                new OrderBookLevel(5000, 5),
                new OrderBookLevel(5001, 5),
                new OrderBookLevel(5002, 5),
                new OrderBookLevel(5003, 5),
                new OrderBookLevel(5004, 5),
                };
        FILLED_BIDS = new OrderBookLevel[] {
                new OrderBookLevel(6004, 5),
                new OrderBookLevel(6003, 5),
                new OrderBookLevel(6002, 5),
                new OrderBookLevel(6001, 5),
                new OrderBookLevel(6000, 5),
                };
    }

    private DepthMessageProcessor mockProcessor;
    private LevelUpdateEventConsumer mockConsumer;
    private DepthMessageProcessor testProcessor;
    private IbOrderBook result;

    @BeforeEach
    void initialiseProcessor() {
        mockConsumer = mock(LevelUpdateEventConsumer.class);
        mockProcessor = new DepthMessageProcessor(mockConsumer, () -> {});

        result = new IbOrderBook(5);
        testProcessor = new DepthMessageProcessor(result::directBookUpdate, () -> {});
    }

    private void setAskLevel(int level, OrderBookLevel value) {
        testProcessor.processUpdate(Instant.EPOCH, level, BidAsk.ASK, value.getPrice(), 0);
        testProcessor.processUpdate(Instant.EPOCH, level, BidAsk.ASK, 0, value.getQuantity());
    }

    private void setBidLevel(int level, OrderBookLevel value) {
        testProcessor.processUpdate(Instant.EPOCH, level, BidAsk.BID, value.getPrice(), 0);
        testProcessor.processUpdate(Instant.EPOCH, level, BidAsk.BID, 0, value.getQuantity());
    }

    private void assertCallbackSentOnce(int level, BidAsk side, double price, double quantity) {
        assertCallbackSent(ONCE, level, side, price, quantity);
    }

    private void assertCallbackSent(VerificationMode times, int level, BidAsk side, double price, double quantity) {
        verify(mockConsumer, times).send(TIME_STAMP, level, side, price, (int) quantity);
    }

    private void assertCallbackNeverSent(int level, BidAsk side, double price, double quantity) {
        assertCallbackSent(NEVER, level, side, price, quantity);
    }

    @Nested
    class WhenProcessedSingleValidEvent {
        @Test
        void shouldProcessWithoutError() {
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.ASK, 0, 0);
        }

        @Test
        void shouldNotSendCallback() {
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.ASK, 30000, 3);
            assertCallbackNeverSent(0, BidAsk.ASK, 30000, 3);
        }
    }

    @Nested
    class WhenProcessingMultipleSimilarEvents {
        @Test
        void shouldProcessWithoutError() {
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.ASK, 30000, 3);
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.ASK, 30000, 3);
        }

        @Test
        void shouldNotSendCallbackAfterOneEvent() {
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.ASK, 30000, 3);
            assertCallbackNeverSent(0, BidAsk.ASK, 30000, 3);
        }

        @Test
        void shouldSendCallbackAfterTwoEvents() {
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.ASK, 30000, 3);
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.ASK, 30000, 3);

            assertCallbackSentOnce(0, BidAsk.ASK, 30000, 3);
        }
    }

    @Nested
    class WhenProcessingMultipleDissimilarEvents {
        @Test
        void shouldProcessWithoutError() {
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.ASK, 30000, 3);
            mockProcessor.processUpdate(TIME_STAMP, 1, BidAsk.ASK, 10000, 5);
            mockProcessor.processUpdate(TIME_STAMP, 1, BidAsk.BID, 15000, 5);
        }

        @Test
        void shouldSendCallbackAfterLevelsDiffer() {
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.ASK, 30000, 3);
            mockProcessor.processUpdate(TIME_STAMP, 1, BidAsk.ASK, 30000, 3);
            mockProcessor.processUpdate(TIME_STAMP, 2, BidAsk.ASK, 30000, 3);

            assertCallbackSentOnce(0, BidAsk.ASK, 30000, 3);
            assertCallbackSentOnce(1, BidAsk.ASK, 30000, 3);
        }

        @Test
        void shouldSendCallbackAfterSidesDiffer() {
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.ASK, 15000, 1);
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.BID, 15000, 1);
            mockProcessor.processUpdate(TIME_STAMP, 0, BidAsk.ASK, 15000, 1);

            assertCallbackSentOnce(0, BidAsk.ASK, 15000, 1);
            assertCallbackSentOnce(0, BidAsk.BID, 15000, 1);
        }

        @Test
        void shouldNotSendCallbackAfterPricesDiffer() {
            mockProcessor.processUpdate(TIME_STAMP, 2, BidAsk.ASK, 10000, 3);
            mockProcessor.processUpdate(TIME_STAMP, 2, BidAsk.ASK, 15000, 3);
            mockProcessor.processUpdate(TIME_STAMP, 2, BidAsk.ASK, 10000, 3);

            assertCallbackSentOnce(2, BidAsk.ASK, 15000, 3);
            assertCallbackNeverSent(2, BidAsk.ASK, 10000, 3);
        }

        @Test
        void shouldNotSendCallbackAfterQuantitiesDiffer() {
            mockProcessor.processUpdate(TIME_STAMP, 2, BidAsk.ASK, 17000, 3);
            mockProcessor.processUpdate(TIME_STAMP, 2, BidAsk.ASK, 17000, 7);
            mockProcessor.processUpdate(TIME_STAMP, 2, BidAsk.ASK, 17000, 3);

            assertCallbackSentOnce(2, BidAsk.ASK, 17000, 7);
            assertCallbackNeverSent(2, BidAsk.ASK, 17000, 3);
        }
    }

    @Nested
    class WhenOrderBookFilled {
        @BeforeEach
        void fillOrderBook() {
            for (int i = 0; i < FILLED_ASKS.length; i++) {
                setAskLevel(i, FILLED_ASKS[i]);
                setBidLevel(i, FILLED_BIDS[i]);
            }
        }

        @Test
        void shouldHaveNoEmptyLevels() {
            Predicate<OrderBookLevel> emptyLevel = level -> level.equals(OrderBookLevel.EMPTY);
            OrderBook book = result.toImmutableOrderBook(EventType.UNKNOWN);
            Stream<OrderBookLevel> asks = Stream.of(book.getAsks());
            Stream<OrderBookLevel> bids = Stream.of(book.getBids());

            assertAll(
                    () -> assertFalse(asks.anyMatch(emptyLevel)),
                    () -> assertFalse(bids.anyMatch(emptyLevel))
            );
        }

        @Test
        void shouldMaintainIntegrityOfAskData() {
            for (int i = 0; i < FILLED_ASKS.length; i++) {
                assertEquals(FILLED_ASKS[i].getPrice(), result.getAskPrice(i), Integer.toString(i));
                assertEquals(FILLED_ASKS[i].getQuantity(), result.getAskSize(i), Integer.toString(i));
            }
        }

        @Test
        void shouldMaintainIntegrityOfBidData() {
            for (int i = 0; i < FILLED_BIDS.length; i++) {
                assertEquals(FILLED_BIDS[i].getPrice(), result.getBidPrice(i));
                assertEquals(FILLED_BIDS[i].getQuantity(), result.getBidSize(i));
            }
        }
    }
}