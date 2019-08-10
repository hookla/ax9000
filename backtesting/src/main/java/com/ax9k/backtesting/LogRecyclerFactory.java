package com.ax9k.backtesting;

import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.provider.MarketDataProvider;
import com.ax9k.provider.MarketDataProviderFactory;
import com.ax9k.utils.config.Configuration;
import com.ax9k.utils.config.Configurations;
import com.ax9k.utils.path.PathLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class LogRecyclerFactory implements MarketDataProviderFactory {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Pattern FILE_LIST_FORMAT = Pattern.compile(
            "[^\\s^,]+[\\s]*,[\\s]*[^\\s^,]+[\\s]*,[\\s]*[^\\s^,]+"
    );
    private static final String FILE_LIST_OPTION = "fileList";
    private static final String SINGLE_LOG_FILE_ERROR = "Must provide all log files";
    private static final String NO_INPUT_ERROR = format("No '%s' or log files provided", FILE_LIST_OPTION);
    private static final String BOTH_INPUT_ERROR =
            format("Both '%s' and log file provided. Can only use one", FILE_LIST_OPTION);
    private static final String INVALID_FILE_LIST_ERROR =
            "File list line is not in the format of '" + FILE_LIST_FORMAT + "'. Line: %s";
    private static final String INDEX_OPTION = "batchIndexEnvironmentParameter";
    private static final String BOOK_LOG_OPTION = "bookLog";
    private static final String TRADE_LOG_OPTION = "tradeLog";
    private static final String BAR_LOG_OPTION = "barLog";
    private static final String SEPARATOR = ",";
    private static final Path NO_PATH = null;
    private static final int BOOK_LOG_INDEX = 0;
    private static final int TRADE_LOG_INDEX = 1;
    private static final int BAR_LOG_INDEX = 2;

    @Override
    public MarketDataProvider create(TradingDay tradingDay, Configuration configuration) {
        configuration.loadExternalFileFromOptions();

        if (notAllLogFilesProvided(configuration)) {
            throw new IllegalArgumentException(SINGLE_LOG_FILE_ERROR);
        } else if (!configuration.hasOption(BOOK_LOG_OPTION) && !configuration.hasOption(FILE_LIST_OPTION)) {
            throw new IllegalArgumentException(NO_INPUT_ERROR);
        } else if (configuration.hasOption(BOOK_LOG_OPTION) && configuration.hasOption(FILE_LIST_OPTION)) {
            throw new IllegalArgumentException(BOTH_INPUT_ERROR);
        }

        Path[] logFiles = findInputFiles(configuration);

        return new LogRecycler(tradingDay,
                               logFiles[BOOK_LOG_INDEX],
                               logFiles[TRADE_LOG_INDEX],
                               logFiles[BAR_LOG_INDEX]);
    }

    private static boolean notAllLogFilesProvided(Configuration configuration) {
        return !configuration.hasOption(BOOK_LOG_OPTION) ||
               !configuration.hasOption(TRADE_LOG_OPTION);
    }

    private static Path[] findInputFiles(Configuration configuration) {
        if (!configuration.hasOption(FILE_LIST_OPTION)) {
            Path barLog = NO_PATH;
            if (configuration.hasOption(BAR_LOG_OPTION)) {
                barLog = PathLoader.load(configuration.get(BAR_LOG_OPTION));
            }

            return new Path[] {
                    PathLoader.load(configuration.get(BOOK_LOG_OPTION)),
                    PathLoader.load(configuration.get(TRADE_LOG_OPTION)),
                    barLog
            };
        }

        Path fileList = configuration.get(FILE_LIST_OPTION, PathLoader::load);
        int index = getBatchIndex(configuration);

        return loadFilesAtIndex(fileList, index);
    }

    private static int getBatchIndex(Configuration configuration) {
        String indexParameter = configuration.get(INDEX_OPTION);
        Configuration environment = Configurations.load(System.getenv());

        Optional<Integer> index = environment.getOptional(indexParameter, Integer.class);

        index.ifPresentOrElse(value -> LOGGER.info("{} = {}", indexParameter, value),
                              () -> LOGGER.warn("No environment variable for key '{}'", indexParameter));

        return index.orElse(1);
    }

    private static Path[] loadFilesAtIndex(Path fileList, int index) {
        String line = readLine(fileList, index);
        if (!FILE_LIST_FORMAT.matcher(line).matches()) {
            throw new IllegalArgumentException(format(INVALID_FILE_LIST_ERROR, line));
        }
        String[] paths = line.split(SEPARATOR);

        Path barLog = null;
        if (!paths[BAR_LOG_INDEX].equalsIgnoreCase("null")) {
            barLog = PathLoader.load(paths[BAR_LOG_INDEX]);
        }

        return new Path[] {
                PathLoader.load(paths[BOOK_LOG_INDEX]),
                PathLoader.load(paths[TRADE_LOG_INDEX]),
                barLog
        };
    }

    private static String readLine(Path fileList, int index) {
        String logs = "";

        try (BufferedReader lineBuffer = Files.newBufferedReader(fileList)) {
            for (int i = BOOK_LOG_INDEX; i <= index; i++) {
                logs = lineBuffer.readLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading input file", e);
        }

        return logs;
    }
}
