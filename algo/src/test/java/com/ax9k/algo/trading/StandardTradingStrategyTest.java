package com.ax9k.algo.trading;

import com.ax9k.algo.Algo.Signal;
import com.ax9k.algo.SamplePhase;
import com.ax9k.core.time.Time;
import com.ax9k.positionmanager.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.ax9k.algo.Algo.Signal.BUY;
import static com.ax9k.algo.Algo.Signal.NONE;
import static com.ax9k.algo.Algo.Signal.SELL;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

class StandardTradingStrategyTest {
    private static final LocalTime EXIT_TIME = LocalTime.of(13, 30);

    private StandardTradingStrategy testStrategy;
    private TradingAlgo mockAlgo;

    private static SignalContext createEnterSignalContext(Signal previous, Signal current) {
        return createSignalContext(previous, current, NONE, NONE);
    }

    private static SignalContext createSignalContext(Signal previousEnter, Signal currentEnter,
                                                     Signal previousExit, Signal currentExit) {
        SignalContext previous = new SignalContext(previousEnter, previousExit);
        return previous.update(currentEnter, currentExit);
    }

    private static SignalContext createExitSignalContext(Signal previous, Signal current) {
        return createSignalContext(NONE, NONE, previous, current);
    }

    private static Position createPosition(PositionType type) {
        Position result = new Position();
        result.setContractPosition(type.samplePosition);
        return result;
    }

    @BeforeAll
    static void initTradingSchedule() {
        Time.setTradingSchedule(SamplePhase.tradingSchedule());
    }

    @BeforeEach
    void initialiseTestEnvironment() {
        Time.update(Instant.EPOCH);
        testStrategy = StandardTradingStrategy.exitAt(EXIT_TIME, 1);
        mockAlgo = mock(TradingAlgo.class);
    }

    private void assertBuys(double quantity, TradeDirective directive) {
        directive.execute(mockAlgo);

        verify(mockAlgo, times(1)).buyAtMarket(quantity);
        verifyNoMoreInteractions(mockAlgo);
    }

    private void assertSells(double quantity, TradeDirective directive) {
        directive.execute(mockAlgo);

        verify(mockAlgo, times(1)).sellAtMarket(quantity);
        verifyNoMoreInteractions(mockAlgo);
    }

    private void assertExits(TradeDirective directive) {
        directive.execute(mockAlgo);

        verify(mockAlgo, times(1)).exitPosition(anyString());
        verifyNoMoreInteractions(mockAlgo);
    }

    private void assertDoesNothing(TradeDirective directive) {
        directive.execute(mockAlgo);

        verifyZeroInteractions(mockAlgo);
    }

    private enum PositionType {
        LONG(1), SHORT(-1), FLAT(0);

        private final double samplePosition;

        PositionType(double samplePosition) {
            this.samplePosition = samplePosition;
        }
    }

    @Nested
    class WhenEnterSignalChangesDirectlyFromBuyToSell {
        @Test
        void shouldSellTwoInLongPosition() {
            var signals = createEnterSignalContext(BUY, SELL);
            var position = createPosition(PositionType.LONG);

            var directive = testStrategy.decideAction(signals, position);

            assertSells(2, directive);
        }

        @Test
        void shouldSellOneInFlatPosition() {
            var signals = createEnterSignalContext(BUY, SELL);
            var position = createPosition(PositionType.FLAT);

            var directive = testStrategy.decideAction(signals, position);

            assertSells(1, directive);
        }

        @Test
        void shouldDoNothingInShortPosition() {
            var signals = createEnterSignalContext(BUY, SELL);
            var position = createPosition(PositionType.SHORT);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }
    }

    @Nested
    class WhenEnterSignalChangesDirectlyFromSellToBuy {
        @Test
        void shouldBuyTwoInShortPosition() {
            var signals = createEnterSignalContext(SELL, BUY);
            var position = createPosition(PositionType.SHORT);

            var directive = testStrategy.decideAction(signals, position);

            assertBuys(2, directive);
        }

        @Test
        void shouldBuyOneInFlatPosition() {
            var signals = createEnterSignalContext(SELL, BUY);
            var position = createPosition(PositionType.FLAT);

            var directive = testStrategy.decideAction(signals, position);

            assertBuys(1, directive);
        }

        @Test
        void shouldDoNothingInLongPosition() {
            var signals = createEnterSignalContext(SELL, BUY);
            var position = createPosition(PositionType.LONG);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }
    }

    @Nested
    class WhenEnterSignalChangesFromNoneToBuy {
        @Test
        void shouldDoNothingInLongPosition() {
            var signals = createEnterSignalContext(NONE, BUY);
            var position = createPosition(PositionType.LONG);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }

        @Test
        void shouldBuyOneInFlatPosition() {
            var signals = createEnterSignalContext(NONE, BUY);
            var position = createPosition(PositionType.FLAT);

            var directive = testStrategy.decideAction(signals, position);

            assertBuys(1, directive);
        }

