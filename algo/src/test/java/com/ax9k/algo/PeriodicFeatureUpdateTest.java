package com.ax9k.algo;

import com.ax9k.core.history.History;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.core.time.Time;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

import static java.time.LocalDate.EPOCH;
import static java.time.LocalTime.MIDNIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodicFeatureUpdateTest {
    private static final TradingSchedule SAMPLE_TRADING_SCHEDULE = SamplePhase.tradingSchedule();
    private final Duration updatePeriod = Duration.ofMinutes(1);
    private final LocalTime startTime = MIDNIGHT;

    private EventRecorder recorder;
    private PeriodicFeatureUpdate update;
    private Instant testCurrentTime;

    @BeforeAll
    static void initialiseTime() {
        Time.setTradingSchedule(SamplePhase.tradingSchedule());
    }

    @BeforeEach
    void initialiseTestEnvironment() {
        recorder = new EventRecorder();
        update = new PeriodicFeatureUpdate(SAMPLE_TRADING_SCHEDULE, recorder, updatePeriod, startTime);

        LocalTime testTime = startTime.plus(updatePeriod).minusSeconds(30);
        testCurrentTime = toInstant(testTime);
    }

    private static Instant toInstant(LocalTime time) {
        return ZonedDateTime.of(EPOCH, time, SAMPLE_TRADING_SCHEDULE.getTimeZone()).toInstant();
    }

    private void assertTimesEqual(History<PeriodicFeatureResult> history, String[] times, String timeKey) {
        assertEquals(times.length, history.getSize());

        int i = 0;
        for (PeriodicFeatureResult result : history) {
            assertEquals(times[i++], result.get(timeKey, String.class));
        }
    }

    private static final class EventRecorder implements Consumer<PeriodicFeatureResult> {
        private int count;

        @Override
        public void accept(PeriodicFeatureResult result) {
            count++;
        }

        int invocationCount() {
            return count;
        }
    }

    @Nested
    class WhenNew {
        @Test
        void historyShouldBeEmpty() {
            assertTrue(update.getHistory().isEmpty());
        }

        @Test
        void shouldHaveNoLatestResult() {
            assertFalse(update.getLatestResult().isPresent());
        }

        @Test
        void shouldNotReportBeingUpdated() {
            assertFalse(update.updatedInLastCycle());
        }

        @Test
        void shouldHaveNonNullFeatureManager() {
            assertNotNull(update.getManager());
        }
    }

    @Nested
    class WhenNoUpdatesDue {
        @BeforeEach
        void setClock() {
            LocalTime thirtySecondsBeforeUpdate = startTime.plus(updatePeriod).minusSeconds(30);
            testCurrentTime = toInstant(thirtySecondsBeforeUpdate);
        }

        @Test
        void historyShouldBeEmpty() {
            update.runIfNecessary(testCurrentTime);

            assertTrue(update.getHistory().isEmpty());
        }

        @Test
        void shouldNotReportUpdate() {
            update.runIfNecessary(testCurrentTime);

            assertFalse(update.updatedInLastCycle());
        }

        @Test
        void shouldNotSendPeriodicFeatureResult() {
            update.runIfNecessary(testCurrentTime);

            assertEquals(0, recorder.invocationCount());
        }

        @Test
        void shouldHaveNoLatestResult() {
            update.runIfNecessary(testCurrentTime);

            assertFalse(update.getLatestResult().isPresent());
        }
    }

    @Nested
    class WhenSingleUpdateDue {
        @BeforeEach
        void setClock() {
            LocalTime firstUpdateTime = startTime.plus(updatePeriod);
            testCurrentTime = toInstant(firstUpdateTime);
        }

        @Test
        void shouldReportUpdate() {
            update.runIfNecessary(testCurrentTime);

            assertTrue(update.updatedInLastCycle());
        }

        @Test
        void shouldHaveLatestResult() {
            update.runIfNecessary(testCurrentTime);

            assertTrue(update.getLatestResult().isPresent());
        }

        @Test
        void historyShouldHaveOneElement() {
            update.runIfNecessary(testCurrentTime);

            assertEquals(1, update.getHistory().getSize());
        }

        @Test
        void shouldSendPeriodicFeatureResultOnlyOnce() {
            update.runIfNecessary(testCurrentTime);

            assertEquals(1, recorder.invocationCount());
        }
    }

    @Nested
    class WhenFiveUpdatesDue {
        @BeforeEach
        void setClock() {
            initialiseTestEnvironment();

            LocalTime fiveUpdatesIn = startTime.plus(updatePeriod.multipliedBy(5));
            testCurrentTime = toInstant(fiveUpdatesIn);
        }

        @Test
        void shouldReportUpdate() {
            update.runIfNecessary(testCurrentTime);

            assertTrue(update.updatedInLastCycle());
        }

        @Test
        void shouldHaveLatestResult() {
            update.runIfNecessary(testCurrentTime);

            assertTrue(update.getLatestResult().isPresent());
        }

        @Test
        void historyShouldHaveFiveElements() {
            update.runIfNecessary(testCurrentTime);

            assertEquals(5, update.getHistory().getSize());
        }

        @Test
        void shouldSendPeriodicFeatureResultTwice() {
            update.runIfNecessary(testCurrentTime);

            /* Once for the filler and once for the actual features. */
            assertEquals(2, recorder.invocationCount());
        }
    }

    @Nested
    class When50UpdatesDue {
        @BeforeEach
        void setClock() {
            LocalTime oneHundredUpdatesIn = startTime.plus(updatePeriod.multipliedBy(50));

            testCurrentTime = toInstant(oneHundredUpdatesIn);
        }

        @Test
        void shouldReportUpdate() {
            update.runIfNecessary(testCurrentTime);

            assertTrue(update.updatedInLastCycle());
        }

        @Test
        void shouldHaveLatestResult() {
            update.runIfNecessary(testCurrentTime);

            assertTrue(update.getLatestResult().isPresent());
        }

        @Test
        void historyShouldHave100Elements() {
            update.runIfNecessary(testCurrentTime);

            assertEquals(50, update.getHistory().getSize());
        }

        @Test
        void shouldSendPeriodicFeatureResultTwice() {
            update.runIfNecessary(testCurrentTime);

            /* Once for the filler and once for the actual features. */
            assertEquals(2, recorder.invocationCount());
        }
    }

    @Nested
    class WhenRunOverLunch {
        @Test
        void shouldNotSendUpdateOverLunch() {
            update = new PeriodicFeatureUpdate(SAMPLE_TRADING_SCHEDULE, recorder, updatePeriod, LocalTime.of(12, 30));
            testCurrentTime = toInstant(LocalTime.of(12, 31));

            update.runIfNecessary(testCurrentTime);

            assertEquals(0, recorder.invocationCount());
            assertEquals(0, update.getHistory().getSize());
        }

        @Test
        void shouldSkipLunchPeriodDuringCatchup() {
            update = new PeriodicFeatureUpdate(SAMPLE_TRADING_SCHEDULE, recorder, updatePeriod, LocalTime.of(11, 58));
            testCurrentTime = toInstant(LocalTime.of(13, 2));

            update.runIfNecessary(testCurrentTime);

            String[] expectedStartTimes = { "11:58", "11:59", "13:00", "13:01" };
            String[] expectedEndTimes = { "11:59", "12:00", "13:01", "13:02" };

            assertTimesEqual(update.getHistory(), expectedStartTimes, "periodStart");
            assertTimesEqual(update.getHistory(), expectedEndTimes, "periodEnd");
        }
    }
}