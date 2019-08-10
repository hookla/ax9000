package com.ax9k.app;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

class SupportedOptions {
    static final Option ALGO_NAME;
    static final Option BROKER_NAME;
    static final Option PROVIDER_NAME;
    static final Option ALGO_PROPERTIES;
    static final Option BROKER_PROPERTIES;
    static final Option PROVIDER_PROPERTIES;
    static final Option TESTING_MODE;
    static final Option OUTPUT_BUCKET;
    static final Option OUTPUT_PATH;
    static final Option BATCH_ID;
    static final Option REST_SERVICE;
    static final Option DONT_COPY_LOGS;
    static final Option SLACK_ERROR_LOG;
    static final Option EXIT_BETWEEN_TRADING_SESSIONS;
    static final Option CONFIG_FILE;
    static final Option RISK_MANAGER_CONFIG;

    static final Option TRAIN;
    static final Option TRAINING_CONFIG;
    static final Option TRAINING_PROPERTIES;
    static final Option TRAINING_VALIDATION;
    static final Option TRAINING_EVENT_TIME;
    static final Option TRAINING_SOURCE;
    static final Option TRAINING_CSV;
    static final Option TRAINING_ALLOW_INVALID_DATA;
    static final Option TRAINING_DESTINATION_BUCKET;
    static final Option TRAINING_DESTINATION_PATH;
    static final Option TRAINING_PERIODS;
    static final Option TRAINING_TARGET_FILTERS;
    static final Option TRAINING_TOLERANCE;
    static final Option TRAINING_DATA_FILE_SUFFIX;

    static {
        ALGO_PROPERTIES = Option.builder("A")
                                .argName("property=value")
                                .numberOfArgs(2)
                                .valueSeparator('=')
                                .desc("use value for given Algo property")
                                .required(false)
                                .build();

        BROKER_PROPERTIES = Option.builder("B")
                                  .argName("property=value")
                                  .numberOfArgs(2)
                                  .valueSeparator('=')
                                  .desc("use value for given Broker property")
                                  .required(false)
                                  .build();

        PROVIDER_PROPERTIES = Option.builder("P")
                                    .argName("property=value")
                                    .numberOfArgs(2)
                                    .valueSeparator('=')
                                    .desc("use value for given MarketDataProvider property")
                                    .required(false)
                                    .build();

        REST_SERVICE = Option.builder("r")
                             .hasArg(false)
                             .desc("start REST service")
                             .longOpt("rest")
                             .required(false)
                             .build();

        TESTING_MODE = Option.builder("t")
                             .hasArg(false)
                             .desc("run the application in testing mode")
                             .longOpt("testing")
                             .required(false)
                             .build();

        DONT_COPY_LOGS = Option.builder("lx")
                               .hasArg(false)
                               .desc("dont copy the logs to S3")
                               .longOpt("dont-copy-logs")
                               .required(false)
                               .build();

        SLACK_ERROR_LOG = Option.builder()
                                .hasArg(false)
                                .desc("log uncaught exceptions to Slack")
                                .longOpt("slack-error-log")
                                .required(false)
                                .build();

        ALGO_NAME = Option.builder("a")
                          .hasArg(true)
                          .desc("set a filter for loading Algo implementations")
                          .longOpt("algo")
                          .required(false)
                          .build();

        BROKER_NAME = Option.builder("b")
                            .hasArg(true)
                            .desc("set a filter for loading Broker implementations")
                            .longOpt("broker")
                            .required(false)
                            .build();

        PROVIDER_NAME = Option.builder("p")
                              .hasArg(true)
                              .desc("set a filter for loading MarketDataProvider implementations")
                              .longOpt("provider")
                              .required(false)
                              .build();

        OUTPUT_BUCKET = Option.builder("o")
                              .hasArg(true)
                              .desc("set the S3 bucket name in which to store log outputs")
                              .longOpt("output-bucket")
                              .required(false)
                              .build();

        OUTPUT_PATH = Option.builder("d")
                            .hasArg(true)
                            .desc("set the S3 path in which to store log outputs")
                            .longOpt("output-path")
                            .required(false)
                            .build();

        BATCH_ID = Option.builder("b")
                         .hasArg(true)
                         .desc("set the batch id to use for the end-of-day report")
                         .longOpt("batch-id")
                         .required(true)
                         .build();

        EXIT_BETWEEN_TRADING_SESSIONS = Option.builder()
                                              .hasArg(false)
                                              .desc("exit the current position when a trading session ends")
                                              .longOpt("exit-between-trading-sessions")
                                              .required(false)
                                              .build();

        CONFIG_FILE = Option.builder()
                            .hasArg(true)
                            .desc("set a configuration file to supplement the command-line options")
                            .longOpt("config-file")
                            .required(false)
                            .build();

        RISK_MANAGER_CONFIG = Option.builder()
                                    .hasArg(true)
                                    .desc("set the path to the risk manager configuration file")
                                    .longOpt("risk-manager-config")
                                    .required(false)
                                    .build();

        TRAIN = Option.builder()
                      .hasArg(false)
                      .desc("generate training data using the default settings")
                      .longOpt("train")
                      .required(false)
                      .build();

        TRAINING_CONFIG = Option.builder()
                                .hasArg(true)
                                .desc("set the path to the training configuration file")
                                .longOpt("training-config")
                                .required(false)
                                .build();

        TRAINING_PROPERTIES = Option.builder()
                                    .hasArg(true)
                                    .desc("set the semi-colon-separated list of properties for which to generate " +
                                          "future data")
                                    .longOpt("training-properties")
                                    .required(false)
                                    .build();

        TRAINING_VALIDATION = Option.builder()
                                    .hasArg(true)
                                    .desc("set the semi-colon-separated list of properties and min/max values with " +
                                          "which to " +
                                          "validate training data")
                                    .longOpt("training-validation")
                                    .required(false)
                                    .build();

        TRAINING_PERIODS = Option.builder()
                                 .hasArg(true)
                                 .desc("set the comma-separated list of periods for which to look ahead, in minutes")
                                 .longOpt("training-periods")
                                 .required(false)
                                 .build();

        TRAINING_TARGET_FILTERS = Option.builder()
                                        .hasArg(true)
                                        .desc("a comma separated list of chances to determine when determining " +
                                              "whether to log target values")
                                        .longOpt("training-target-filters")
                                        .required(false)
                                        .build();

        TRAINING_CSV = Option.builder()
                             .hasArg(false)
                             .desc("whether or not to output training data in CSV format")
                             .longOpt("training-gen-csv")
                             .required(false)
                             .build();

        TRAINING_EVENT_TIME = Option.builder()
                                    .hasArg(true)
                                    .desc("set the event time property name used in the training source file")
                                    .longOpt("training-time")
                                    .required(false)
                                    .build();

        TRAINING_SOURCE = Option.builder()
                                .hasArg(true)
                                .desc("set the name of the file to use as a source for generating training data")
                                .longOpt("training-source")
                                .required(false)
                                .build();

        TRAINING_DESTINATION_BUCKET = Option.builder("d")
                                            .hasArg(true)
                                            .desc("the S3 bucket in which to upload the output file ")
                                            .longOpt("training-destination-bucket")
                                            .required(false)
                                            .build();
        TRAINING_DESTINATION_PATH = Option.builder("dp")
                                          .hasArg(true)
                                          .desc("the S3 bucket path in which to upload the output file ")
                                          .longOpt("training-destination-path")
                                          .required(false)
                                          .build();

        TRAINING_ALLOW_INVALID_DATA = Option.builder()
                                            .hasArg(false)
                                            .desc("whether or not to use feature values of Double.MIN_VALUE in future" +
                                                  " data generation")
                                            .longOpt("training-allow-invalid-data")
                                            .required(false)
                                            .build();

        TRAINING_TOLERANCE = Option.builder()
                                   .hasArg(true)
                                   .desc("the threshold for registering a change in future feature values")
                                   .longOpt("training-tolerance")
                                   .type(Number.class)
                                   .required(false)
                                   .build();

        TRAINING_DATA_FILE_SUFFIX = Option.builder()
                                          .hasArg(true)
                                          .desc("suffix training file name eg security code")
                                          .longOpt("training-data-file-suffix")
                                          .type(Number.class)
                                          .required(false)
                                          .build();
    }

