package com.ax9k.algorohit.ml;

import com.ax9k.algo.Algo;
import com.ax9k.algo.AlgoFactory;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.positionmanager.PositionManager;
import com.ax9k.utils.config.Configuration;

public class AmazonMLAlgoFactory implements AlgoFactory {
    @Override
    public Algo create(PositionManager positionManager, TradingDay tradingDay, Configuration configuration) {
        configuration.requireOptions("modelId", "stopBuffer", "periodicUpdateDurationSeconds");

        String modelId = configuration.get("modelId");

        int periodicUpdateDurationSeconds = configuration.get("periodicUpdateDurationSeconds", Integer.class);
        int stopBuffer = configuration.get("stopBuffer", Integer.class);

        double entryThreshold = configuration.getOptional("entryThreshold", .65);
        double exitThreshold = configuration.getOptional("exitThreshold", .5);

        return new AmazonMLAlgo(
                positionManager,
                tradingDay,
                modelId,
                stopBuffer,
                periodicUpdateDurationSeconds,
                entryThreshold,
                exitThreshold
        );
    }
}
