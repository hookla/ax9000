package com.ax9k.algo.features.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ResultStore {
    private static final String DEFAULT_FEATURE_RESULT_KEY = "FEATURE_RESULT";

    private final Map<String, Object> cache = new HashMap<>();

    public Optional<Double> getPreviousResult() {
        return get(DEFAULT_FEATURE_RESULT_KEY);
    }

    public Optional<Double> get(String key) {
        return get(key, Double.class);
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        return Optional.ofNullable(cache.get(key)).map(type::cast);
    }

    public void storeFeatureResult(double value) {
        store(DEFAULT_FEATURE_RESULT_KEY, value);
    }

    public void store(String key, Object value) {
        cache.put(key, value);
    }

    public boolean hasPreviousResult() {
        return contains(DEFAULT_FEATURE_RESULT_KEY);
    }

    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    public void discard(String key) {
        cache.remove(key);
    }
}
