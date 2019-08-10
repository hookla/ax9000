package com.ax9k.broker;

import com.ax9k.core.marketmodel.Contract;
import com.ax9k.utils.config.Configuration;

public interface BrokerFactory {
    Broker create(Contract contract, BrokerCallbackReceiver receiver, Configuration configuration);
}
