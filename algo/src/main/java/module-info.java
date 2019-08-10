module com.ax9k.algo {
    requires transitive com.ax9k.core;
    requires transitive com.ax9k.positionmanager;
    requires transitive com.ax9k.utils;

    requires org.apache.logging.log4j;
    requires org.apache.commons.lang3;

    requires com.fasterxml.jackson.annotation;
    requires ta.lib;

    exports com.ax9k.algo;
    exports com.ax9k.algo.features;
    exports com.ax9k.algo.features.set;
    exports com.ax9k.algo.trading;

    provides com.ax9k.algo.AlgoFactory with com.ax9k.algo.DoNothingAlgoFactory;

}