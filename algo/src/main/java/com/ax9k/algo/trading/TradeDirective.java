package com.ax9k.algo.trading;

import java.util.function.Consumer;

@FunctionalInterface
public interface TradeDirective extends Consumer<TradingAlgo> {
    @Override
    default void accept(TradingAlgo algo) {
        execute(algo);
    }

    void execute(TradingAlgo algo);
}
