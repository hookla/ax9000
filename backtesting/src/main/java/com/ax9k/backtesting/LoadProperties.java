package com.ax9k.backtesting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class LoadProperties {

    private static final Logger LOGGER = LogManager.getLogger();

    private LoadProperties() {
        throw new IllegalStateException("Utility class");
    }

    public static Properties getPropertiesFile() {
        Properties properties = new Properties();
        try (InputStream is = LoadProperties.class.getResourceAsStream("config.properties")) {
            properties.load(is);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return properties;
    }
}
