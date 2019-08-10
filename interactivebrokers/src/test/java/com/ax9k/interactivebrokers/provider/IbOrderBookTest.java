package com.ax9k.interactivebrokers.provider;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.ax9k.core.marketmodel.BidAsk.ASK;
import static com.ax9k.core.marketmodel.BidAsk.BID;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IbOrderBookTest {
    private final IbOrderBook testBook = new IbOrderBook(10);

    private void assertBidLevel0Equals(IbOrderBook book, int expectedSize, int expectedPrice) {
        assertBidLevelEquals(book, 0, expectedSize, expectedPrice);
    }

    private void assertBidLevelEquals(IbOrderBook book, int level, int expectedSize, int expectedPrice) {
        assertAll(
                () -> assertEquals(expectedPrice, book.getBidPrice(level)),
                () -> assertEquals(expectedSize, book.getBidSize(level))
        );
    }

    private void assertAskLevel0Equals(IbOrderBook book, int expectedSize, int expectedPrice) {
        assertAskLevelEquals(book, 0, expectedSize, expectedPrice);
    }

    private void assertAskLevelEquals(IbOrderBook book, int level, int expectedSize, int expectedPrice) {
        assertAll(
                () -> assertEquals(expectedPrice, book.getAskPrice(level)),
                () -> assertEquals(expectedSize, book.getAskSize(level))
        );
    }

    @Nested
    class WhenGivenValidDirectBookUpdateValues {
        @Test
        void shouldUpdateAskPriceWithoutError() {
            testBook.directBookUpdate(Instant.EPOCH, 0, ASK, 50, 1);
        }

        @Test
        void shouldUpdateAskPriceWithCorrectResult() {
            testBook.directBookUpdate(Instant.EPOCH, 0, ASK, 50, 1);
            assertAskLevel0Equals(testBook, 1, 50);
        }

        @Test
        void shouldUpdateAskQuantityWithoutError() {
            testBook.directBookUpdate(Instant.EPOCH, 0, ASK, 5, 10);
        }

        @Test
        void shouldUpdateAskQuantityWithCorrectResult() {
            testBook.directBookUpdate(Instant.EPOCH, 0, ASK, 21, 10);
            assertAskLevel0Equals(testBook, 10, 21);
        }

        @Test
        void shouldUpdateBidPriceWithoutError() {
            testBook.directBookUpdate(Instant.EPOCH, 0, BID, 50, 8);
        }

        @Test
        void shouldUpdateBidPriceWithCorrectResult() {
            testBook.directBookUpdate(Instant.EPOCH, 0, BID, 50, 8);
            assertBidLevel0Equals(testBook, 8, 50);
        }

        @Test
        void shouldUpdateBidQuantityWithoutError() {
            testBook.directBookUpdate(Instant.EPOCH, 0, BID, 2, 4);
        }

        @Test
        void shouldUpdateBidQuantityWithCorrectResult() {
            testBook.directBookUpdate(Instant.EPOCH, 0, BID, 2, 4);
            assertBidLevel0Equals(testBook, 4, 2);
        }
    }
}