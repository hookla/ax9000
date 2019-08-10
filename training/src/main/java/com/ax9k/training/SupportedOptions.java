package com.ax9k.training;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

class SupportedOptions {
    static final Option SOURCE_PATH;
    static final Option SOURCE_BUCKET;
    static final Option DESTINATION_BUCKET;
    static final Option DESTINATION_PATH;
    static final Option DONT_COPY_LOGS;

    static final Option VALIDATION;
    static final Option PROPERTIES;
    static final Option TARGET_FILTERS;
    static final Option TIME_PROPERTY;
    static final Option FORMAT;
    static final Option PERIODS;
    static final Option TOLERANCE;
    static final Option ALLOW_INVALID_DATA;

    static {
        SOURCE_PATH = Option.builder("p")
                            .hasArg(true)
                            .desc("the path to the input book state log")
                            .longOpt("source-path")
                            .required(true)
                            .build();
        SOURCE_BUCKET = Option.builder("s")
                              .hasArg(true)
                              .desc("the S3 bucket in which to look for the input file ")
                              .longOpt("source-bucket")
                              .required(false)
                              .build();
        DESTINATION_BUCKET = Option.builder("d")
                                   .hasArg(true)
                                   .desc("the S3 bucket in which to upload the output file ")
                                   .longOpt("destination-bucket")
                                   .required(false)
                                   .build();
        DESTINATION_PATH = Option.builder("dp")
                                 .hasArg(true)
                                 .desc("the S3 bucket path in which to upload the output file ")
                                 .longOpt("destination-path")
                                 .required(false)
                                 .build();
        TIME_PROPERTY = Option.builder()
                              .hasArg(true)
                              .desc("the JSON property under which event time values are stored in the input file")
                              .longOpt("time")
                              .required(true)
                              .build();
        VALIDATION = Option.builder()
                           .hasArg(true)
                           .desc("a semi-colon separated list of properties and min/max values to validate training " +
                                 "data")
                           .longOpt("validation")
                           .required(false)
                           .build();
        PROPERTIES = Option.builder()
                           .hasArg(true)
                           .desc("a semi-colon separated list of properties for which to generate training date")
                           .longOpt("properties")
                           .required(true)
                           .build();
        FORMAT = Option.builder("f")
                       .hasArg(true)
                       .desc("the format in which to print training data")
                       .longOpt("format")
                       .required(false)
                       .build();

        PERIODS = Option.builder()
                        .hasArg(true)
                        .desc("a comma separated list of periods for which to look ahead, in minutes")
                        .longOpt("periods")
                        .required(true)
                        .build();

        TARGET_FILTERS = Option.builder()
                               .hasArg(true)
                               .desc("a comma separated list of chances to determine when determining whether to log " +
                                     "target values")
                               .longOpt("target-filters")
                               .required(false)
                               .build();

        ALLOW_INVALID_DATA = Option.builder()
                                   .hasArg(false)
                                   .desc("whether or not to use feature values of Double.MIN_VALUE in future data " +
                                         "generation")
                                   .longOpt("allow-invalid-data")
                                   .required(false)
                                   .build();

        TOLERANCE = Option.builder()
                          .hasArg(true)
                          .desc("the threshold for registering a change in future feature values")
                          .longOpt("tolerance")
                          .required(true)
                          .build();

        DONT_COPY_LOGS = Option.builder("lx")
                               .hasArg(false)
                               .desc("dont copy the logs to S3")
                               .longOpt("dont-copy-logs")
                               .required(false)
                               .build();
    }

    static Options allSupportedOptions() {
        Options result = new Options();
        result.addOption(SOURCE_PATH);
        result.addOption(SOURCE_BUCKET);
        result.addOption(DESTINATION_PATH);
        result.addOption(DESTINATION_BUCKET);
        result.addOption(PROPERTIES);
        result.addOption(VALIDATION);
        result.addOption(TIME_PROPERTY);
        result.addOption(FORMAT);
        result.addOption(PERIODS);
        result.addOption(TARGET_FILTERS);
        result.addOption(ALLOW_INVALID_DATA);
        result.addOption(TOLERANCE);
        result.addOption(DONT_COPY_LOGS);
        return result;
    }
}
