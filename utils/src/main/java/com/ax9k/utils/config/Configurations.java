package com.ax9k.utils.config;

import com.ax9k.utils.path.PathLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public final class Configurations {
    private static final Logger LOGGER = LogManager.getLogger();

    private Configurations() {
        throw new AssertionError("Configurations is not instantiable");
    }

    public static Configuration empty() {
        return new PropertiesConfiguration(Collections.emptyMap());
    }

    public static Configuration load(Map<String, ?> properties) {
        return new PropertiesConfiguration(properties);
    }

    public static Configuration load(Properties properties) {
        return new PropertiesConfiguration(properties);
    }

    public static Configuration load(String fileLocation) {
        requireNonNull(fileLocation, "Configuration load fileLocation");
        ensureFileIsInSupportedFormat(fileLocation);

        LOGGER.info("Loading configuration file at: {}", fileLocation);

        Path localCopy = PathLoader.load(fileLocation);

        return loadFile(localCopy);
    }

    private static void ensureFileIsInSupportedFormat(String fileLocation) {
        if (!fileLocation.endsWith("properties")) {
            /* TODO We will probably need to support more formats in the future. */
            String extension = fileLocation.substring(fileLocation.lastIndexOf('.') + 1);
            throw new IllegalArgumentException(
                    format("Unsupported configuration file format '%s'. Supported formats: [properties]", extension)
            );
        }
    }

    private static Configuration loadFile(Path localFile) {
        try (InputStream stream = Files.newInputStream(localFile)) {
            Properties properties = new Properties();
            properties.load(stream);
            return new PropertiesConfiguration(properties);
        } catch (IOException e) {
            throw new UncheckedIOException("Error parsing properties file: " + localFile, e);
        }
    }
}
