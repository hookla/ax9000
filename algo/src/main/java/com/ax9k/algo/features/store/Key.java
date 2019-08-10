package com.ax9k.algo.features.store;

import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.Parameters;
import com.ax9k.core.event.Event;
import com.ax9k.core.history.Source;

import static java.util.Objects.requireNonNull;

public class Key implements Comparable<Key> {
    private final String value;

    private Key(String value) {
        this.value = requireNonNull(value, "Key value");
    }

    public static <T extends Event> Key create(Feature<T> feature, Source<T> history, Parameters parameters) {
        int periods = history.getIntendedSize().orElse(history.getSize());
        return new Key(feature.toString() + periods + parameters);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) { return true; }
        if (object == null || getClass() != object.getClass()) { return false; }
        Key that = (Key) object;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return 37 * 17 + value.hashCode();
    }

    @Override
    public int compareTo(Key that) {
        requireNonNull(that, "compareTo that");
        return value.compareTo(that.value);
    }
}
