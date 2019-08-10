package com.ax9k.backtesting;

import com.ax9k.core.event.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.apache.commons.lang3.Validate.notNull;

public final class EventReplay<T extends Event> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Function<BufferedReader, T> parser;
    private final BufferedReader lines;
    private final long delayNanos;

    private T next;

    private T lastEvent;

    private EventReplay(Function<BufferedReader, T> parser, BufferedReader lines, Duration replayTimeDelay) {
        this.parser = notNull(parser, "EventReplay parser");
        this.lines = notNull(lines, "EventReplay lines");
        this.delayNanos = notNull(replayTimeDelay, "EventReplay replayTimeDelay").toNanos();

        next = readNext();
    }

    public static <T extends Event> EventReplay<T> of(Function<BufferedReader, T> parser, BufferedReader lines) {
        return new EventReplay<>(parser, lines, Duration.ZERO);
    }

    public static <T extends Event> EventReplay<T> of(Function<BufferedReader, T> parser,
                                                      BufferedReader lines,
                                                      Duration replayTimeDelay) {
        return new EventReplay<>(parser, lines, replayTimeDelay);
    }

    public static <T extends Event> EventReplay<T> ofSingleLineEvents(Function<String, T> parser,
                                                                      BufferedReader lines) {
        return new EventReplay<>(singleLineParser(parser), lines, Duration.ZERO);
    }

    private static <T> Function<BufferedReader, T> singleLineParser(Function<String, T> parser) {
        return reader -> {
            try {
                return parser.apply(reader.readLine());
            } catch (IOException readError) {
                throw new UncheckedIOException("Could not read input lines", readError);
            }
        };
    }

    public static <T extends Event> EventReplay<T> ofSingleLineEvents(Function<String, T> parser,
                                                                      BufferedReader lines,
                                                                      Duration replayTimeDelay) {
        return new EventReplay<>(singleLineParser(parser), lines, replayTimeDelay);
    }

    private static <T extends Comparable<T>> boolean greaterOrEqualTo(T one, T two) {
        return one.compareTo(two) >= 0;
    }

    private T readNext() {
        try {
            lines.mark(10_000);
            String nextLine = lines.readLine();
            if (nextLine == null) {
                return null;
            }
            lines.reset();

            try {
                return parser.apply(lines);
            } catch (IllegalStateException | IllegalArgumentException | NullPointerException invalidLine) {
                LOGGER.warn(invalidLine.getMessage());
                return readNext();
            }
        } catch (IOException readError) {
            throw new UncheckedIOException("Could not read input lines", readError);
        }
    }

    public List<T> replayAll() {
        List<T> result = new ArrayList<>();
        while (hasMoreEvents()) {
            result.add(next);
            next = readNext();
        }
        return result;
    }

    public boolean hasMoreEvents() {
        return next != null;
    }

    public List<T> replayUntil(Instant currentTimestamp) {
        List<T> result = new ArrayList<>();
        while (hasMoreEvents() && greaterOrEqualTo(currentTimestamp, offset(nextTimestamp()))) {
            result.add(next);
            next = readNext();
        }
        return result;
    }

    private Instant offset(Instant timestamp) {
        return timestamp.plusNanos(delayNanos);
    }

    private Instant nextTimestamp() {
        return next.getTimestamp();
    }
}
