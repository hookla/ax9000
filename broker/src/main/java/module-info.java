module com.ax9k.broker {
    requires com.ax9k.utils;
    requires com.ax9k.core;
    requires org.apache.commons.lang3;
    exports com.ax9k.broker;

    opens com.ax9k.broker to com.fasterxml.jackson.databind;
}