package com.ax9k.demo;

import com.ax9k.algo.Algo;
import com.ax9k.algo.AlgoFactory;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.positionmanager.PositionManager;
import com.ax9k.utils.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DemoAlgoFactory implements AlgoFactory {
    @Override
    public Algo create(PositionManager positionManager, TradingDay tradingDay, Configuration configuration) {
        boolean logToSlack = configuration.getOptional("logToSlack", false);
        String loggerName = logToSlack ? "algoLoggerWithSlack" : "algoLogger";
        Logger algoLogger = LogManager.getLogger(loggerName);

        return new DemoAlgo(positionManager, tradingDay, algoLogger);
    }
}
