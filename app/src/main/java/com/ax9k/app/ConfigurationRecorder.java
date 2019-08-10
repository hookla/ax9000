package com.ax9k.app;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

class ConfigurationRecorder {
    private static final char NULL = Character.MIN_VALUE;

    static Path save(CommandLine arguments) {
        Properties fileContents = toPropertiesFileFormat(arguments);

        Path filePath = Paths.get(".", "temp", "run_configuration.properties");
        try (OutputStream stream = Files.newOutputStream(filePath, CREATE, WRITE, TRUNCATE_EXISTING)) {
            fileContents.store(stream, null);
        } catch (IOException e) {
            throw new UncheckedIOException("Error recording command-line arguments", e);
        }
        return filePath;
    }

    private static Properties toPropertiesFileFormat(CommandLine commandLine) {
        Properties result = new Properties();

        for (Option option : commandLine.getOptions()) {
            String identifier = getIdentifier(option);
            String[] values = option.getValues();
            if (isDynamicProperty(option)) {
                identifier = identifier.concat(values[0]);
                values = ArrayUtils.remove(values, 0);
            }

            String actualValue = toPropertyValue(values);
            if (!result.containsKey(identifier)) {
                result.put(identifier, actualValue);
            }
        }

        return result;
    }

    private static boolean isDynamicProperty(Option option) {
        return option.getLongOpt() == null && option.getValueSeparator() != NULL;
    }

    private static String toPropertyValue(String[] values) {
        if (isBooleanValue(values)) {
            return "true";
        } else {
            return values[0];
        }
    }

    private static boolean isBooleanValue(String[] values) {
        return values == null || values.length == 0 || values[0] == null;
    }

    private static String getIdentifier(Option option) {
        return option.getLongOpt() != null ? option.getLongOpt() : option.getOpt();
    }
}
