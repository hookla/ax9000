package com.ax9k.app;

import com.ax9k.training.Trainer;
import com.ax9k.utils.config.Configuration;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static com.ax9k.app.SupportedOptions.DONT_COPY_LOGS;
import static com.ax9k.app.SupportedOptions.TRAIN;
import static com.ax9k.app.SupportedOptions.TRAINING_ALLOW_INVALID_DATA;
import static com.ax9k.app.SupportedOptions.TRAINING_CONFIG;
import static com.ax9k.app.SupportedOptions.TRAINING_CSV;
import static com.ax9k.app.SupportedOptions.TRAINING_DATA_FILE_SUFFIX;
import static com.ax9k.app.SupportedOptions.TRAINING_DESTINATION_BUCKET;
import static com.ax9k.app.SupportedOptions.TRAINING_DESTINATION_PATH;
import static com.ax9k.app.SupportedOptions.TRAINING_EVENT_TIME;
import static com.ax9k.app.SupportedOptions.TRAINING_PERIODS;
import static com.ax9k.app.SupportedOptions.TRAINING_PROPERTIES;
import static com.ax9k.app.SupportedOptions.TRAINING_SOURCE;
import static com.ax9k.app.SupportedOptions.TRAINING_TARGET_FILTERS;
import static com.ax9k.app.SupportedOptions.TRAINING_TOLERANCE;
import static com.ax9k.app.SupportedOptions.TRAINING_VALIDATION;

final class TrainingPhase {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DEFAULT_TRAINING_FILE = "Features.log";
    private static final String DEFAULT_PROPERTIES = "MID";
    private static final String DEFAULT_VALIDATION = "";
    private static final String DEFAULT_PERIODS = "10";
    private static final String DEFAULT_FILTERS = "1,1,1";
    private static final String DEFAULT_EVENT_TIME_PROPERTY = "eventTime";
    private static final double DEFAULT_TOLERANCE = 0.0025;

    private final boolean generateTrainingData;

    private final Path trainingDataSource;
    private final String trainingDataDestinationBucket;
    private final Path trainingDataDestinationPath;
    private final LocalDate date;

    private final String properties;
    private final String validation;
    private final String eventTimeProperty;
    private final String periods;
    private final String filters;

    private final double tolerance;

    private final boolean outputCsvTrainingData;
    private final boolean filterInvalidData;
    private final boolean copyLogs;
    private final String trainingDataFileSuffix;

    TrainingPhase(CommandLine arguments, Configuration externalConfig, LocalDate date, String symbol) {
        this.date = date;

        trainingDataFileSuffix = retrieveConfig(arguments, TRAINING_DATA_FILE_SUFFIX,
                                                externalConfig, "data_file_suffix",
                                                symbol);

        properties = retrieveConfig(arguments, TRAINING_PROPERTIES,
                                    externalConfig, "properties",
                                    DEFAULT_PROPERTIES);

        validation = retrieveConfig(arguments, TRAINING_VALIDATION,
                                    externalConfig, "validation",
                                    DEFAULT_VALIDATION);

        eventTimeProperty = retrieveConfig(arguments, TRAINING_EVENT_TIME,
                                           externalConfig, "event_time_property",
                                           DEFAULT_EVENT_TIME_PROPERTY);

        periods = retrieveConfig(arguments, TRAINING_PERIODS,
                                 externalConfig, "periods",
                                 DEFAULT_PERIODS);

        outputCsvTrainingData =
                arguments.hasOption(TRAINING_CSV.getLongOpt()) ||
                (externalConfig.hasOption("format") &&
                 externalConfig.get("format").toLowerCase().equals("csv"));

        filterInvalidData =
                !arguments.hasOption(TRAINING_ALLOW_INVALID_DATA.getLongOpt()) ||
                (externalConfig.hasOption("allow_invalid_data") &&
                 externalConfig.get("allow_invalid_data").toLowerCase().equals("false"));

        copyLogs =
                !arguments.hasOption(DONT_COPY_LOGS.getLongOpt()) ||
                (externalConfig.hasOption("dont_copy_logs") &&
                 externalConfig.get("dont_copy_logs").toLowerCase().equals("false"));

        trainingDataDestinationBucket = retrieveConfig(arguments, TRAINING_DESTINATION_BUCKET,
                                                       externalConfig, "destination_bucket",
                                                       null);

        String sourceString = retrieveConfig(arguments, TRAINING_SOURCE,
                                             externalConfig, "source",
                                             DEFAULT_TRAINING_FILE);
        trainingDataSource = Paths.get(".", "temp", sourceString);

        String parsedDestination = arguments.getOptionValue(TRAINING_DESTINATION_PATH.getLongOpt());
        trainingDataDestinationPath = parsedDestination != null ?
                                      Paths.get(parsedDestination) :
                                      externalConfig.getOptional("destination", Path.class)
                                                    .orElse(trainingDataSource.getParent());

        filters = retrieveConfig(arguments, TRAINING_TARGET_FILTERS,
                                 externalConfig, "target_filters",
                                 DEFAULT_FILTERS);

        if (arguments.hasOption(TRAINING_TOLERANCE.getLongOpt())) {
            try {
                tolerance = Double.parseDouble(arguments.getOptionValue(TRAINING_TOLERANCE.getLongOpt()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("tolerance is not a valid number");
            }
        } else {
            tolerance = externalConfig.getOptional("tolerance", Double.class).orElse(DEFAULT_TOLERANCE);
        }

        generateTrainingData =
                arguments.hasOption(TRAIN.getLongOpt()) ||
                arguments.hasOption(TRAINING_CONFIG.getLongOpt()) ||
                trainingSettingsConfigured(arguments);
    }

    private String retrieveConfig(CommandLine commandLine,
                                  Option option,
                                  Configuration configuration,
                                  String configKey,
                                  String defaultValue) {
        return commandLine.getOptionValue(option.getLongOpt(),
                                          configuration.getOptional(configKey)
                                                       .orElse(defaultValue));
    }

    @SuppressWarnings({ "StringEquality", "ConstantConditions" })
    private boolean trainingSettingsConfigured(CommandLine arguments) {
        return properties != DEFAULT_PROPERTIES ||
               validation != DEFAULT_VALIDATION ||
               eventTimeProperty != DEFAULT_EVENT_TIME_PROPERTY ||
               arguments.hasOption(TRAINING_CSV.getLongOpt()) ||
               periods != DEFAULT_PERIODS ||
               tolerance != DEFAULT_TOLERANCE ||
               filters != DEFAULT_FILTERS;
    }

    void run() {

        if (generateTrainingData) {
            try {
                new Trainer(trainingDataSource,
                            trainingDataDestinationPath,
                            null,
                            trainingDataDestinationBucket,
                            eventTimeProperty,
                            properties,
                            validation,
                            periods,
                            filters,
                            date,
                            tolerance,
                            outputCsvTrainingData,
                            filterInvalidData,
                            copyLogs,
                            trainingDataFileSuffix).run();
            } catch (IOException exception) {
                LOGGER.error("Error during training data generation,", exception);
            }
        }
    }
}
