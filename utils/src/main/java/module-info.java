import com.ax9k.utils.s3.S3UrlStreamHandlerFactory;

import java.net.spi.URLStreamHandlerProvider;

module com.ax9k.utils {
    exports com.ax9k.utils.math;
    exports com.ax9k.utils.compare;
    exports com.ax9k.utils.config;
    exports com.ax9k.utils.s3;
    exports com.ax9k.utils.path;
    exports com.ax9k.utils.json;
    exports com.ax9k.utils.logging;

    requires s3;
    requires core;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires org.apache.logging.log4j.core;
    requires org.apache.commons.lang3;
    requires unirest.java;

    opens com.ax9k.utils.json to com.fasterxml.jackson.databind;

    provides URLStreamHandlerProvider with S3UrlStreamHandlerFactory;
}