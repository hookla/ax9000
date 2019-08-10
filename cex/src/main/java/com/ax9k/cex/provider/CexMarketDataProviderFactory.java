package com.ax9k.cex.provider;

import com.ax9k.cex.client.CexClient;
import com.ax9k.cex.client.CexContract;
import com.ax9k.cex.client.ClientRegistry;
import com.ax9k.cex.client.Pair;
import com.ax9k.cex.client.SignatureGenerator;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.time.Time;
import com.ax9k.provider.MarketDataProvider;
import com.ax9k.provider.MarketDataProviderFactory;
import com.ax9k.utils.config.Configuration;

public class CexMarketDataProviderFactory implements MarketDataProviderFactory {
    @Override
    public MarketDataProvider create(TradingDay tradingDay, Configuration configuration) {
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

        return new CexMarketDataProvider(tradingDay, client, contract);
    }
}