        @Test
        void shouldDoNothingInShortPosition() {
            var signals = createEnterSignalContext(NONE, BUY);
            var position = createPosition(PositionType.SHORT);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }
    }

    @Nested
    class WhenEnterSignalChangesFromNoneToSell {
        @Test
        void shouldDoNothingInLongPosition() {
            var signals = createEnterSignalContext(NONE, SELL);
            var position = createPosition(PositionType.LONG);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }

        @Test
        void shouldSellOneInFlatPosition() {
            var signals = createEnterSignalContext(NONE, SELL);
            var position = createPosition(PositionType.FLAT);

            var directive = testStrategy.decideAction(signals, position);

            assertSells(1, directive);
        }

        @Test
        void shouldDoNothingInShortPosition() {
            var signals = createEnterSignalContext(NONE, SELL);
            var position = createPosition(PositionType.SHORT);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }
    }

    @Nested
    class WhenExitSignalChangesToSell {
        @Test
        void shouldExitLongPosition() {
            var signals = createExitSignalContext(NONE, SELL);
            var position = createPosition(PositionType.LONG);

            var directive = testStrategy.decideAction(signals, position);

            assertExits(directive);
        }

        @Test
        void shouldDoNothingInFlatPosition() {
            var signals = createExitSignalContext(NONE, SELL);
            var position = createPosition(PositionType.FLAT);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }

        @Test
        void shouldDoNothingInShortPosition() {
            var signals = createExitSignalContext(NONE, SELL);
            var position = createPosition(PositionType.SHORT);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }
    }

    @Nested
    class WhenExitSignalChangesToBuy {
        @Test
        void shouldDoNothingInLongPosition() {
            var signals = createExitSignalContext(NONE, BUY);
            var position = createPosition(PositionType.LONG);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }

        @Test
        void shouldDoNothingInFlatPosition() {
            var signals = createExitSignalContext(NONE, BUY);
            var position = createPosition(PositionType.FLAT);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }

        @Test
        void shouldExitShortPosition() {
            var signals = createExitSignalContext(NONE, BUY);
            var position = createPosition(PositionType.SHORT);

            var directive = testStrategy.decideAction(signals, position);

            assertExits(directive);
        }
    }

    @Nested
    class WhenExitSignalChangesToNone {
        @Test
        void shouldExitLongPosition() {
            var signals = createExitSignalContext(BUY, NONE);
            var position = createPosition(PositionType.LONG);

            var directive = testStrategy.decideAction(signals, position);

            assertExits(directive);
        }

        @Test
        void shouldDoNothingInFlatPosition() {
            var signals = createExitSignalContext(BUY, NONE);
            var position = createPosition(PositionType.FLAT);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }

        @Test
        void shouldExitShortPosition() {
            var signals = createExitSignalContext(BUY, NONE);
            var position = createPosition(PositionType.SHORT);

            var directive = testStrategy.decideAction(signals, position);

            assertExits(directive);
        }
    }

    @Nested
    class WhenAtExitTime {
        @BeforeEach
        void setTime() {
            Instant exitTimestamp =
                    Time.internationalise(LocalDateTime.of(LocalDate.EPOCH, EXIT_TIME));
            Time.update(exitTimestamp);
        }

        @Test
        void shouldExitLongPosition() {
            var signals = createEnterSignalContext(BUY, SELL);
            var position = createPosition(PositionType.LONG);

            var directive = testStrategy.decideAction(signals, position);

            assertExits(directive);
        }

        @Test
        void shouldDoNothingInFlatPosition() {
            var signals = createEnterSignalContext(NONE, BUY);
            var position = createPosition(PositionType.FLAT);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }

        @Test
        void shouldExitShortPosition() {
            var signals = createEnterSignalContext(SELL, BUY);
            var position = createPosition(PositionType.SHORT);

            var directive = testStrategy.decideAction(signals, position);

            assertExits(directive);
        }
    }

    @Nested
    class WhenPastExitTime {
        @BeforeEach
        void setTime() {
            Instant oneMinuteAfterExit =
                    Time.internationalise(LocalDateTime.of(LocalDate.EPOCH, EXIT_TIME.plusMinutes(1)));
            Time.update(oneMinuteAfterExit);
        }

        @Test
        void shouldExitLongPosition() {
            var signals = createEnterSignalContext(BUY, SELL);
            var position = createPosition(PositionType.LONG);

            var directive = testStrategy.decideAction(signals, position);

            assertExits(directive);
        }

        @Test
        void shouldDoNothingInFlatPosition() {
            var signals = createEnterSignalContext(NONE, BUY);
            var position = createPosition(PositionType.FLAT);

            var directive = testStrategy.decideAction(signals, position);

            assertDoesNothing(directive);
        }

        @Test
        void shouldExitShortPosition() {
            var signals = createEnterSignalContext(SELL, BUY);
            var position = createPosition(PositionType.SHORT);

            var directive = testStrategy.decideAction(signals, position);

            assertExits(directive);
        }
    }
}