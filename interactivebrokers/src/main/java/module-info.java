module com.ax9k.interactivebrokers {
    requires com.fasterxml.jackson.annotation;
    requires org.apache.logging.log4j;
    requires tws.api;
    requires transitive com.ax9k.broker;
    requires transitive com.ax9k.provider;
    requires org.apache.commons.lang3;

    exports com.ax9k.interactivebrokers.broker;
    exports com.ax9k.interactivebrokers.client to com.fasterxml.jackson.databind, com.ax9k.algorohit;
    exports com.ax9k.interactivebrokers.provider to com.fasterxml.jackson.databind;

    provides com.ax9k.provider.MarketDataProviderFactory with
            com.ax9k.interactivebrokers.provider.IbMarketDataProviderFactory;

    provides com.ax9k.broker.BrokerFactory with
            com.ax9k.interactivebrokers.broker.IbBrokerFactory;
}