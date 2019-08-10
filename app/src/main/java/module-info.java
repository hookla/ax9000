module com.ax9k.app {
    requires org.apache.logging.log4j;
    requires org.apache.commons.lang3;
    requires commons.cli;
    requires com.ax9k.core;
    requires com.ax9k.algo;
    requires com.ax9k.positionmanager;
    requires com.ax9k.broker;
    requires com.ax9k.backtesting;
    requires com.ax9k.provider;
    requires com.ax9k.service;
    requires com.ax9k.training;

    /* Unmodularised AWS Java SDK artifacts */
    requires s3;

    requires java.sql;
    requires com.ax9k.utils;
    requires org.apache.logging.log4j.core;
    requires com.fasterxml.jackson.annotation;

    opens com.ax9k.app.contract to com.fasterxml.jackson.databind;

    uses com.ax9k.algo.AlgoFactory;
    uses com.ax9k.provider.MarketDataProviderFactory;
    uses com.ax9k.broker.BrokerFactory;
}