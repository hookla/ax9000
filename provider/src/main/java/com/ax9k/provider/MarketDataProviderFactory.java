package com.ax9k.provider;

import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.utils.config.Configuration;

public interface MarketDataProviderFactory {
    MarketDataProvider create(TradingDay tradingDay, Configuration configuration);
}
