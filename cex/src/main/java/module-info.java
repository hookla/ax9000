module com.ax9k.cex {
    requires transitive com.ax9k.broker;
    requires transitive com.ax9k.provider;

    requires com.ax9k.backtesting;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires org.apache.logging.log4j;
    requires org.apache.commons.lang3;
    requires org.apache.commons.codec;
    requires Java.WebSocket;

    opens com.ax9k.cex.client to com.fasterxml.jackson.databind;
    opens com.ax9k.cex.broker to com.fasterxml.jackson.databind;
    opens com.ax9k.cex.provider to com.fasterxml.jackson.databind;

    exports com.ax9k.cex.data;

    provides com.ax9k.broker.BrokerFactory with com.ax9k.cex.broker.CexBrokerFactory;
    provides com.ax9k.provider.MarketDataProviderFactory with com.ax9k.cex.provider.CexMarketDataProviderFactory;
}