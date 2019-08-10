package com.ax9k.algo.features;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Parameters {
    public static final Parameters NONE = new Parameters(Map.of());

    private final Map<String, Object> delegate;

    private Parameters(Map<String, Object> delegate) {
        this.delegate = delegate;
    }

    public static Parameters params(String param1, Object value1) {
        return new Parameters(Map.of(param1, value1));
    }

    public static Parameters params(String param1, Object value1, String param2, Object value2) {
        return new Parameters(Map.of(param1, value1, param2, value2));
    }

    @SafeVarargs
    public static Parameters params(Map.Entry<String, Object>... parameters) {
        return new Parameters(Map.ofEntries(parameters));
    }

    public Optional<Double> getDouble(String key) {
        return getString(key).map(Double::valueOf);
    }

    public Optional<String> getString(String key) {
        return get(key).map(String::valueOf);
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(delegate.get(key));
    }

    public Optional<Integer> getInt(String key) {
        return getString(key).map(Integer::valueOf);
    }

    @Override
    public int hashCode() {
        return 31 * delegate.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        Parameters that = (Parameters) other;
        return Objects.equals(delegate, that.delegate);
    }

    @Override
    public String toString() {
        return "Parameters: " + delegate.keySet().toString();
    }
}