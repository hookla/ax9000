package com.ax9k.interactivebrokers.broker;

import com.ax9k.broker.Broker;
import com.ax9k.broker.BrokerCallbackReceiver;
import com.ax9k.broker.BrokerFactory;
import com.ax9k.core.marketmodel.Contract;
import com.ax9k.utils.config.Configuration;

public class IbBrokerFactory implements BrokerFactory {
    @Override
    public Broker create(Contract contract, BrokerCallbackReceiver receiver, Configuration configuration) {
        configuration.requireOptions("contractId", "exchange", "gateway", "clientId");

        String exchange = configuration.get("exchange");
        int contractId = configuration.get("contractId", Integer.class);

        String[] gateway = configuration.get("gateway").split(":");
        if (gateway.length == 1) {
            throw new IllegalArgumentException("gateway must have both a IPv4 address and a port");
        }

        String address = gateway[0];
        int port = Integer.parseInt(gateway[1]);

        int clientId = configuration.get("clientId", Integer.class);

        return new IbBroker(receiver,
                            contract,
                            address,
                            port,
                            exchange,
                            contractId,
                            clientId);
    }
}
