package com.ax9k.algo;

import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.positionmanager.PositionManager;
import org.apache.logging.log4j.LogManager;

public class DoNothingAlgo extends Algo {

    DoNothingAlgo(PositionManager positionManager, TradingDay tradingDay) {
        super("0.1", positionManager, tradingDay, LogManager.getLogger("algoLogger"), false, false);
    }
}