    static Options allSupportedOptions() {
        var options = new Options();
        options.addOption(ALGO_NAME);
        options.addOption(BROKER_NAME);
        options.addOption(PROVIDER_NAME);
        options.addOption(ALGO_PROPERTIES);
        options.addOption(BROKER_PROPERTIES);
        options.addOption(PROVIDER_PROPERTIES);
        options.addOption(TESTING_MODE);
        options.addOption(OUTPUT_BUCKET);
        options.addOption(OUTPUT_PATH);
        options.addOption(BATCH_ID);
        options.addOption(REST_SERVICE);
        options.addOption(DONT_COPY_LOGS);
        options.addOption(EXIT_BETWEEN_TRADING_SESSIONS);
        options.addOption(SLACK_ERROR_LOG);
        options.addOption(RISK_MANAGER_CONFIG);

        options.addOption(TRAIN);
        options.addOption(TRAINING_CONFIG);
        options.addOption(TRAINING_PROPERTIES);
        options.addOption(TRAINING_VALIDATION);
        options.addOption(TRAINING_EVENT_TIME);
        options.addOption(TRAINING_SOURCE);
        options.addOption(TRAINING_CSV);
        options.addOption(TRAINING_ALLOW_INVALID_DATA);
        options.addOption(TRAINING_DESTINATION_BUCKET);
        options.addOption(TRAINING_DESTINATION_PATH);
        options.addOption(TRAINING_PERIODS);
        options.addOption(TRAINING_TARGET_FILTERS);
        options.addOption(TRAINING_TOLERANCE);
        options.addOption(TRAINING_DATA_FILE_SUFFIX);

        return options;
    }

    static Options configFile() {
        Options options = new Options();
        options.addOption(CONFIG_FILE);
        return options;
    }
}
