package com.ax9k.algorohit;

import com.ax9k.utils.path.PathLoader;
import com.ax9k.utils.s3.S3Downloader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

public final class AverageQuantityData {
    private static final AverageQuantityData EMPTY = new AverageQuantityData(emptyMap());

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String FILE_NAME_SUFFIX = "_60.csv";
    private static final String VOLUME_DATA_BUCKET = "ax9000-market-data";
    private static final String VOLUME_DATA_DIRECTORY = "volume_data_formatted_for_date";

    private static final int NOT_FOUND = 404;

    private final Map<LocalTime, Double> averageQuantityByTime;

    private AverageQuantityData(Map<LocalTime, Double> averageQuantityByTime) {
        this.averageQuantityByTime = averageQuantityByTime;
    }

    public static AverageQuantityData load(String fileLocationPathFormat, LocalDate date) {
        LOGGER.info("Loading average volume data for date: {}", date);

        String dataFilePath = format(fileLocationPathFormat, date.format(FILE_NAME_FORMAT));

        Path localCopy;
        try {
            localCopy = PathLoader.load(dataFilePath);
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof NoSuchKeyException) {
                LOGGER.warn("Could not find file for given date. " +
                            "Ensure that it exists and the path is correct. Path: {}", dataFilePath);
                return EMPTY;
            }
            throw ex;
        }

        return new AverageQuantityData(parseDataFile(localCopy));
    }

    private static Map<LocalTime, Double> parseDataFile(Path file) {
        try {
            return Files.lines(file)
                        .skip(1)
                        .filter(line -> !line.isEmpty())
                        .map(line -> line.split(","))
                        .collect(toMap(AverageQuantityData::extractTime,
                                       AverageQuantityData::extractAverageVolume));
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading data file", e);
        }
    }

    private static LocalTime extractTime(String[] parts) {
        try {
            String part = parts[1];
            if (part.length() == 5) {
                part = part.concat(":00");
            }
            return LocalTime.parse(part, TIME_FORMAT);
        } catch (DateTimeParseException invalidTime) {
            throw new IllegalArgumentException("Given time is not valid: " + Arrays.toString(parts),
                                               invalidTime);
        }
    }

    private static Double extractAverageVolume(String[] parts) {
        try {
            if (parts.length >= 7) {
                return Double.valueOf(parts[7]);
            } else {
                return 0d;
            }
        } catch (NumberFormatException invalidDouble) {
            throw new IllegalArgumentException(
                    "Given volume is not valid: " + Arrays.toString(parts),
                    invalidDouble
            );
        }
    }

    public static AverageQuantityData load(LocalDate date) {
        LOGGER.info("Loading average volume data for date: {}", date);
        String fileName = date.format(FILE_NAME_FORMAT) + FILE_NAME_SUFFIX;

        Path fileLocation = Paths.get(VOLUME_DATA_DIRECTORY, fileName);
        LOGGER.info("Looking for data file in S3 directory: {}", fileLocation);

        Path localCopy;
        try {
            localCopy = new S3Downloader(fileLocation, VOLUME_DATA_BUCKET).download();
        } catch (IOException e) {
            throw new UncheckedIOException("Error downloading file from S3", e);
        } catch (S3Exception e) {
            if (e.statusCode() == NOT_FOUND) {
                LOGGER.warn("Could not find file for given date. " +
                            "Ensure that it exists and the path is correct: {}", date);
                return EMPTY;
            } else {
                throw new RuntimeException("Error downloading file from S3", e);
            }
        }
        LOGGER.info("Got it...");
        return new AverageQuantityData(parseDataFile(localCopy));
    }

    public double getForTime(LocalTime time) {
        return averageQuantityByTime.getOrDefault(time.truncatedTo(MINUTES), 0d);
    }

    public int getNumberOfEntries() {
        return averageQuantityByTime.size();
    }
}
