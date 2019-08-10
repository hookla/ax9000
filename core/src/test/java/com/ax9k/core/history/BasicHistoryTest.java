package com.ax9k.core.history;

import com.ax9k.core.event.Event;
import com.ax9k.core.marketmodel.MockEvent;
import com.ax9k.core.time.Time;
import com.ax9k.util.EqualsAndHashCodeSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.ax9k.core.marketmodel.MockEvent.listWithMillisAfterEpoch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicHistoryTest {
    private static final Duration FIVE_MILLIS = Duration.ofMillis(5);
    private static final Duration THREE_MILLIS = Duration.ofMillis(3);

    private BasicHistory<Event> testHistory;

    private static BasicHistory<Event> fillHistoryWithMillisAfterEpoch(int... values) {
        BasicHistory<Event> result = new BasicHistory<>();

        MockEvent.streamWithMillisAfterEpoch(values).forEach(result::record);

        return result;
    }

    private static void assertNotNull(Object object) {
        assertNotEquals(null, object);
    }

    @Nested
    class WhenEqualsAndHashCodeMethodsCalled implements EqualsAndHashCodeSpecification<BasicHistory> {

        @Override
        public BasicHistory getSecondEqualValue() {
            return fillHistoryWithMillisAfterEpoch(1, 2, 3);
        }

        @Override
        public BasicHistory getValue() {
            return fillHistoryWithMillisAfterEpoch(1, 2, 3);
        }

        @Override
        public BasicHistory getEqualValue() {
            return fillHistoryWithMillisAfterEpoch(1, 2, 3);
        }

        @Override
        public BasicHistory getUnequalValue() {
            return fillHistoryWithMillisAfterEpoch(1, 2, 3, 4);
        }
    }

    @Nested
    class WhenFilteringValidHistoryByDuration {
        @BeforeEach
        void fillHistory() {
            testHistory = fillHistoryWithMillisAfterEpoch(1, 2, 3, 4, 4, 4, 5, 6, 7, 8, 9, 10);
            Time.update(Instant.ofEpochMilli(10));
        }

        @Test
        void shouldFilterByDurationWithoutError() {
            Source filtered = testHistory.asSource(FIVE_MILLIS);
            assertNotNull(filtered);
        }

        @Test
        void shouldFilterByDurationWithCorrectResult() {
            Source filtered = testHistory.asSource(THREE_MILLIS);
            List<Event> expected = listWithMillisAfterEpoch(7, 8, 9, 10);
            assertIterableEquals(expected, filtered);
        }

        @Test
        void shouldFilterByDurationWithConsistentResult() {
            Source filtered1 = testHistory.asSource(FIVE_MILLIS);
            Source filtered2 = testHistory.asSource(FIVE_MILLIS);

            assertEquals(filtered1, filtered2);
            assertIterableEquals(filtered1, filtered2);
        }
    }

    @Nested
    class WhenFilteringValidHistoryByNumberOfEntries {
        @BeforeEach
        void fillHistory() {
            testHistory = fillHistoryWithMillisAfterEpoch(1, 2, 3, 4, 4, 4, 5, 6, 7, 8, 9, 10);
        }

        @Test
        void shouldFilterByNumberOfEntriesWithoutError() {
            Source filtered = testHistory.asSource(2);
            assertNotNull(filtered);
        }

        @Test
        void shouldFilterByNumberOfEntriesWithCorrectResult() {
            Source filtered = testHistory.asSource(4);
            List<Event> expected = listWithMillisAfterEpoch(7, 8, 9, 10);
            assertEquals(4, expected.size());
            assertIterableEquals(expected, filtered);
        }

        @Test
        void shouldFilterByNumberOfEntriesWithConsistentResult() {
            Source filtered1 = testHistory.asSource(3);
            Source filtered2 = testHistory.asSource(3);

            assertEquals(filtered1, filtered2);
            assertIterableEquals(filtered1, filtered2);
        }
    }

    @Nested
    class WhenConvertingWholeValidHistoryToSource {
        @BeforeEach
        void fillHistory() {
            testHistory = fillHistoryWithMillisAfterEpoch(1, 2, 2, 4, 5);
        }

        @Test
        void shouldConvertWithoutError() {
            Source source = testHistory.asSource();
            assertNotNull(source);
        }

        @Test
        void shouldConvertWithCorrectResult() {
            Source filtered = testHistory.asSource();
            List<Event> expected = listWithMillisAfterEpoch(1, 2, 2, 4, 5);
            assertIterableEquals(expected, filtered);
        }

        @Test
        void shouldConvertWithConsistentResult() {
            Source filtered1 = testHistory.asSource();
            Source filtered2 = testHistory.asSource();

            assertEquals(filtered1, filtered2);
            assertIterableEquals(filtered1, filtered2);
        }
    }

    @Nested
    class WhenGivenValidState {
        @BeforeEach
        void giveValidState() {
            testHistory = fillHistoryWithMillisAfterEpoch(1, 2, 3, 4);
            Time.update(Instant.ofEpochMilli(4));
        }

        @Test
        void shouldGetLatestWithoutError() {
            Optional<Event> latest = testHistory.getLatest();
            assertNotNull(latest);
            assertTrue(latest.isPresent());
        }

        @Test
        void shouldGetLatestWithCorrectResult() {
            Event latest = testHistory.getLatest().orElseThrow();
            assertEquals(Instant.EPOCH.plusMillis(4), latest.getTimestamp());
        }

        @Test
        void shouldGetPreviousWithoutError() {
            Optional<Event> previous = testHistory.getPrevious();
            assertNotNull(previous);
            assertTrue(previous.isPresent());
        }

        @Test
        void shouldGetPreviousWithCorrectResult() {
            Event previous = testHistory.getPrevious().orElseThrow();
            assertEquals(Instant.EPOCH.plusMillis(3), previous.getTimestamp());
        }
    }
}