package com.ax9k.backtesting;

import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.utils.s3.S3Downloader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

class HistoricBarDataLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String FILE_NAME_SUFFIX = "_5.csv";
    private static final String VOLUME_DATA_BUCKET = "ax9000-market-data";
    private static final String VOLUME_DATA_DIRECTORY = "extracted_ohlc_data";

    private static final int NOT_FOUND = 404;

    static EventReplay<OhlcvBar> load(LocalDate date) {
        LOGGER.info("Loading historic bar data for date: {}", date);
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
                throw new RuntimeException("Could not find file for given date. " +
                                           "Ensure that it exists and the path is correct: " + date, e);
            } else {
                throw new RuntimeException("Error downloading file from S3", e);
            }
        }
        LOGGER.info("Got it...");
        try {
            BufferedReader lines = Files.newBufferedReader(localCopy);
            //Skip header row
            lines.readLine();
            return EventReplay.ofSingleLineEvents(new CsvOhlcvParser(),
                                                  lines,
                                                  OhlcvBarAggregator.INPUT_BAR_PERIOD_LENGTH);
        } catch (IOException e) {
            throw new UncheckedIOException("Error opening local file copy", e);
        }
    }
}
