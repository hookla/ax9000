package com.ax9k.algo;

import com.ax9k.algo.features.FeatureManager;
import com.ax9k.algo.features.set.SetFeature;
import com.ax9k.core.event.Event;
import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.Trade;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import com.ax9k.core.time.Time;
import com.ax9k.positionmanager.PositionManager;
import com.ax9k.positionmanager.PositionReporter;
import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;

import static org.apache.commons.lang3.Validate.notNull;

@JsonPropertyOrder({ "algoName" })
public abstract class Algo implements Observer {
    protected static final double INVALID_FEATURE_VALUE = Double.MIN_VALUE;
    protected static final String SPACER = "------------------------------";
    protected static final Logger ERROR_LOG = LogManager.getLogger("error");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Logger FEATURE_LOGGER = LogManager.getLogger("featureLogger");
    private static final Path END_OF_DAY_REPORT_FILE = Paths.get("temp", "EndOfDayReport.json");
    private static final String BASE_ALGO_VERSION = "StandardAlgo:v1.0";
    protected final Logger algoLogger;
    protected final Lock lock = new ReentrantLock();
    protected final PositionReporter positionReporter;
    protected final TradingDay tradingDay;
    protected final FeatureManager<Trade> tradeFeatures;
    protected final FeatureManager<OrderBook> bookFeatures;
    protected final PeriodicFeatureUpdates periodicUpdates = new PeriodicFeatureUpdates();
    private final boolean logFeatureArray;
    private final boolean logFeatureArrayOnHeartBeat;
    private final String version;
    private final Trigger closedTrigger;
    private final Trigger firstEventTrigger;
    private final Map<String, Double> features = new HashMap<>();
    private final Map<Phase, Runnable> phaseCallbacks = new HashMap<>();
    private final Map<Phase, Trigger> phaseChangeTriggers = new HashMap<>();
    protected Event triggeringEvent;
    private Phase previousPhase;

    public Algo(String version,
                PositionManager positionManager,
                TradingDay tradingDay,
                Logger algoLogger,
                boolean logFeatureArray,
                boolean logFeatureArrayOnHeartBeat) {
        this.version = version;
        this.positionReporter = positionManager.getPositionReporter();
        this.tradingDay = tradingDay;
        this.algoLogger = algoLogger;
        this.logFeatureArray = logFeatureArray;
        this.logFeatureArrayOnHeartBeat = logFeatureArrayOnHeartBeat;

        bookFeatures = new FeatureManager<>(tradingDay::getOrderBookHistory);
        tradeFeatures = new FeatureManager<>(tradingDay::getTradeHistory);

        firstEventTrigger = new Trigger(() -> {
            lock.lock();
            try {
                algoLogger.info(SPACER);
                algoLogger.info("SETUP");
                algoLogger.info(getAlgoName());
                algoLogger.info("Date: {} {}", Time.currentDate(), Time.currentTime());
                algoLogger.info("Heartbeat: {}. PID: {}",
                                tradingDay.getHeartBeat().getPeriod(),
                                ProcessHandle.current().pid());
                algoLogger.info("Registered Periodic Updates: {}", periodicUpdates.getPeriods());
                algoLogger.info("PnL Broker / AX9k: {} / {} ",
                                positionReporter.getBrokerRealisedPnl(), positionReporter.getPnl());
                algoLogger.info("Position: {}", positionReporter.getCurrentPosition().getContractPosition());
                supplementalStartupInfo();
            } finally {
                lock.unlock();
            }
            onFirstEvent();
        });

        closedTrigger = new Trigger(() -> {
            logNewPhase();
            initialiseClosed();

            algoLogger.info(this::endOfDayReport);
            algoLogger.info(SPACER);

            persistEndOfDayReport();
        });
    }

    public String getAlgoName() {
        return getClass().getSimpleName();
    }

    private void logNewPhase() {
        algoLogger.info(SPACER);
        algoLogger.info(SPACER);
        algoLogger.info(tradingDay::getPhase);
    }

