module com.ax9k.core {
    exports com.ax9k.core.marketmodel;
    exports com.ax9k.core.marketmodel.orderbook;
    exports com.ax9k.core.marketmodel.bar;
    exports com.ax9k.core.history;
    exports com.ax9k.core.time;
    exports com.ax9k.core.event;

    requires com.ax9k.utils;

    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.annotation;
    requires org.apache.logging.log4j.core;

    opens com.ax9k.core.marketmodel to com.fasterxml.jackson.databind;
    opens com.ax9k.core.marketmodel.bar to com.fasterxml.jackson.databind;
}