module com.ax9k.backtesting {
    requires transitive com.ax9k.core;
    requires transitive com.ax9k.broker;
    requires transitive com.ax9k.provider;
    requires com.ax9k.utils;

    requires com.fasterxml.jackson.core;
    requires org.apache.logging.log4j;
    requires org.apache.commons.lang3;
    requires com.fasterxml.jackson.databind;

    requires s3;

    exports com.ax9k.backtesting;

    provides com.ax9k.provider.MarketDataProviderFactory with
            com.ax9k.backtesting.FileLoaderFactory,
            com.ax9k.backtesting.LogRecyclerFactory;
}