    private void persistEndOfDayReport() {
        try (BufferedWriter writer = Files.newBufferedWriter(END_OF_DAY_REPORT_FILE)) {
            writer.write(endOfDayReport());
        } catch (IOException e) {
            LOGGER.error("Error writing end of day report", e);
        }
    }

    private String endOfDayReport() {
        lock.lock();
        try {
            var report = Map.of("algo", this, "positionReporter", positionReporter);
            return JsonUtils.toJsonString(report);
        } finally {
            lock.unlock();
        }
    }

    protected void onFirstEvent() {
    }

    protected void supplementalStartupInfo() {

    }

    protected void initialiseClosed() {
    }

    protected void logFeaturesToAlgoLogger() {
        lock.lock();
        try {
            if (features.size() > 0) {
                algoLogger.info("features : {}", features);
            }

            if (getPeriodicUpdates().getPeriodicFeatureUpdatesCount() > 0) {
                algoLogger.info("Periodic features : {}", getPeriodicUpdates().toString());
            }
        } finally {
            lock.unlock();
        }
    }

    @JsonIgnore
    public PeriodicFeatureUpdates getPeriodicUpdates() {
        lock.lock();
        try {
            return periodicUpdates;
        } finally {
            lock.unlock();
        }
    }

    protected boolean validFeatureResults(double... results) {
        return DoubleStream.of(results)
                           .allMatch(this::validFeatureResult);
    }

    protected boolean validFeatureResult(double result) {
        return result != SetFeature.INVALID_RESULT;
    }

    protected void registerPhaseChangeTrigger(Phase phase, Runnable onFirstChange) {
        phaseChangeTriggers.put(notNull(phase, "phase"),
                                new Trigger(notNull(onFirstChange, "onFirstChange")));
    }

    protected void deregisterPhaseChangeTrigger(Phase phase) {
        phaseChangeTriggers.remove(notNull(phase));
    }

    protected void registerPhaseEventCallback(Phase phase, Runnable onPhase) {
        phaseCallbacks.put(notNull(phase, "phase"), notNull(onPhase, "onPhase"));
    }

    protected void deregisterPhaseEventCallback(Phase phase) {
        phaseCallbacks.remove(notNull(phase));
    }

    protected void registerFeatureLogging(Duration period, LocalTime periodStart,
                                          Consumer<PeriodicFeatureResult> features) {
        getPeriodicUpdates().add(Time.schedule(), period, features, periodStart);
    }

    private void cancelFeatureLogging(Duration period) {
        algoLogger.info("{} : Cancelling periodic updates", Time.currentTime());
        getPeriodicUpdates().cancel(period);
    }

    protected FeatureManager<PeriodicFeatureResult> getFeatureManager(Duration period) {
        return getPeriodicUpdates().getFeatures(period);
    }

    protected boolean periodicUpdatePerformed(Duration period) {
        return getPeriodicUpdates().updatedInLastCycle(period);
    }

    protected void ifPeriodicFeaturesUpdated(Duration period, Consumer<PeriodicFeatureResult> consumer) {
        Optional<PeriodicFeatureResult> lastResult = Optional.empty();

        lock.lock();
        try {
            if (periodicUpdates.hasPeriod(period) &&
                periodicUpdates.updatedInLastCycle(period)) {
                lastResult = periodicUpdates.getLastResult(period);
            }
        } finally {
            lock.unlock();
        }

        lastResult.ifPresent(consumer);
    }

    protected void declareFeatureOutput(String feature) {
        lock.lock();
        try {
            Validate.validState(!firstEventTrigger.isTriggered(),
                                "feature '%s' must be declared on or before the first event", feature);

            features.put(feature, INVALID_FEATURE_VALUE);
        } finally {
            lock.unlock();
        }
    }

    protected void updateFeatureOutput(String feature, Double value) {
        lock.lock();
        try {
            Validate.validState(features.containsKey(feature),
                                "feature '%s' must be declared before a value is set", feature);
            features.put(feature, value);
        } finally {
            lock.unlock();
        }
    }

    protected double getFeature(String feature) {
        return features.get(feature);
    }

    @JsonIgnore
    public Logger getAlgoLogger() {
        return algoLogger;
    }

