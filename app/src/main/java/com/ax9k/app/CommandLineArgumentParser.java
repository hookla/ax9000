package com.ax9k.app;

import com.ax9k.utils.path.PathLoader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.ax9k.app.SupportedOptions.CONFIG_FILE;
import static com.ax9k.app.SupportedOptions.allSupportedOptions;
import static com.ax9k.app.SupportedOptions.configFile;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

final class CommandLineArgumentParser {
    private static final String NOTHING = "";
    private static final char[] CAPITAL_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private final String[] rawArguments;

    CommandLineArgumentParser(String[] rawArguments) {
        this.rawArguments = requireNonNull(rawArguments, "CommandLineArgumentParser rawArguments");
    }

    CommandLine commandLine() {
        return parseArguments(rawArguments);
    }

    private static CommandLine parseArguments(String[] arguments) {
        arguments = cleanArguments(arguments);

        try {
            arguments = loadConfigFile(arguments);
            return parse(arguments);
        } catch (IOException e) {
            throw new UncheckedIOException("error while reading configuration file", e);
        } catch (S3Exception e) {
            throw new IllegalStateException("error while downloading configuration file", e);
        } catch (ParseException e) {
            throw new IllegalArgumentException(format("could not parse command line arguments: %s. Reason: %s",
                                                      Arrays.toString(arguments), e.getMessage()));
        }
    }

    private static String[] cleanArguments(String[] arguments) {
        if (arguments.length > 0 && (arguments[0].startsWith("-Dfile.encoding") || arguments[0].startsWith("-Xms"))) {
            /*
            FIXME This is an ugly hack to deal with a Gradle bug.
            It looks like GRADLE_OPTS configuration are being leaked to the application, so
            this hack simply ignores the first five arguments. A better solution must be found, though.
            */
            return Arrays.copyOfRange(arguments, 5, arguments.length);
        } else {
            return arguments;
        }
    }

    private static String[] loadConfigFile(String[] arguments) throws IOException, S3Exception {
        CommandLine commandLine = parseConfigOptions(arguments);

        if (commandLine != null && commandLine.hasOption(CONFIG_FILE.getLongOpt())) {
            String path = commandLine.getOptionValue(CONFIG_FILE.getLongOpt());
            Path configFile = PathLoader.load(path);

            String[] fileArguments = readConfigFile(configFile);
            if (commandLine.getArgs().length == 0) {
                return fileArguments;
            } else if (fileArguments.length == 0) {
                return commandLine.getArgs();
            }
            arguments = merge(commandLine.getArgs(), fileArguments);
        }
        return arguments;
    }

    private static CommandLine parseConfigOptions(String[] arguments) {
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(configFile(), arguments, true);
        } catch (ParseException ignored) {}

        return null;
    }

    private static String[] readConfigFile(Path configFile) throws IOException {
        Options supportedOptions = allSupportedOptions();

        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(configFile)) {
            properties.load(stream);
        }

        return properties
                .entrySet()
                .stream()
                .map(entry -> toArgument(entry, supportedOptions))
                .toArray(String[]::new);
    }

    private static String toArgument(Map.Entry<Object, Object> entry, Options supportedOptions) {
        String key = entry.getKey().toString();
        String value = entry.getValue().toString();
        boolean isTrueOption = value.equalsIgnoreCase("true");
        boolean isFalseOption = value.equalsIgnoreCase("false");
        boolean isBooleanOption = isTrueOption || isFalseOption;

        if (isBooleanOption) {
            Option option = supportedOptions.getOption(key);
            if (option != null && !option.hasArg()) {
                if (isTrueOption) {
                    return "-".concat(key);
                } else {
                    return NOTHING;
                }
            }
        }
        return format("-%s=%s", key, value);
    }

    private static String[] merge(String[] arguments1, String[] arguments2) {
        String[] result = ArrayUtils.addAll(arguments1, arguments2);

        result = mergeDynamicProperties(result);

        return result;
    }

    private static String[] mergeDynamicProperties(String[] arguments) {
        Set<String> dynamicPropertyKeysFound = new HashSet<>(arguments.length);

        for (int i = 0; i < arguments.length; i++) {
            String argument = arguments[i];

            if (isDynamicProperty(argument)) {
                String key = dynamicPropertyKey(argument);
                if (dynamicPropertyKeysFound.contains(key)) {
                    arguments = ArrayUtils.remove(arguments, i);
                } else {
                    dynamicPropertyKeysFound.add(key);
                }
            }
        }
        return arguments;
    }

    private static String dynamicPropertyKey(String argument) {
        return argument.substring(0, argument.lastIndexOf('='));
    }

    private static boolean isDynamicProperty(String argument) {
        return firstCharacterIsHyphen(argument) &&
               secondCharacterIsCapitalLetter(argument) &&
               containsSingleEqualsSignBetweenKeyAndValue(argument);
    }

    private static boolean containsSingleEqualsSignBetweenKeyAndValue(String argument) {
        return containsSingleEqualsSign(argument) && !argument.endsWith("=");
    }

    private static boolean containsSingleEqualsSign(String argument) {
        int expectedLengthWithoutEqualsSign = argument.length() - 1;

        String withoutEquals = argument.replaceAll("=", NOTHING);

        return withoutEquals.length() == expectedLengthWithoutEqualsSign;
    }

    private static boolean secondCharacterIsCapitalLetter(String argument) {
        return ArrayUtils.contains(CAPITAL_LETTERS, argument.charAt(1));
    }

    private static boolean firstCharacterIsHyphen(String argument) {
        return argument.charAt(0) == '-';
    }

    private static CommandLine parse(String[] arguments) throws ParseException {
        DefaultParser parser = new DefaultParser();
        return parser.parse(allSupportedOptions(), arguments);
    }
}
