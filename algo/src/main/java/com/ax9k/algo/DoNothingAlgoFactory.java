package com.ax9k.algo;

import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.positionmanager.PositionManager;
import com.ax9k.utils.config.Configuration;

public class DoNothingAlgoFactory implements AlgoFactory {
    @Override
    public Algo create(PositionManager positionManager, TradingDay tradingDay, Configuration configuration) {
        return new DoNothingAlgo(positionManager, tradingDay);
    }
}
