package com.ax9k.positionmanager;

import com.ax9k.broker.BrokerCallbackReceiver;

public interface PositionManager {

    BrokerCallbackReceiver getBrokerCallbackReceiver();

    OrderReceiver getOrderReceiver();

    PositionReporter getPositionReporter();

    MarketDataProviderCallbackReceiver getMarketDataProviderCallbackReceiver();
}
