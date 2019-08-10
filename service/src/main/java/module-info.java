module com.ax9k.service {
    requires transitive com.ax9k.core;
    requires transitive com.ax9k.positionmanager;
    requires transitive com.ax9k.provider;
    requires transitive com.ax9k.broker;
    requires transitive com.ax9k.algo;

    requires com.ax9k.utils;

    requires spark.core;

    requires org.apache.logging.log4j;

    exports com.ax9k.service;
}