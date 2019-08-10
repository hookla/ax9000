package com.ax9k.backtesting;

import com.ax9k.core.marketmodel.MarketEvent;
import com.ax9k.core.marketmodel.Milestone;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.core.time.Time;
import com.ax9k.provider.MarketDataProvider;
import com.ax9k.utils.config.Configuration;
import com.ax9k.utils.config.Configurations;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import static com.ax9k.core.marketmodel.StandardMilestone.wrap;

class LocalFileLoader implements MarketDataProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Logger ERROR_LOG = LogManager.getLogger("error");

    private final TradingDay tradingDay;
    private final FileEventProcessor eventProcessor;
    private final File inputFile;
    private final OhlcvBarAggregator aggregator;
    private final Consumer<OhlcvBar> sendCompleteBarEvent;
    private final int counterCheckpoint;
    private final int maxLineLimit;

    private EventReplay<OhlcvBar> barEvents;
    private LocalDate fileDate;
    private boolean triedLoadingBars;

    LocalFileLoader(TradingDay tradingDay, String path, ProcessingMode mode) {
        this(tradingDay, new File(path).getAbsoluteFile(), mode);
    }

    LocalFileLoader(TradingDay tradingDay, File inputFile, ProcessingMode mode) {
        this.tradingDay = tradingDay;
        this.inputFile = inputFile;
        aggregator = new OhlcvBarAggregator();
        eventProcessor = new FileEventProcessor(tradingDay, mode);
        sendCompleteBarEvent = barEvent -> {
            OhlcvBar completeBar = aggregator.aggregateData(barEvent);
            if (completeBar != null) {
                tradingDay.bar(completeBar);
            }
        };

        Configuration config = Configurations.load(LoadProperties.getPropertiesFile());
        counterCheckpoint = config.get("COUNTER_CHECKPOINT", Integer.class);
        maxLineLimit = config.get("MAX_LINE_LIMIT", Integer.class);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void startRequest(boolean delayUntilMarketOpen) {
        loadFile();
    }

    private void loadFile() {
        LOGGER.info("will load : {}", inputFile);
        try (InputStream fileStream = openFileStream();
             Reader reader = new InputStreamReader(fileStream);
             BufferedReader lineBuffer = new BufferedReader(reader)) {
            parseLines(lineBuffer);
            renameLogs();
        } catch (final Exception unhandleable) {
            throw new RuntimeException(unhandleable);
        }
    }

    private InputStream openFileStream() throws IOException {
        InputStream result = new FileInputStream(inputFile);

        if (inputFile.getName().endsWith(".gz")) {
            result = new GZIPInputStream(result);
        }

        return result;
    }

    private void parseLines(BufferedReader lineBuffer) throws IOException {
        String line = lineBuffer.readLine(); //skip header row

        int counter = 0;
        while (counter < maxLineLimit) {
            {
                int loopCounter = 0;
                StopWatch stopWatch = StopWatch.createStarted();
                while (loopCounter <= counterCheckpoint
                       && counter < maxLineLimit) {

                    line = lineBuffer.readLine();
                    if (line == null) { break; }

                    if (counter == 0) {
                        fileDate = extractDate(line);
                        tradingDay.setTradingDayDate(fileDate);
                        Milestone marketOpen = Time.schedule().marketOpen().getStart();
                        tradingDay.getHeartBeat()
                                  .setLastHeartBeat(addDate(marketOpen, fileDate));
                    }
                    MarketEvent marketEvent = MarketEventParser.parse(line);

                    if (!triedLoadingBars) {
                        try {
                            barEvents = HistoricBarDataLoader.load(fileDate);
                        } catch (RuntimeException noBarEvents) {
                            ERROR_LOG.warn("No bar events available", noBarEvents);
                        }
                        triedLoadingBars = true;
                    }
                    if (barEvents != null) {
                        barEvents.replayUntil(marketEvent.getEventTimestamp())
                                 .forEach(sendCompleteBarEvent);
                    }

                    tradingDay.getHeartBeat().massageHeart(marketEvent.getEventTimestamp());
                    eventProcessor.processExchangeMessage(marketEvent);
                    loopCounter++;
                    counter++;
                }

                if (line == null) { break; }
                long timeMicros = Math.max(1, stopWatch.getTime(TimeUnit.MICROSECONDS));
                int rate = (int) Math.round(counterCheckpoint / (timeMicros / 1_000_000d));

                double totalMemory = Runtime.getRuntime().totalMemory();
                double usedMemory = totalMemory - Runtime.getRuntime().freeMemory();
                LOGGER.info("last event TS : {}, {}@{}/sec, total mb {}, used mb {}",
                            tradingDay.getLocalisedLastEventTime(),
                            counter,
                            rate,
                            toMegabytes(totalMemory),
                            toMegabytes(usedMemory));
            }
        }
        Milestone marketClose = Time.schedule().marketClose().getStart();
        if (marketClose == null) {
            marketClose = wrap(LocalTime.of(23, 59));
        }
        tradingDay.getHeartBeat().massageHeart(addDate(marketClose, fileDate));
    }

    private static Instant addDate(Milestone milestone, LocalDate date) {
        return Time.internationalise(LocalDateTime.of(date, milestone.getTime()));
    }

    private LocalDate extractDate(String line) {
        return MarketEventParser.parse(line).getLocalisedEventTimestamp().toLocalDate();
    }

    private long toMegabytes(double bytes) {
        return Math.round(bytes / 1024 / 1024);
    }

    private void renameLogs() {
        LocalDate fileDate = Time.currentDate();
        File dataLogFile = new File("BookState.log");
        File renamedDataLogFile = new File("BookState_" + fileDate + ".log");
        if (!dataLogFile.renameTo(renamedDataLogFile)) {
            LOGGER.info("couldn't rename book state log file");
        }

        File featureLogFile = new File("FeatureArray.log");
        File renamedFeatureLogFile = new File("FeatureArray_" + fileDate + ".log");
        if (!featureLogFile.renameTo(renamedFeatureLogFile)) {
            LOGGER.info("couldn't rename feature log file");
        }
    }

    @Override
    public void stopRequest() {
    }

    @Override
    public String getSource() {
        return inputFile.getName();
    }

    public LocalDate getDate() {
        return fileDate;
    }

    public String getSymbol() {
        return inputFile.getName();
    }
}