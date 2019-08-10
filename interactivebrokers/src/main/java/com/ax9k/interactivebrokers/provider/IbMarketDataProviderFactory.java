package com.ax9k.interactivebrokers.provider;

import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.provider.MarketDataProvider;
import com.ax9k.provider.MarketDataProviderFactory;
import com.ax9k.utils.config.Configuration;

public class IbMarketDataProviderFactory implements MarketDataProviderFactory {
    @Override
    public MarketDataProvider create(TradingDay tradingDay, Configuration configuration) {
        configuration.requireOptions("contractId", "exchange", "gateway");

        int contractId = configuration.get("contractId", Integer.class);

        String[] gateway = configuration.get("gateway").split(":");
        if (gateway.length == 1) {
            throw new IllegalArgumentException("gateway must have both a IPv4 address and a port");
        }

        String address = gateway[0];
        int port = Integer.parseInt(gateway[1]);

        int clientId = configuration.get("clientId", Integer.class);
        String exchange = configuration.get("exchange");

        return new IbMarketDataProvider(tradingDay, address, port, contractId, exchange, clientId);
    }
}
