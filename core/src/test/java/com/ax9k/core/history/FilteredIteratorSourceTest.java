package com.ax9k.core.history;

import com.ax9k.core.event.Event;
import com.ax9k.util.EqualsAndHashCodeSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import static com.ax9k.core.marketmodel.MockEvent.listWithMillisAfterEpoch;
import static com.ax9k.core.marketmodel.MockEvent.wrapMillisSinceEpoch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilteredIteratorSourceTest {
    private static final Predicate<Event> TIMESTAMP_IS_EVEN = timestamped ->
            timestamped.getTimestamp().toEpochMilli() % 2 == 0;

    private FilteredIteratorSource<Event> testSource;

    @Nested
    class WhenContainingValidValues {
        @BeforeEach
        void filterAwayOddTimestamps() {
            testSource = new FilteredIteratorSource<>(
                    listWithMillisAfterEpoch(1, 2, 3, 4, 5, 6, 7, 8, 9),
                    2,
                    TIMESTAMP_IS_EVEN
            );
        }

        @Test
        void shouldOnlyIterateOverValidValues() {
            List<Event> expected = listWithMillisAfterEpoch(4, 6, 8);

            assertIterableEquals(expected, testSource);
        }

        @Test
        void shouldNotReturnInvalidEarliestValue() {
            assertEquals(wrapMillisSinceEpoch(4), testSource.getEarliest().orElseThrow());
        }

        @Test
        void shouldNotReturnInvalidLatestValue() {
            assertEquals(wrapMillisSinceEpoch(8), testSource.getLatest().orElseThrow());
        }

        @Test
        void sizeShouldOnlyCountValidValues() {
            assertEquals(3, testSource.getSize());
        }
    }

    @Nested
    class WhenContainingNoValidValues {
        @BeforeEach
        void filterAwayAllValues() {
            testSource = new FilteredIteratorSource<>(
                    listWithMillisAfterEpoch(1, 2, 3, 4, 5, 6, 7, 8, 9),
                    2,
                    timestamped -> false
            );
        }

        @Test
        void iteratorShouldBeEmpty() {
            assertFalse(testSource.iterator().hasNext());
        }

        @Test
        void sizeShouldBeZero() {
            assertEquals(0, testSource.getSize());
        }

        @Test
        void durationShouldBeZero() {
            assertEquals(Duration.ZERO, testSource.getDuration());
        }

        @Test
        void shouldNotHaveEarliestValue() {
            assertFalse(testSource.getEarliest().isPresent());
        }

        @Test
        void shouldNotHaveLatestValue() {
            assertFalse(testSource.getLatest().isPresent());
        }
    }

    @Nested
    class WhenContainingOneValidValue {
        @BeforeEach
        void filterAllButOneValue() {
            testSource = new FilteredIteratorSource<>(
                    listWithMillisAfterEpoch(1, 2, 3, 4, 5, 6, 7, 8, 9),
                    0,
                    timestamped -> timestamped.getTimestamp().toEpochMilli() == 5
            );
        }

        @Test
        void iteratorShouldHaveOneElement() {
            Iterator<?> iterator = testSource.iterator();

            assertTrue(iterator.hasNext());
            iterator.next();
            assertFalse(iterator.hasNext());
        }

        @Test
        void sizeShouldBeOne() {
            assertEquals(1, testSource.getSize());
        }

        @Test
        void shouldHaveSameEarliestAndLatestValues() {
            Object earliest = testSource.getEarliest().orElseThrow();
            Object latest = testSource.getLatest().orElseThrow();

            assertSame(earliest, latest);
        }
    }

    @Nested
    class WhenEqualsAndHashcodeMethodsCalled implements EqualsAndHashCodeSpecification<FilteredIteratorSource> {
        @Override
        public FilteredIteratorSource getSecondEqualValue() {
            return new FilteredIteratorSource<>(
                    listWithMillisAfterEpoch(1, 2, 3, 4),
                    0,
                    timestamped -> true
            );
        }

        @Override
        public FilteredIteratorSource getValue() {
            return new FilteredIteratorSource<>(
                    listWithMillisAfterEpoch(1, 2, 3, 4),
                    0,
                    timestamped -> true
            );
        }

        @Override
        public FilteredIteratorSource getEqualValue() {
            return new FilteredIteratorSource<>(
                    listWithMillisAfterEpoch(1, 2, 3, 4),
                    0,
                    timestamped -> true
            );
        }

        @Override
        public FilteredIteratorSource getUnequalValue() {
            return new FilteredIteratorSource<>(
                    listWithMillisAfterEpoch(1, 2, 3, 4),
                    1,
                    timestamped -> true
            );
        }
    }
}