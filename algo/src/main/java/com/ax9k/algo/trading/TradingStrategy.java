package com.ax9k.algo.trading;

import com.ax9k.positionmanager.Position;

public interface TradingStrategy {
    TradeDirective decideAction(SignalContext signal, Position position);

    boolean isTimeToExit();

    double getQuantityMultiplier();
}
