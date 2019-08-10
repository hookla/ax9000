package com.ax9k.app;

import com.ax9k.algo.Algo;
import com.ax9k.algo.trading.TradingAlgo;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.positionmanager.PositionReporter;
import com.ax9k.provider.MarketDataProvider;
import com.ax9k.utils.s3.S3Uploader;
import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static com.ax9k.app.SupportedOptions.BATCH_ID;
import static com.ax9k.app.SupportedOptions.OUTPUT_BUCKET;
import static com.ax9k.app.SupportedOptions.OUTPUT_PATH;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

class EndOfDayReport {
    private static final Logger LOGGER = LogManager.getLogger();

    private final S3Uploader uploader;
    private final String outputPath;
    private final String providerSource;
    private final String createdTimestamp;
    private final String batchId;
    private final LocalDate date;
    private final String algoName;
    private final double pnl;
    private final double maxPnl;
    private final double minPnl;
    private final int hitStopCount;
    private final double dailyTradeHigh;
    private final double dailyTradeLow;
    private final int winningTrades;
    private final int losingTrades;

    private final int tradeCount;

    private Connection dbConnection;

    EndOfDayReport(CommandLine arguments,
                   PositionReporter positionReporter,
                   Algo algo,
                   MarketDataProvider provider,
                   TradingDay tradingDay) {
        String outputBucket = arguments.getOptionValue(OUTPUT_BUCKET.getLongOpt());
        uploader = new S3Uploader(outputBucket);
        outputPath = arguments.getOptionValue(OUTPUT_PATH.getLongOpt());
        batchId = arguments.getOptionValue(BATCH_ID.getLongOpt());
        createdTimestamp = Instant.now().toString();
        pnl = positionReporter.getPnl();
        maxPnl = positionReporter.getDailyHighestPnl();
        minPnl = positionReporter.getDailyLowestPnl();
        hitStopCount = algo instanceof TradingAlgo ? ((TradingAlgo) algo).getHitStopCount() : 0;
        tradeCount = positionReporter.getTradeCount();
        providerSource = provider.getSource();
        algoName = algo.getAlgoName();
        date = provider.getDate();
        dailyTradeHigh = tradingDay.getDailyTradeHigh();
        dailyTradeLow = tradingDay.getDailyTradeLow();
        int dailyTradeCount = tradingDay.getDailyTradeCount();
        winningTrades = positionReporter.getWinningTrades();
        losingTrades = positionReporter.getLosingTrades();
    }

    void upload() {
        boolean success = tryConnect();
        if (!success) {
            return;
        }

        uploadApplicationLogs();
        updatePnlTable();
    }

    private boolean tryConnect() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                return true;
            }
            Class.forName("org.postgresql.Driver");
            dbConnection = DriverManager.getConnection(
                    "jdbc:postgresql://ax9k.cqksghpydg8r.ap-southeast-1.rds.amazonaws.com:5432/postgres",
                    "ax9k",
                    "AX9000!!");
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.error("Couldn't establish DB connection.", e);
            return false;
        }
        return true;
    }

    private void uploadApplicationLogs() {
        try {
            File index = new File("temp");
            for (File log : gzipBookAndFeatureLogs(index)) {
                String fileName = providerSource + "_" + log.getName();
                Path s3Key = Paths.get(batchId, outputPath, date.format(BASIC_ISO_DATE), fileName);
                uploader.upload(log.toPath(), s3Key);
            }
        } catch (IOException e) {
            LOGGER.error("Could not compress log files: " + e.getMessage());
        }
    }

    private List<File> gzipBookAndFeatureLogs(File directory) throws IOException {
        File[] entries = directory.listFiles();
        if (entries == null) {
            return Collections.emptyList();
        }

        List<File> result = new ArrayList<>(entries.length);

        for (File entry : entries) {
            if (isBookOrFeatureLog(entry)) {
                File zipped = createTempFile(entry.getName());
                gzip(entry, zipped);
                result.add(zipped);
            } else {
                result.add(entry);
            }
        }
        return result;
    }

    private boolean isBookOrFeatureLog(File log) {
        String name = log.getName().toLowerCase();

        return name.contains("book") || name.contains("feature");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File createTempFile(String name) throws IOException {
        String tempDirectory = System.getProperty("java.io.tmpdir");
        File result = new File(tempDirectory, name.concat(".gz"));
        result.createNewFile();
        result.deleteOnExit();
        result.setWritable(true);
        return result;
    }

    private void gzip(File source, File zipped) throws IOException {
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(zipped);
             GZIPOutputStream zip = new GZIPOutputStream(output)) {
            input.transferTo(zip);
            zip.flush();
            output.flush();
        }
    }

    private void updatePnlTable() {
        boolean connected = tryConnect();
        if (!connected) {
            return;
        }

        try (Statement statement = dbConnection.createStatement()) {

            String deleteSQL =
                    String.format("DELETE FROM DAILY_REPORT WHERE BATCH_ID = '%s' AND DATE='%s'", batchId, date);
            statement.executeUpdate(deleteSQL);
            String sql =
                    String.format("INSERT INTO DAILY_REPORT (BATCH_ID, ALGO_NAME, PNL, MAX_PNL, MIN_PNL,STOP_COUNT, " +
                                  "TRADES, MARKET_HIGH, MARKET_LOW, WINNING_TRADES, LOOSING_TRADES, DATE, " +
                                  "CREATED_TIMESTAMP) VALUES ('%s','%s', %s, %s, %s, %s, %s, %s, %s,%s,%s,'%s', '%s')",
                                  batchId,
                                  algoName,
                                  pnl,
                                  maxPnl,
                                  minPnl,
                                  hitStopCount,
                                  tradeCount,
                                  dailyTradeHigh,
                                  dailyTradeLow,
                                  winningTrades,
                                  losingTrades,
                                  date,
                                  createdTimestamp);
            LOGGER.info("Deleted existing records. Batch ID: {}, Date: {}", batchId, date);

            statement.executeUpdate(sql);
            LOGGER.info("Successfully updated PostgreSQL database.");
        } catch (SQLException e) {
            throw new RuntimeException("Error updating database.", e);
        }
    }
}

