module com.ax9k.positionmanager {
    requires com.ax9k.core;
    requires com.ax9k.broker;
    requires com.ax9k.utils;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.annotation;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j.core;
    exports com.ax9k.positionmanager;
    opens com.ax9k.positionmanager to com.fasterxml.jackson.databind;
}