package com.ax9k.training;

import com.ax9k.training.writer.CsvWriter;
import com.ax9k.training.writer.JsonWriter;
import com.ax9k.training.writer.Writer;
import com.ax9k.utils.s3.S3Downloader;
import com.ax9k.utils.s3.S3Uploader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.ax9k.training.SupportedOptions.ALLOW_INVALID_DATA;
import static com.ax9k.training.SupportedOptions.DESTINATION_BUCKET;
import static com.ax9k.training.SupportedOptions.DESTINATION_PATH;
import static com.ax9k.training.SupportedOptions.DONT_COPY_LOGS;
import static com.ax9k.training.SupportedOptions.FORMAT;
import static com.ax9k.training.SupportedOptions.PERIODS;
import static com.ax9k.training.SupportedOptions.PROPERTIES;
import static com.ax9k.training.SupportedOptions.SOURCE_BUCKET;
import static com.ax9k.training.SupportedOptions.SOURCE_PATH;
import static com.ax9k.training.SupportedOptions.TARGET_FILTERS;
import static com.ax9k.training.SupportedOptions.TIME_PROPERTY;
import static com.ax9k.training.SupportedOptions.TOLERANCE;
import static com.ax9k.training.SupportedOptions.VALIDATION;
import static com.ax9k.training.SupportedOptions.allSupportedOptions;
import static java.util.Objects.requireNonNull;

