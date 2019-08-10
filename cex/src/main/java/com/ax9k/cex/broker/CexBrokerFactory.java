package com.ax9k.cex.broker;

import com.ax9k.backtesting.AutoFillBroker;
import com.ax9k.broker.Broker;
import com.ax9k.broker.BrokerCallbackReceiver;
import com.ax9k.broker.BrokerFactory;
import com.ax9k.cex.client.CexClient;
import com.ax9k.cex.client.CexContract;
import com.ax9k.cex.client.ClientRegistry;
import com.ax9k.cex.client.Pair;
import com.ax9k.cex.client.SignatureGenerator;
import com.ax9k.core.marketmodel.Contract;
import com.ax9k.core.time.Time;
import com.ax9k.utils.config.Configuration;

public class CexBrokerFactory implements BrokerFactory {
    @Override
    public Broker create(Contract ignored, BrokerCallbackReceiver receiver, Configuration configuration) {
        Pair pair = configuration.get("pair", Pair::fromString);

        CexContract contract = new CexContract(pair);
        Time.setTradingSchedule(contract.getTradingSchedule());

        CexClient client = ClientRegistry.get(pair);
        if (client == null) {
            String apiKey = configuration.get("apiKey");
            byte[] secretKey = configuration.get("secretKey", byte[].class);
            SignatureGenerator signatureGenerator = new SignatureGenerator(apiKey, secretKey);

            client = new CexClient(signatureGenerator);
            ClientRegistry.register(pair, client);
        }

        if (configuration.getOptional("autoFill", false)) {
            int slippage = configuration.getOptional("slippage", 0);
            return new AutoFillBroker(contract, receiver, slippage);
        }

        return new CexBroker(client, contract, receiver);
    }
}
