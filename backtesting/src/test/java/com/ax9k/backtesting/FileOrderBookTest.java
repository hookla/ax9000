package com.ax9k.backtesting;

import com.ax9k.core.marketmodel.MarketEvent;
import com.ax9k.core.marketmodel.orderbook.OrderBookLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.ax9k.backtesting.TestMarketOrders.testAskOrder;
import static com.ax9k.backtesting.TestMarketOrders.testBidOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileOrderBookTest {
    private FileOrderBook testBook;

    @BeforeEach
    void initOrderBook() {
        testBook = new FileOrderBook(Instant.EPOCH);
    }

    private void assertBidLevel0Equals(FileOrderBook book, int expectedSize, int expectedPrice) {
        assertBidLevelEquals(book, 0, expectedSize, expectedPrice);
    }

    private void assertBidLevelEquals(FileOrderBook book, int level, int expectedSize, int expectedPrice) {
        assertAll(
                () -> assertEquals(expectedPrice, book.getBidPrice(level)),
                () -> assertEquals(expectedSize, book.getBidSize(level))
        );
    }

    private void assertAskLevel0Equals(FileOrderBook book, int expectedSize, int expectedPrice) {
        assertAskLevelEquals(book, 0, expectedSize, expectedPrice);
    }

    private void assertAskLevelEquals(FileOrderBook book, int level, int expectedSize, int expectedPrice) {
        assertAll(
                () -> assertEquals(expectedPrice, book.getAskPrice(level)),
                () -> assertEquals(expectedSize, book.getAskSize(level))
        );
    }

    @Nested
    class WhenProcessingValidAddOrderEvent {
        @Test
        void shouldProcessBidOrderWithoutError() {
            testBook.processAddOrderEvent(testBidOrder(1, 45));
        }

        @Test
        void shouldProcessBidOrderWithCorrectResult() {
            testBook.processAddOrderEvent(testBidOrder(1, 45));

            assertBidLevel0Equals(testBook, 1, 45);
        }

        @Test
        void shouldProcessAskOrderWithoutError() {
            testBook.processAddOrderEvent(testAskOrder(1, 45));
        }

        @Test
        void shouldProcessAskOrderWithCorrectResult() {
            testBook.processAddOrderEvent(testAskOrder(1, 45));

            assertAskLevel0Equals(testBook, 1, 45);
        }
    }

    @Nested
    class WhenProcessingValidTradeEvent {
        @Test
        void shouldProcessBidOrderWithoutError() {
            MarketEvent testBid = testBidOrder(5, 45);
            testBook.processAddOrderEvent(testBid);

            testBook.processTradeEvent(testBidOrder(3, 45), testBid);
        }

        @Test
        void shouldProcessBidOrderWithCorrectResult() {
            MarketEvent testBid = testBidOrder(5, 45);
            testBook.processAddOrderEvent(testBid);

            testBook.processTradeEvent(testBidOrder(3, 45), testBid);
            assertBidLevel0Equals(testBook, 2, 45);
        }

        @Test
        void shouldProcessAskOrderWithoutError() {
            MarketEvent testAsk = testAskOrder(5, 45);
            testBook.processAddOrderEvent(testAsk);

            testBook.processTradeEvent(testAskOrder(3, 45), testAsk);
        }

        @Test
        void shouldProcessAskOrderWithCorrectResult() {
            MarketEvent testAsk = testAskOrder(5, 45);
            testBook.processAddOrderEvent(testAsk);

            testBook.processTradeEvent(testAskOrder(3, 45), testAsk);
            assertAskLevel0Equals(testBook, 2, 45);
        }
    }

    @Nested
    class WhenProcessingValidDeleteOrderEvent {
        @Test
        void shouldProcessBidOrderWithoutError() {
            MarketEvent testBid = testBidOrder(5, 33);
            testBook.processAddOrderEvent(testBid);

            testBook.processDeleteOrder(testBid, testBid);
        }

        @Test
        void shouldProcessBidOrderWithCorrectResult() {
            MarketEvent testBid = testBidOrder(5, 33);
            testBook.processAddOrderEvent(testBid);

            testBook.processDeleteOrder(testBid, testBid);
            assertBidLevel0Equals(testBook, 0, 0);
        }

        @Test
        void shouldProcessAskOrderWithoutError() {
            MarketEvent testAsk = testAskOrder(3, 40);
            testBook.processAddOrderEvent(testAsk);

            testBook.processDeleteOrder(testAsk, testAsk);
        }

        @Test
        void shouldProcessAskOrderWithCorrectResult() {
            MarketEvent testAsk = testAskOrder(3, 40);
            testBook.processAddOrderEvent(testAsk);

            testBook.processDeleteOrder(testAsk, testAsk);
            assertAskLevel0Equals(testBook, 0, 0);
        }
    }

    @Nested
    class WhenAskLevelsFilled {
        @Test
        void shouldBeOrderedFromLowestToHighestPrices() {
            fillAskLevels(testBook);

            double previousPrice = testBook.getAskPrice(0);
            for (OrderBookLevel level : testBook.getAsks()) {
                double currentPrice = level.getPrice();
                assertTrue(currentPrice >= previousPrice);
                previousPrice = currentPrice;
            }
        }

        void fillAskLevels(FileOrderBook book) {
            for (int i = 0; i < 10; i++) {
                book.processAddOrderEvent(testAskOrder(i + 5, i + 20));
            }
        }

        @Test
        void askLevelsShouldShiftForNewLowestPriceValue() {
            fillAskLevels(testBook);
            OrderBookLevel previousAsk0 = testBook.getAsks()[0];

            testBook.processAddOrderEvent(testAskOrder(1, 1));

            assertNotEquals(previousAsk0, testBook.getAsks()[0]);
            assertEquals(previousAsk0, testBook.getAsks()[1]);
        }

        @Test
        void askLevelsShouldNotShiftForNewHighestPriceValue() {
            fillAskLevels(testBook);
            OrderBookLevel[] asks = testBook.getAsks();
            OrderBookLevel previousAsk99 = asks[asks.length - 1];

            testBook.processAddOrderEvent(testAskOrder(50, 50));

            assertEquals(previousAsk99, asks[asks.length - 1]);
        }

        @Test
        void shouldNotContainLevelsWithPriceOfZero() {
            fillAskLevels(testBook);

            for (OrderBookLevel level : testBook.getAsks()) {
                assertNotEquals(0, level.getPrice());
            }
        }
    }

    @Nested
    class WhenBidLevelsFilled {
        @Test
        void shouldBeOrderedFromHighestToLowestPrices() {
            fillBidLevels(testBook);

            double previousPrice = testBook.getAskPrice(0);
            for (OrderBookLevel level : testBook.getAsks()) {
                double currentPrice = level.getPrice();
                assertTrue(currentPrice <= previousPrice);
                previousPrice = currentPrice;
            }
        }

        void fillBidLevels(FileOrderBook book) {
            for (int i = 0; i < 10; i++) {
                book.processAddOrderEvent(testBidOrder(i + 5, i + 20));
            }
        }

        @Test
        void bidLevelsShouldShiftForNewHighestPriceValue() {
            fillBidLevels(testBook);
            OrderBookLevel previousBid0 = testBook.getBids()[0];

            testBook.processAddOrderEvent(testBidOrder(50, 50));

            assertNotEquals(previousBid0, testBook.getBids()[0]);
            assertEquals(previousBid0, testBook.getBids()[1]);
        }

        @Test
        void bidLevelsShouldNotShiftForNewLowestPriceValue() {
            fillBidLevels(testBook);
            OrderBookLevel[] bids = testBook.getBids();
            OrderBookLevel previousBid99 = bids[bids.length - 1];

            testBook.processAddOrderEvent(testBidOrder(1, 1));

            assertEquals(previousBid99, bids[bids.length - 1]);
        }

        @Test
        void shouldNotContainLevelsWithPriceOfZero() {
            fillBidLevels(testBook);

            for (OrderBookLevel level : testBook.getBids()) {
                assertNotEquals(0, level.getPrice());
            }
        }
    }
}