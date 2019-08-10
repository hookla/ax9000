package com.ax9k.algo.features.store;

import java.util.Map;
import java.util.Optional;

public class ResultStoreRegistry {
    private static final int MAXIMUM_REGISTERED_CACHES = 100;

    private final Map<Key, ResultStore> registry = new LeastRecentlyUsedCache<>(MAXIMUM_REGISTERED_CACHES);

    public void register(Key key) {
        registry.put(key, new ResultStore());
    }

    public Optional<ResultStore> get(Key key) {
        return Optional.ofNullable(registry.get(key));
    }

    public ResultStore registerAndGet(Key key) {
        return registry.computeIfAbsent(key, __ -> new ResultStore());
    }
}
