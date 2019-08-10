package com.ax9k.backtesting;

import com.ax9k.core.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventReplayTest {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final Instant[] ALL_EVENTS;
    private static final Collector<String, StringBuilder, BufferedReader> TO_BUFFERED_READER = Collector.of(
            StringBuilder::new,
            (lines, line) -> lines.append(line).append(LINE_SEPARATOR),
            StringBuilder::append,
            lines -> new BufferedReader(new StringReader(lines.toString()))
    );

    static {
        ALL_EVENTS = epochMilliRange(10);
    }

    private EventReplay<MockEvent> testReplayer;
    private List<MockEvent> replayedEvents;

    private static Instant[] epochMilliRange(int endInclusive) {
        return IntStream.rangeClosed(1, endInclusive)
                        .mapToObj(Instant::ofEpochMilli)
                        .toArray(Instant[]::new);
    }

    private static Instant[] toTimestampArray(List<MockEvent> events) {
        return events.stream()
                     .map(MockEvent::getTimestamp)
                     .toArray(Instant[]::new);
    }

    private static Instant millisAfterEpoch(int millis) {
        return Instant.ofEpochMilli(millis);
    }

    @BeforeEach
    void initialiseTestEnvironment() {
        replayedEvents = new ArrayList<>();
        BufferedReader reader = readingTimestamps(ALL_EVENTS);

        testReplayer = EventReplay.ofSingleLineEvents(MockEvent::parse, reader);
    }

    private static BufferedReader readingTimestamps(Instant... timestamps) {
        return Stream.of(timestamps)
                     .map(Instant::toString)
                     .collect(TO_BUFFERED_READER);
    }

    private static final class MockEvent implements Event {
        private final Instant timestamp;

        private MockEvent(Instant timestamp) {this.timestamp = timestamp;}

        private static MockEvent parse(String text) {
            return new MockEvent(Instant.parse(text));
        }

        @Override
        public Instant getTimestamp() {
            return timestamp;
        }
    }

    @Nested
    class WhenCurrentTimestampAfterSingleEvent {
        @BeforeEach
        void replayUntilCurrentTimestamp() {
            Instant currentTimestamp = millisAfterEpoch(1);
            replayedEvents = testReplayer.replayUntil(currentTimestamp);
        }

        @Test
        void shouldHaveMoreEvents() {
            assertTrue(testReplayer.hasMoreEvents());
        }

        @Test
        void shouldSendOnlyOneEvent() {
            assertEquals(1, replayedEvents.size());
        }

        @Test
        void shouldSendCorrectEvent() {
            assertEquals(millisAfterEpoch(1), replayedEvents.get(0).getTimestamp());
        }
    }

    @Nested
    class WhenCurrentTimestampAfterMultipleEvents {
        private final Instant[] EXPECTED_EVENTS = epochMilliRange(5);

        @BeforeEach
        void replayUntilCurrentTimestamp() {
            Instant currentTimestamp = millisAfterEpoch(EXPECTED_EVENTS.length);
            replayedEvents = testReplayer.replayUntil(currentTimestamp);
        }

        @Test
        void shouldHaveMoreEvents() {
            assertTrue(testReplayer.hasMoreEvents());
        }

        @Test
        void shouldSendCorrectNumberOfEvents() {
            assertEquals(EXPECTED_EVENTS.length, replayedEvents.size());
        }

        @Test
        void shouldSendCorrectEvents() {
            Instant[] replayedEventTimestamps = toTimestampArray(replayedEvents);

            assertArrayEquals(EXPECTED_EVENTS, replayedEventTimestamps);
        }
    }

    @Nested
    class WhenCurrentTimestampAfterAllEvents {
        @BeforeEach
        void replayUntilCurrentTimestamp() {
            Instant currentTimestamp = Instant.MAX.minusSeconds(1);
            replayedEvents = testReplayer.replayUntil(currentTimestamp);
        }

        @Test
        void shouldHaveNoMoreEvents() {
            assertFalse(testReplayer.hasMoreEvents());
        }

        @Test
        void shouldSendCorrectNumberOfEvents() {
            assertEquals(ALL_EVENTS.length, replayedEvents.size());
        }

        @Test
        void shouldSendCorrectEvents() {
            Instant[] replayedEventTimestamps = toTimestampArray(replayedEvents);

            assertArrayEquals(ALL_EVENTS, replayedEventTimestamps);
        }

        @Test
        void shouldNotSendAnyMoreEventsWhenReplayedAgainAtOnce() {
            List<MockEvent> newEvents = testReplayer.replayAll();

            assertEquals(0, newEvents.size());
        }

        @Test
        void shouldNotSendAnyMoreEventsWhenReplayedAgainByTimestamp() {
            List<MockEvent> newEvents = testReplayer.replayUntil(Instant.MAX);

            assertEquals(0, newEvents.size());
        }
    }

    @Nested
    class WhenCurrentTimestampBeforeAllEvents {
        @BeforeEach
        void replayUntilCurrentTimestamp() {
            Instant currentTimestamp = millisAfterEpoch(0);
            replayedEvents = testReplayer.replayUntil(currentTimestamp);
        }

        @Test
        void shouldHaveMoreEvents() {
            assertTrue(testReplayer.hasMoreEvents());
        }

        @Test
        void shouldSendNoEvents() {
            assertEquals(0, replayedEvents.size());
        }
    }

    @Nested
    class WhenReplayingEventsWithADelay {
        private final Instant[] EXPECTED_EVENTS = epochMilliRange(9);
        private final Duration delay = Duration.ofMillis(1);

        @BeforeEach
        void replayUntilCurrentTimestamp() {
            BufferedReader reader = readingTimestamps(ALL_EVENTS);
            testReplayer = EventReplay.ofSingleLineEvents(MockEvent::parse, reader, delay);
            Instant currentTimestamp = millisAfterEpoch(ALL_EVENTS.length);
            replayedEvents = testReplayer.replayUntil(currentTimestamp);
        }

        @Test
        void shouldHaveMoreEvents() {
            assertTrue(testReplayer.hasMoreEvents());
        }

        @Test
        void shouldSendCorrectNumberOfEvents() {
            assertEquals(EXPECTED_EVENTS.length, replayedEvents.size());
        }

        @Test
        void shouldSendCorrectEvents() {
            Instant[] replayedEventTimestamps = toTimestampArray(replayedEvents);

            assertArrayEquals(EXPECTED_EVENTS, replayedEventTimestamps);
        }
    }

    @Nested
    class WhenReplayingAllEventsAtOnce {
        @BeforeEach
        void replayAllEvents() {
            replayedEvents = testReplayer.replayAll();
        }

        @Test
        void shouldHaveNoMoreEvents() {
            assertFalse(testReplayer.hasMoreEvents());
        }

        @Test
        void shouldSendCorrectNumberOfEvents() {
            assertEquals(ALL_EVENTS.length, replayedEvents.size());
        }

        @Test
        void shouldSendCorrectEvents() {
            Instant[] replayedEventTimestamps = toTimestampArray(replayedEvents);

            assertArrayEquals(ALL_EVENTS, replayedEventTimestamps);
        }

        @Test
        void shouldNotSendAnyMoreEventsWhenReplayedAgainAtOnce() {
            List<MockEvent> newEvents = testReplayer.replayAll();

            assertEquals(0, newEvents.size());
        }

        @Test
        void shouldNotSendAnyMoreEventsWhenReplayedAgainByTimestamp() {
            List<MockEvent> newEvents = testReplayer.replayUntil(Instant.MAX);

            assertEquals(0, newEvents.size());
        }
    }
}