package com.ax9k.core.history;

import com.ax9k.core.event.Event;
import com.ax9k.core.time.Time;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;

public class BasicHistory<T extends Event> implements History<T> {
    private static final Source EMPTY = new EmptySource<>();
    private static final Duration HISTORY_WINDOW = Duration.ofHours(1).plusMinutes(30);
    private static final int MAX_HISTORY_SIZE = 600_000;
    private static final int MAX_CACHED_INDICES = 25;
    private final Map<String, Integer> indexCache;
    private List<T> history;
    private Optional<T> latest = empty();
    private Optional<T> previous = empty();
    private Instant lastCachedTimestamp;

    public BasicHistory() {
        history = new ArrayList<>(MAX_HISTORY_SIZE);
        indexCache = new HashMap<>(MAX_CACHED_INDICES);
    }

    @Override
    public Iterator<T> iterator() {
        return asCollection().iterator();
    }

    @Override
    public Collection<T> asCollection() {
        return Collections.unmodifiableCollection(history);
    }

    @Override
    public void record(T datum) {
        history.add(requireNonNull(datum));
        previous = latest;
        latest = Optional.of(datum);
        pruneOldHistory(datum);
    }

    private void pruneOldHistory(T latest) {
        if (history.size() < MAX_HISTORY_SIZE) {
            return;
        }
        int index = indexOfFirstValidDatum(latest);
        history = removeBeforeIndex(index);
    }

    private int indexOfFirstValidDatum(T latest) {
        Instant lastTimestamp = Time.now();
        Instant earliestAllowedTimestamp = lastTimestamp.minus(HISTORY_WINDOW);

        if (!latest.getTimestamp().isAfter(earliestAllowedTimestamp)) {
            return highestValidIndex();
        }

        for (int i = 0; i < history.size(); i++) {
            Instant currentTimestamp = history.get(i).getTimestamp();
            if (!currentTimestamp.isBefore(earliestAllowedTimestamp)) {
                return i;
            }
            //TODO consider doing something other than a linear search here
        }

        throw new IllegalStateException("could not find first valid index");
    }

    private int highestValidIndex() {
        return history.size() - 1;
    }

    private List<T> removeBeforeIndex(int index) {
        return new ArrayList<>(history.subList(index, history.size()));
    }

    @Override
    public Optional<T> getLatest() {
        return latest;
    }

    @Override
    public boolean isEmpty() {
        return history.isEmpty();
    }

    @Override
    public Optional<T> getPrevious() {
        return previous;
    }

    @Override
    public Source<T> asSource() {
        if (history.isEmpty()) {
            return emptySource();
        }
        return new IteratorSource<>(history, 0);
    }

    @SuppressWarnings("unchecked")
    private Source<T> emptySource() {
        return (Source<T>) EMPTY;
    }

    @Override
    public Source<T> asSource(Duration relevantPeriod) {
        if (history.isEmpty()) {
            return new EmptySource<>(relevantPeriod);
        }
        requireNonNull(relevantPeriod, "relevantPeriod");

        int index = listIndexForFilter(relevantPeriod);
        if (index > highestValidIndex()) {
            return new EmptySource<>(relevantPeriod);
        }

        return new IteratorSource<>(history, index, relevantPeriod);
    }

    private int listIndexForFilter(Duration relevantPeriod) {
        Instant latestTimestamp = Time.now();
        Instant earliestTimestamp = latestTimestamp.minus(relevantPeriod);
        if (isLessThanOrEqualToFirstElementTimestamp(earliestTimestamp)) {
            return 0;
        }

        String key;
        if (lastCachedTimestamp != null &&
            lastCachedTimestamp.equals(latestTimestamp) &&
            indexCache.containsKey(key = calculateKey(relevantPeriod, lastCachedTimestamp))) {
            return indexCache.get(key);
        }
        if (indexCache.size() > MAX_CACHED_INDICES) { indexCache.clear(); }
        int index = findEarliestValidIndex(earliestTimestamp);

        lastCachedTimestamp = latestTimestamp;
        indexCache.put(calculateKey(relevantPeriod, lastCachedTimestamp), index);
        return index;
    }

    private boolean isLessThanOrEqualToFirstElementTimestamp(Instant timestamp) {
        Instant firstTimestamp = history.get(0).getTimestamp();

        return timestamp.compareTo(firstTimestamp) <= 0;
    }

    private String calculateKey(Duration duration, Instant lastCachedTimestamp) {
        return lastCachedTimestamp.toString().concat(duration.toString());
    }

    private int findEarliestValidIndex(Instant earliestTimestamp) {
        for (int i = highestValidIndex(); i >= 0; i--) {
            T datum = history.get(i);
            /*
            This implementation includes elements at the earliest timestamp.
            For example, given a BasicHistory with Event objects of (in ms)
            [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]. Calling asSource with a Duration
            of 3ms will return a Source over the elements with timestamps of
            7, 8, 9, and 10 ms.
             */
            if (datum.getTimestamp().isBefore(earliestTimestamp)) {
                return i + 1;
            }
        }
        return 0;
    }

    @Override
    public Source<T> asSource(int numberOfEntries) {
        if (numberOfEntries == 0 || history.isEmpty()) {
            return new EmptySource<>(numberOfEntries);
        }

        requirePositive(numberOfEntries);

        int actualSize = Math.min(numberOfEntries, history.size());
        int index = history.size() - actualSize;

        return new IteratorSource<>(history, index, numberOfEntries);
    }

    private static void requirePositive(int numberOfEntries) {
        if (numberOfEntries < 0) {
            throw new IllegalArgumentException("numberOfEntries < 0");
        }
    }

    @Override
    public Source<T> asSource(Duration relevantPeriod, Predicate<T> filter) {
        if (history.isEmpty()) {
            return new EmptySource<>(relevantPeriod);
        }

        requireNonNull(relevantPeriod, "relevantPeriod");
        requireNonNull(filter, "filter");

        int index = listIndexForFilter(relevantPeriod);
        if (index > highestValidIndex()) {
            return new EmptySource<>(relevantPeriod);
        }

        return new FilteredIteratorSource<>(history, index, relevantPeriod, filter);
    }

    @Override
    public Source<T> asSource(int numberOfEntries, Predicate<T> filter) {
        if (numberOfEntries == 0 || history.isEmpty()) {
            return new EmptySource<>(numberOfEntries);
        }

        requirePositive(numberOfEntries);
        requireNonNull(filter, "filter");

        int actualSize = Math.min(numberOfEntries, history.size());
        int index = history.size() - actualSize;
        return new FilteredIteratorSource<>(history, index, filter, numberOfEntries);
    }

    @Override
    public Stream<T> stream() {
        return history.stream();
    }

    public int getSize() {
        return history.size();
    }

    @Override
    public int hashCode() {
        int result;
        result = Objects.hashCode(latest);
        result = 31 * result + Objects.hashCode(previous);
        result = 31 * result + Objects.hashCode(history);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        BasicHistory<?> that = (BasicHistory<?>) other;
        return Objects.equals(latest, that.latest) &&
               Objects.equals(previous, that.previous) &&
               Objects.equals(history, that.history);
    }

    @Override
    public String toString() {
        return "BasicHistory[" +
               "history=" + history +
               ']';
    }
}
