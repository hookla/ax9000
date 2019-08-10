module com.ax9k.training {
    requires commons.cli;

    requires com.ax9k.utils;

    requires org.apache.logging.log4j;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    exports com.ax9k.training;

    opens com.ax9k.training to com.fasterxml.jackson.databind;
}