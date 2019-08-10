package com.ax9k.algo;

import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.positionmanager.PositionManager;
import com.ax9k.utils.config.Configuration;

import java.util.Collection;

public interface AlgoFactory {
    default Algo create(PositionManager positionManager,
                        TradingDay tradingDay,
                        Configuration configuration,
                        Collection<Class<?>> extraDataTypes) {
        return create(positionManager, tradingDay, configuration);
    }

    Algo create(PositionManager positionManager, TradingDay tradingDay, Configuration configuration);
}
