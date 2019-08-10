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
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileLoaderFactory implements MarketDataProviderFactory {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public MarketDataProvider create(TradingDay tradingDay, Configuration configuration) {
        if (!configuration.hasOption("path") && !configuration.hasOption("fileList")) {
            throw new IllegalArgumentException("no 'path' or 'fileList' property set.  Must set one");
        } else if (configuration.hasOption("path") && configuration.hasOption("fileList")) {
            throw new IllegalArgumentException("either set 'path' or 'fileList' not both");
        }

        ProcessingMode mode = ProcessingMode.fromOptionValue(configuration.getOptional("process"));
        File inputFile = findInputFile(configuration);

        return new LocalFileLoader(tradingDay, inputFile, mode);
    }

    private File findInputFile(Configuration configuration) {
        if (!configuration.hasOption("batchIndexEnvironmentParameter")) {
            return PathLoader.load(configuration.get("path")).toFile();
        }

        String envParam = configuration.get("batchIndexEnvironmentParameter");
        String fileListPath = configuration.get("fileList");
        LOGGER.info("File list path: {}", fileListPath);
        LOGGER.info("Environment parameter to look up: {}", envParam);

        int index;
        Configuration env = Configurations.load(System.getenv());
        if (env.hasOption(envParam)) {
            LOGGER.info("{} = {}", envParam, env.get(envParam));
            index = env.get(envParam, Integer.class);
        } else {
            //  throw new IllegalArgumentException(String.format("environmentParameter is set but there is no %s env
            // param set", envParam));
            //TODO hack cos i cant work out how to set local env variable on my mac
            index = 1;
            LOGGER.error("environmentParameter is set but there is no {} env param set", envParam);
        }

        Path fileList = PathLoader.load(fileListPath);

        try {
            return loadFileAtIndex(fileList, index).toFile();
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading input file list: " + fileList, e);
        }
    }

    private Path loadFileAtIndex(Path fileList, int index) throws IOException {
        String name = "";

        try (BufferedReader lineBuffer = Files.newBufferedReader(fileList)) {
            for (int i = 0; i <= index; i++) {
                name = lineBuffer.readLine();
            }
        }

        return PathLoader.load(name);
    }
}
