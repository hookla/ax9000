package com.ax9k.algo.trading;

import com.ax9k.positionmanager.Position;

import static com.ax9k.algo.trading.Directives.none;

public enum NullTradingStrategy implements TradingStrategy {
    INSTANCE;

    @Override
    public TradeDirective decideAction(SignalContext signal, Position position) {
        return none();
    }

    @Override
    public boolean isTimeToExit() {
        return false;
    }

    public double getQuantityMultiplier() {
        return 0;
    }
}