    protected Optional<PeriodicFeatureResult> getLastFeaturesLogged(Duration period) {
        return getPeriodicUpdates().getLastResult(period);
    }

    public void update(Observable o, Object arg) {
        StopWatch stopWatch = StopWatch.createStarted();
        notNull(arg, "Update argument cannot be null");
        Validate.isInstanceOf(Event.class, arg, "Update argument must of type Event");
        Event event = (Event) arg;
        EventType eventType = event.getType();

        lock.lock();
        try {
            triggeringEvent = event;
            firstEventTrigger.trigger();
        } finally {
            lock.unlock();
        }

        if (o instanceof TradingDay) {
            processBookUpdate(eventType);
            calculateFeatures();
            LOGGER.trace("finished calculateFeatures() {} micro sec.", stopWatch.getTime(TimeUnit.MICROSECONDS));
        }

        lock.lock();
        try {
            if (shouldRunPeriodicUpdates(tradingDay.getPhase(), eventType)) {
                periodicUpdates.runUpdates(Time.now());
            }
        } finally {
            lock.unlock();
        }

        LOGGER.trace("processed Algo actions {} micro sec.", stopWatch.getTime(TimeUnit.MICROSECONDS));
    }

    protected void calculateFeatures() {
    }

    protected boolean shouldRunPeriodicUpdates(Phase phase, EventType triggeringEventType) {
        return phase.isMarketOpen() || phase.isTradingSession();
    }

    protected void processBookUpdate(EventType eventType) {
        if (!tradingDay.isReady()) {
            return;
        }

        lock.lock();
        try {
            if (logThisPeriod() && logFeatureArray &&
                (logFeatureArrayOnHeartBeat || !eventType.equals(EventType.HEARTBEAT))) {
                FEATURE_LOGGER.info(this::getFeaturesJson);
            }
        } finally {
            lock.unlock();
        }

        Phase currentPhase = tradingDay.getPhase();

        if (previousPhase != null && marketJustClosed(currentPhase)) {
            closedTrigger.trigger();
        } else if (!currentPhase.equals(previousPhase)) {
            logPhaseChange();
        }

        Trigger trigger = phaseChangeTriggers.get(currentPhase);
        if (trigger != null) {
            trigger.trigger();
        }

        Runnable callback = phaseCallbacks.get(currentPhase);
        if (callback != null) {
            callback.run();
        }

        previousPhase = currentPhase;
    }

    @JsonIgnore
    public String getFeaturesJson() {
        lock.lock();
        try {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("eventTime", tradingDay.getLastEventTimeString());
            output.put("triggeringEventType", triggeringEvent != null ? triggeringEvent.getType() : null);
            output.putAll(features);
            return JsonUtils.toJsonString(output);
        } finally {
            lock.unlock();
        }
    }

    private void logPhaseChange() {
        logNewPhase();
        logPosition();
    }

    private void logPosition() {
        algoLogger.info(positionReporter::toString);
        algoLogger.info(SPACER);
    }

    private boolean marketJustClosed(Phase currentPhase) {
        return !currentPhase.isMarketOpen() && previousPhase.isMarketOpen();
    }

    private boolean logThisPeriod() {
        return tradingDay.getPhase().isTradingSession();
    }

    protected boolean validBook() {
        return getAsk0() > 0 && getBid0() > 0;
    }

    protected double getBid0() {
        return tradingDay.getBid0();
    }

    protected double getAsk0() {
        return tradingDay.getAsk0();
    }

    public String getAlgoLongName() {
        return BASE_ALGO_VERSION + ":" + getAlgoName() + ":v" + version;
    }

    public boolean isLogFeatureArray() {
        lock.lock();
        try {
            return logFeatureArray;
        } finally {
            lock.unlock();
        }
    }

    public boolean isLogFeatureArrayOnHeartBeat() {
        lock.lock();
        try {
            return logFeatureArrayOnHeartBeat;
        } finally {
            lock.unlock();
        }
    }

    public String getVersion() {
        lock.lock();
        try {
            return version;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            return JsonUtils.toJsonString(this);
        } finally {
            lock.unlock();
        }
    }

    public enum Signal {BUY, SELL, NONE}
}
