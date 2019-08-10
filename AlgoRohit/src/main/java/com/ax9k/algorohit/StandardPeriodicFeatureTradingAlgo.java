package com.ax9k.algorohit;

import com.ax9k.algo.trading.PeriodicFeatureTradingAlgo;
import com.ax9k.algo.trading.PeriodicSignalChangeStrategy;
import com.ax9k.algo.trading.TradingStrategy;
import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.positionmanager.PositionManager;
import org.apache.logging.log4j.Logger;

public class StandardPeriodicFeatureTradingAlgo extends PeriodicFeatureTradingAlgo {
    private final PeriodicSignalChangeStrategy signalChangeStrategy;

    StandardPeriodicFeatureTradingAlgo(PositionManager positionManager,
                                       TradingDay tradingDay,
                                       Logger algoLogger,
                                       PeriodicSignalChangeStrategy signalChangeStrategy,
                                       TradingStrategy tradingStrategy,
                                       int stopBuffer,
                                       double cashoutUpperLevel,
                                       double cashoutLowerLevel,
                                       double maxGiveBack) {
        super("0.1",
              positionManager,
              tradingDay,
              algoLogger,
              tradingStrategy);
        this.stopBuffer = stopBuffer;
        this.cashoutLowerLevel = cashoutLowerLevel;
        this.cashoutUpperLevel = cashoutUpperLevel;
        this.maxGiveBack = maxGiveBack;
        this.signalChangeStrategy = signalChangeStrategy;

        registerSignalChangeStrategy(signalChangeStrategy);
    }

    @Override
    public boolean shouldRunPeriodicUpdates(Phase phase, EventType triggeringEventType) {
        return super.shouldRunPeriodicUpdates(phase, triggeringEventType) &&
               signalChangeStrategy.shouldAttemptSignalChange(triggeringEventType);
    }

    public PeriodicSignalChangeStrategy getSignalChangeStrategy() {
        return signalChangeStrategy;
    }
}
