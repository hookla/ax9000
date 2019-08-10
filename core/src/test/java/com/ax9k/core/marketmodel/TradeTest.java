package com.ax9k.core.marketmodel;

import com.ax9k.util.EqualsAndHashCodeSpecification;
import org.junit.jupiter.api.Nested;

import java.time.Instant;

class TradeTest {
    @Nested
    class WhenEqualsAndHashCodeMethodsCalled implements EqualsAndHashCodeSpecification<Trade> {
        @Override
        public Trade getSecondEqualValue() {
            return new Trade(Instant.EPOCH, 2, 3, 5, BidAsk.ASK);
        }

        @Override
        public Trade getValue() {
            return new Trade(Instant.EPOCH, 2, 3, 5, BidAsk.ASK);
        }

        @Override
        public Trade getEqualValue() {
            return new Trade(Instant.EPOCH, 2, 3, 5, BidAsk.ASK);
        }

        @Override
        public Trade getUnequalValue() {
            return new Trade(Instant.EPOCH, 7, 8, 10, BidAsk.ASK);
        }
    }
}