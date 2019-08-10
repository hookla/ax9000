package com.ax9k.backtesting;

import com.ax9k.core.history.History;
import com.ax9k.core.marketmodel.StandardTradingSchedule;
import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.core.time.Time;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.DoubleStream;

import static com.ax9k.utils.compare.ComparableUtils.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class OhlcvBarAggregatorTest {
    private static final int DUMMY = 2000;
    private static final Duration INPUT_PERIOD_LENGTH = Duration.ofSeconds(5);
    private static final Duration OUTPUT_PERIOD_LENGTH = Duration.ofMinutes(1);

    private OhlcvBarAggregator testAggregator;
    private History<OhlcvBar> completeAggregatedBars;

    private static Instant timestamp(int hours, int minutes) {
        return timestamp(hours, minutes, 0);
    }

    private static Instant timestamp(int hours, int minutes, int seconds) {
        LocalTime time = LocalTime.of(hours, minutes, seconds);
        return timestamp(time);
    }

    private static Instant timestamp(LocalTime time) {
        return LocalDateTime.of(LocalDate.EPOCH, time).atZone(ZoneOffset.ofHours(8)).toInstant();
    }

    @BeforeAll
    static void initialiseTime() {
        Time.setTradingSchedule(StandardTradingSchedule.wrap(List.of(), ZoneOffset.ofHours(8)));
    }

    @BeforeEach
    void initialiseAggregator() {
        testAggregator = new OhlcvBarAggregator();
        completeAggregatedBars = testAggregator.getHistory();
    }

    private void aggregateDummyBars(OhlcvBarAggregator testAggregator, Instant startTime, Instant endTime) {
        aggregateBars(testAggregator, startTime, endTime, DUMMY, DUMMY, DUMMY, DUMMY);
    }

    private void aggregateBars(OhlcvBarAggregator testAggregator, Instant startTime, Instant endTime,
                               double open, double high, double low, double close) {
        Validate.validState(startTime.isBefore(endTime),
                            "start must be before end");
        Validate.validState(DoubleStream.of(open, high, low, close).max().orElseThrow() == high,
                            "high must be greatest value");
        Validate.validState(DoubleStream.of(open, high, low, close).min().orElseThrow() == low,
                            "low must be smallest value");

        Instant currentTime = startTime;
        OhlcvBar openBar = OhlcvBar.of(currentTime, open, high, low, DUMMY, 1);

        testAggregator.aggregateData(openBar);
        currentTime = currentTime.plus(INPUT_PERIOD_LENGTH);

        while (lessThan(currentTime, endTime)) {
            OhlcvBar currentBar = OhlcvBar.of(currentTime, DUMMY, high - 1, low + 1, DUMMY, 1);
            testAggregator.aggregateData(currentBar);
            currentTime = currentTime.plus(INPUT_PERIOD_LENGTH);
        }

        OhlcvBar closeBar = OhlcvBar.of(currentTime, DUMMY, high - 1, low + 1, close, 1);
        testAggregator.aggregateData(closeBar);
    }

    @Nested
    class WhenNew {
        @Test
        void historyShouldBeEmpty() {
            assertEquals(0, completeAggregatedBars.getSize());
            assertTrue(completeAggregatedBars.isEmpty());
        }
    }

    @Nested
    class WhenLessThanOneFullOhlcvPeriodElapsed {
        @BeforeEach
        void simulateOneHalfOhlcvPeriod() {
            Instant start = timestamp(10, 0, 5);
            Instant end = start.plus(OUTPUT_PERIOD_LENGTH.dividedBy(2));
            aggregateDummyBars(testAggregator, start, end);
        }

        @Test
        void historyShouldHaveNoCompleteBars() {
            assertEquals(0, completeAggregatedBars.getSize());
        }
    }

    @Nested
    class WhenOneFullOhlcvPeriodPassed {
        private static final double EXPECTED_OPEN = 4, EXPECTED_HIGH = 5000, EXPECTED_LOW = -1, EXPECTED_CLOSE = 8;

        private final Instant startTime = timestamp(9, 15);
        private final Instant endTime = timestamp(9, 15, 55);

        @BeforeEach
        void simulateOneFullOhlcvPeriod() {
            aggregateBars(testAggregator, startTime, endTime,
                          EXPECTED_OPEN, EXPECTED_HIGH, EXPECTED_LOW, EXPECTED_CLOSE);
        }

        @Test
        void historyShouldHaveOneCompleteBar() {
            assertEquals(1, completeAggregatedBars.getSize());
        }

        @Test
        void outputPeriodShouldEndWithCorrectTimestamp() {
            assertEquals(endTime, completeAggregatedBars.getLatest().get().getTimestamp());
        }

        @Test
        void shouldRecordCorrectOpen() {
            assertEquals(EXPECTED_OPEN, completeAggregatedBars.getLatest().get().getOpen());
        }

        @Test
        void shouldRecordCorrectHigh() {
            assertEquals(EXPECTED_HIGH, completeAggregatedBars.getLatest().get().getHigh());
        }

        @Test
        void shouldRecordCorrectLow() {
            assertEquals(EXPECTED_LOW, completeAggregatedBars.getLatest().get().getLow());
        }

        @Test
        void shouldRecordCorrectClose() {
            assertEquals(EXPECTED_CLOSE, completeAggregatedBars.getLatest().get().getClose());
        }

        @Test
        void periodShouldBeComposedOfCorrectNumberOfInputBars() {
            long eventsInOutputPeriod = OUTPUT_PERIOD_LENGTH.dividedBy(INPUT_PERIOD_LENGTH);
            assertEquals(eventsInOutputPeriod, completeAggregatedBars.getLatest().get().getVolume());
        }
    }

    @Nested
    class WhenTwoFullOhlcvPeriodPassed {
        private static final double EXPECTED_FIRST_OPEN = 84, EXPECTED_SECOND_OPEN = 31;
        private static final double EXPECTED_FIRST_HIGH = 1_026_821, EXPECTED_SECOND_HIGH = 200_000;
        private static final double EXPECTED_FIRST_LOW = 3, EXPECTED_SECOND_LOW = -12_212;
        private static final double EXPECTED_FIRST_CLOSE = 9_866, EXPECTED_SECOND_CLOSE = 14_566;

        private final Instant firstEventTime = timestamp(9, 15);
        private final Instant firstPeriodEnd = timestamp(9, 15, 55);
        private final Instant secondPeriodStart = timestamp(9, 16);
        private final Instant secondPeriodEnd = timestamp(9, 16, 55);

        @BeforeEach
        void simulateTwoFullOhlcvPeriods() {
            aggregateBars(testAggregator, firstEventTime, firstPeriodEnd,
                          EXPECTED_FIRST_OPEN, EXPECTED_FIRST_HIGH, EXPECTED_FIRST_LOW, EXPECTED_FIRST_CLOSE);
            aggregateBars(testAggregator, secondPeriodStart, secondPeriodEnd,
                          EXPECTED_SECOND_OPEN, EXPECTED_SECOND_HIGH, EXPECTED_SECOND_LOW, EXPECTED_SECOND_CLOSE);
        }

        @Test
        void historyShouldHaveTwoCompleteBars() {
            assertEquals(2, completeAggregatedBars.getSize());
        }

        @Test
        void firstOutputPeriodShouldEndWithCorrectTimestamp() {
            assertEquals(firstPeriodEnd, completeAggregatedBars.getPrevious().get().getTimestamp());
        }

        @Test
        void shouldRecordCorrectFirstOpen() {
            assertEquals(EXPECTED_FIRST_OPEN, completeAggregatedBars.getPrevious().get().getOpen());
        }

        @Test
        void shouldRecordCorrectFirstHigh() {
            assertEquals(EXPECTED_FIRST_HIGH, completeAggregatedBars.getPrevious().get().getHigh());
        }

        @Test
        void shouldRecordCorrectFirstLow() {
            assertEquals(EXPECTED_FIRST_LOW, completeAggregatedBars.getPrevious().get().getLow());
        }

        @Test
        void shouldRecordCorrectFirstClose() {
            assertEquals(EXPECTED_FIRST_CLOSE, completeAggregatedBars.getPrevious().get().getClose());
        }

        @Test
        void secondOutputPeriodShouldEndWithCorrectTimestamp() {
            assertEquals(secondPeriodEnd, completeAggregatedBars.getLatest().get().getTimestamp());
        }

        @Test
        void shouldRecordCorrectSecondOpen() {
            assertEquals(EXPECTED_SECOND_OPEN, completeAggregatedBars.getLatest().get().getOpen());
        }

        @Test
        void shouldRecordCorrectSecondHigh() {
            assertEquals(EXPECTED_SECOND_HIGH, completeAggregatedBars.getLatest().get().getHigh());
        }

        @Test
        void shouldRecordCorrectSecondLow() {
            assertEquals(EXPECTED_SECOND_LOW, completeAggregatedBars.getLatest().get().getLow());
        }

        @Test
        void shouldRecordCorrectSecondClose() {
            assertEquals(EXPECTED_SECOND_CLOSE, completeAggregatedBars.getLatest().get().getClose());
        }

        @Test
        void bothOutputBarsShouldBeComposedOfSameNumberOfInputBars() {
            double firstVolume = completeAggregatedBars.getPrevious().get().getVolume();
            double secondVolume = completeAggregatedBars.getLatest().get().getVolume();

            assertEquals(firstVolume, secondVolume);
        }

        @Test
        void outputBarsShouldBeComposedOfCorrectNumberOfInputBars() {
            long eventsInOutputPeriod = OUTPUT_PERIOD_LENGTH.dividedBy(INPUT_PERIOD_LENGTH);
            assertEquals(eventsInOutputPeriod, completeAggregatedBars.getLatest().get().getVolume());
        }
    }

    @Nested
    class WhenLargeDelayOccursBetweenTwoFullOhlcvPeriods {
        private static final double EXPECTED_FIRST_OPEN = 84, EXPECTED_SECOND_OPEN = 31;
        private static final double EXPECTED_FIRST_HIGH = 1_026_821, EXPECTED_SECOND_HIGH = 200_000;
        private static final double EXPECTED_FIRST_LOW = 3, EXPECTED_SECOND_LOW = -12_212;
        private static final double EXPECTED_FIRST_CLOSE = 9_866, EXPECTED_SECOND_CLOSE = 14_566;

        private final Instant firstEventTime = timestamp(11, 59);
        private final Instant firstPeriodEnd = timestamp(11, 59, 55);
        private final Instant secondPeriodStart = timestamp(13, 0);
        private final Instant secondPeriodEnd = timestamp(13, 0, 55);

        @BeforeEach
        void simulateTwoFullOhlcvPeriods() {
            aggregateBars(testAggregator, firstEventTime, firstPeriodEnd,
                          EXPECTED_FIRST_OPEN, EXPECTED_FIRST_HIGH, EXPECTED_FIRST_LOW, EXPECTED_FIRST_CLOSE);
            aggregateBars(testAggregator, secondPeriodStart, secondPeriodEnd,
                          EXPECTED_SECOND_OPEN, EXPECTED_SECOND_HIGH, EXPECTED_SECOND_LOW, EXPECTED_SECOND_CLOSE);
        }

        @Test
        void historyShouldHaveTwoCompleteBars() {
            assertEquals(2, completeAggregatedBars.getSize());
        }

        @Test
        void firstOutputPeriodShouldEndWithCorrectTimestamp() {
            assertEquals(firstPeriodEnd, completeAggregatedBars.getPrevious().get().getTimestamp());
        }

        @Test
        void shouldRecordCorrectFirstOpen() {
            assertEquals(EXPECTED_FIRST_OPEN, completeAggregatedBars.getPrevious().get().getOpen());
        }

        @Test
        void shouldRecordCorrectFirstHigh() {
            assertEquals(EXPECTED_FIRST_HIGH, completeAggregatedBars.getPrevious().get().getHigh());
        }

        @Test
        void shouldRecordCorrectFirstLow() {
            assertEquals(EXPECTED_FIRST_LOW, completeAggregatedBars.getPrevious().get().getLow());
        }

        @Test
        void shouldRecordCorrectFirstClose() {
            assertEquals(EXPECTED_FIRST_CLOSE, completeAggregatedBars.getPrevious().get().getClose());
        }

        @Test
        void secondOutputPeriodShouldEndWithCorrectTimestamp() {
            assertEquals(secondPeriodEnd, completeAggregatedBars.getLatest().get().getTimestamp());
        }

        @Test
        void shouldRecordCorrectSecondOpen() {
            assertEquals(EXPECTED_SECOND_OPEN, completeAggregatedBars.getLatest().get().getOpen());
        }

        @Test
        void shouldRecordCorrectSecondHigh() {
            assertEquals(EXPECTED_SECOND_HIGH, completeAggregatedBars.getLatest().get().getHigh());
        }

        @Test
        void shouldRecordCorrectSecondLow() {
            assertEquals(EXPECTED_SECOND_LOW, completeAggregatedBars.getLatest().get().getLow());
        }

        @Test
        void shouldRecordCorrectSecondClose() {
            assertEquals(EXPECTED_SECOND_CLOSE, completeAggregatedBars.getLatest().get().getClose());
        }

        @Test
        void bothOutputBarsShouldBeComposedOfSameNumberOfInputBars() {
            double firstVolume = completeAggregatedBars.getPrevious().get().getVolume();
            double secondVolume = completeAggregatedBars.getLatest().get().getVolume();

            assertEquals(firstVolume, secondVolume);
        }

        @Test
        void outputBarsShouldBeComposedOfCorrectNumberOfInputBars() {
            long eventsInOutputPeriod = OUTPUT_PERIOD_LENGTH.dividedBy(INPUT_PERIOD_LENGTH);
            assertEquals(eventsInOutputPeriod, completeAggregatedBars.getLatest().get().getVolume());
        }
    }
}