public class Trainer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PROPERTY_SEPARATOR = ";";

    private final Path sourcePath;
    private final Path destinationPath;

    private final Optional<String> sourceBucket;
    private final Optional<String> destinationBucket;

    private final Collection<String> properties;
    private final Collection<String> validation;
    private final String eventTimeProperty;
    private final String periods;
    private final String filters;

    private final double tolerance;

    private final boolean outputCsv;
    private final boolean filterInvalidData;
    private final boolean copyLogs;
    private String trainingDataFileSuffix;

    private LocalDate date;

    public Trainer(Path sourcePath,
                   Path destinationPath,
                   String sourceBucket,
                   String destinationBucket,
                   String eventTimeProperty,
                   String properties,
                   String validation,
                   String periods,
                   String filters,
                   LocalDate date,
                   double tolerance,
                   boolean outputCsv,
                   boolean filterInvalidData,
                   boolean copyLogs,
                   String trainingDataFileSuffix) {
        this.sourcePath = requireNonNull(sourcePath, "Trainer sourcePath");
        this.outputCsv = outputCsv;
        this.filterInvalidData = filterInvalidData;
        this.copyLogs = copyLogs;
        this.properties = parseProperties(requireNonNull(properties, "Trainer properties"));
        this.validation = parseProperties(requireNonNull(validation, "Trainer validation"));
        this.tolerance = tolerance;
        this.eventTimeProperty = requireNonNull(eventTimeProperty, "Trainer eventTimeProperty");
        this.periods = requireNonNull(periods, "Trainer periods");
        this.filters = requireNonNull(filters, "Trainer filters");
        this.sourceBucket = Optional.ofNullable(sourceBucket);
        this.destinationBucket = Optional.ofNullable(destinationBucket);
        this.destinationPath = destinationPath != null ? destinationPath : sourcePath.getParent();
        this.date = date;
        this.trainingDataFileSuffix = trainingDataFileSuffix;
    }

    private Collection<String> parseProperties(String raw) {
        raw = raw.trim();

        if (raw.isEmpty()) {
            return Collections.emptySet();
        }

        return Arrays.stream(raw.split(PROPERTY_SEPARATOR))
                     .map(String::trim)
                     .collect(Collectors.toSet());
    }

    private Trainer(CommandLine arguments) {
        sourcePath = Paths.get(arguments.getOptionValue(SOURCE_PATH.getLongOpt()));
        sourceBucket = Optional.ofNullable(arguments.getOptionValue(SOURCE_BUCKET.getLongOpt()));
        destinationBucket = Optional.ofNullable(arguments.getOptionValue((DESTINATION_BUCKET.getLongOpt())));
        copyLogs = !arguments.hasOption(DONT_COPY_LOGS.getLongOpt());
        eventTimeProperty = arguments.getOptionValue(TIME_PROPERTY.getLongOpt());
        periods = arguments.getOptionValue(PERIODS.getLongOpt());

        filters = arguments.getOptionValue(TARGET_FILTERS.getLongOpt(), "1,1,1");

        outputCsv = "csv".equals(arguments.getOptionValue(FORMAT.getLongOpt()));
        filterInvalidData = !arguments.hasOption(ALLOW_INVALID_DATA.getLongOpt());

        String givenDestination = arguments.getOptionValue((DESTINATION_PATH.getLongOpt()));
        destinationPath = givenDestination != null ? Paths.get(givenDestination) : sourcePath.getParent();

        properties = parseProperties(arguments.getOptionValue(PROPERTIES.getLongOpt()));

        if (arguments.hasOption(VALIDATION.getLongOpt())) {
            validation = parseProperties(arguments.getOptionValue(VALIDATION.getLongOpt()));
        } else {
            validation = Collections.emptySet();
        }

        tolerance = Double.parseDouble(arguments.getOptionValue(TOLERANCE.getLongOpt()));
    }

    public static void main(String[] arguments) {
        LOGGER.info("Starting Training Data Generator");
        CommandLine parsed = parseArguments(arguments);

        try {
            new Trainer(parsed).run();
        } catch (Exception exception) {
            LOGGER.error("Error during programme execution", exception);
        }
        LOGGER.info("... All done!");
    }

    private static CommandLine parseArguments(final String[] original) {
        String[] arguments = workAroundBug(original);
        return parseValidArguments(arguments);
    }

    private static String[] workAroundBug(String[] original) {
        String[] arguments;
        if (original.length > 0 && (original[0].startsWith("-Dfile.encoding") || original[0].startsWith("-Xms"))) {
            /*
            FIXME This is an ugly hack to deal with a Gradle bug.
            It looks like GRADLE_OPTS properties are being leaked to the application, so
            this hack simply ignores the first five arguments. A better solution must be found, though.
            */
            arguments = Arrays.copyOfRange(original, 5, original.length);
        } else {
            arguments = original;
        }
        return arguments;
    }

    private static CommandLine parseValidArguments(String[] arguments) {
        try {
            return new DefaultParser().parse(allSupportedOptions(), arguments);
            //nothing to check right now..
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    String.format("Could not parse command line arguments: %s. Reason: %s",
                                  Arrays.toString(arguments), e.getMessage()
                    )
            );
        }
    }

    public void run() throws IOException {
        Path input = resolveInputPath();
        Path trainingOutput = createTemporaryOutputFile();

        LOGGER.info("Input file: {}", input);
        LOGGER.info("Input file size: {} KiB", toKilobytes(Files.size(input)));
        LOGGER.info("Temporary output file: {}", trainingOutput);

        String trainingFileName;
        Path tempSchemaFile;
        try (BufferedReader reader = createGzipCapableReader(input);
             BufferedWriter writer = createGzipWriter(trainingOutput)) {
            Writer formatter = outputCsv ? new CsvWriter(writer) : new JsonWriter(writer);
            Schema schema = new TrainingDataGenerator(
                    reader,
                    eventTimeProperty,
                    properties,
                    validation,
                    filters,
                    periods,
                    tolerance,
                    filterInvalidData
            ).generate(formatter);

            tempSchemaFile = createTempSchemaFile(schema);
            trainingFileName = formatter.resolveFileName("TrainingData_" + trainingDataFileSuffix, ".gz", date);
        }

        persistOutputFile(trainingOutput, trainingFileName);
        persistOutputFile(tempSchemaFile);
    }

    private Path resolveInputPath() throws IOException {
        if (sourceBucket.isPresent()) {
            return new S3Downloader(sourcePath, sourceBucket.get()).download();
        } else {
            return sourcePath.toAbsolutePath().normalize().toRealPath();
        }
    }

    private Path createTemporaryOutputFile() throws IOException {
        return Files.createTempFile("TrainingData", ".tmp");
    }

    private double toKilobytes(long size) {
        return size / 1024d;
    }

    private BufferedReader createGzipCapableReader(Path file) throws IOException {
        InputStream stream = Files.newInputStream(file);
        if (file.toString().endsWith(".gz")) {
            stream = new GZIPInputStream(stream);
        }
        InputStreamReader reader = new InputStreamReader(stream);
        return new BufferedReader(reader);
    }

    private BufferedWriter createGzipWriter(Path file) throws IOException {
        OutputStream stream = Files.newOutputStream(file);
        stream = new GZIPOutputStream(stream);
        OutputStreamWriter writer = new OutputStreamWriter(stream);
        return new BufferedWriter(writer);
    }

    private Path createTempSchemaFile(Schema schema) throws IOException {
        Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
        String destinationDirectoryName = destinationPath.getFileName().toString();
        return schema.save(tempDirectory, destinationDirectoryName);
    }

    @SuppressWarnings("ConstantConditions")
    private void persistOutputFile(Path tempLocal, String fileName) throws IOException {
        Path output = destinationPath.resolve(fileName);
        if (copyLogs && destinationBucket.isPresent()) {
            new S3Uploader(destinationBucket.get()).upload(tempLocal, output);
            LOGGER.info("Uploaded to S3 {}", output);
            Path local = sourcePath.getParent().resolve(fileName).toAbsolutePath();
            localCopy(tempLocal, local);
        } else {
            output = output.toAbsolutePath();
            localCopy(tempLocal, output.toAbsolutePath());
        }
    }

    private void localCopy(Path source, Path destination) throws IOException {
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Copied to {}", destination);
    }

    private void persistOutputFile(Path temp) throws IOException {
        persistOutputFile(temp, temp.getFileName().toString());
    }
}
