package com.ax9k.cex.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {
    private static final Map<Pair, CexClient> registry = new ConcurrentHashMap<>();

    public static void register(Pair pair, CexClient client) {
        registry.put(pair, client);
    }

    public static CexClient get(Pair pair) {
        return registry.get(pair);
    }
}
