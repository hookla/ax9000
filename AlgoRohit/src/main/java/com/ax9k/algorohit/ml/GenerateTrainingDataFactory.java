package com.ax9k.algorohit.ml;

import com.ax9k.algo.Algo;
import com.ax9k.algo.AlgoFactory;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.positionmanager.PositionManager;
import com.ax9k.utils.config.Configuration;

public class GenerateTrainingDataFactory implements AlgoFactory {
    @Override
    public Algo create(PositionManager positionManager,
                       TradingDay tradingDay,
                       Configuration configuration) {
        return new GenerateTrainingData(
                positionManager,
                tradingDay, 60);
    }
}
