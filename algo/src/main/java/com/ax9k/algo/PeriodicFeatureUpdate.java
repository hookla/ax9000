package com.ax9k.algo;

import com.ax9k.algo.features.FeatureManager;
import com.ax9k.core.history.BasicHistory;
import com.ax9k.core.history.History;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.core.time.Time;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.apache.commons.lang3.Validate.notNull;

class PeriodicFeatureUpdate {
    private static final Logger PERIODIC_FEATURE_LOGGER = LogManager.getLogger("periodicFeatureLogger");

    private final History<PeriodicFeatureResult> history = new BasicHistory<>();
    private final FeatureManager<PeriodicFeatureResult> manager = new FeatureManager<>(this::getHistory);

    private final TradingSchedule tradingSchedule;
    private final Consumer<PeriodicFeatureResult> featuresRecorder;
    private final Duration period;
    private final LocalTime periodStart;

    private Instant lastUpdate;
    private boolean updated;

    PeriodicFeatureUpdate(TradingSchedule tradingSchedule,
                          Consumer<PeriodicFeatureResult> featureRecorder,
                          Duration period,
                          LocalTime periodStart) {
        this.tradingSchedule = notNull(tradingSchedule, "tradingSchedule");
        this.featuresRecorder = notNull(featureRecorder, "featureRecorder");
        this.period = notNull(period, "period");
        this.periodStart = notNull(periodStart, "periodStart");
    }

    void runIfNecessary(Instant now) {
        if (updateDue(now)) {
            performUpdates(now);
            updated = true;
        } else {
            updated = false;
        }
    }

    private boolean updateDue(Instant now) {
        if (lastUpdate == null) {
            lastUpdate = initialiseDate();
        }
        return Duration.between(lastUpdate, now).compareTo(period) >= 0;
    }

    private Instant initialiseDate() {
        return Time.internationalise(Time.today().with(periodStart));
    }

    private void performUpdates(Instant now) {
        if (lastUpdate == null) {
            lastUpdate = initialiseDate();
        }

        long updatesToCatchUp = Duration.between(lastUpdate, now).dividedBy(period);

        if (updatesToCatchUp == 0) {
            return;
        }

        if (updatesToCatchUp == 1) {
            Instant periodEnd = lastUpdate.plus(period);
            updated = logCurrentFeatures(lastUpdate, periodEnd);
            lastUpdate = periodEnd;
            return;
        }

        Map<String, Object> filler = zeroedOutFeatures();
        Instant currentPeriodStart = lastUpdate;
        Instant currentPeriodEnd = currentPeriodStart.plus(period);
        for (int i = 0; i < updatesToCatchUp; i++) {
            if (i == updatesToCatchUp - 1) {
                updated = logCurrentFeatures(currentPeriodStart, currentPeriodEnd);
                lastUpdate = currentPeriodEnd;
                return;
            } else {
                updated = logFiller(filler, currentPeriodStart, currentPeriodEnd);
                currentPeriodStart = currentPeriodEnd;
                currentPeriodEnd = currentPeriodStart.plus(period);
            }
        }
    }

    private boolean logCurrentFeatures(Instant periodStart, Instant periodEnd) {
        PeriodicFeatureResult result = new PeriodicFeatureResult(periodStart, periodEnd);
        result.record("filler", false);

        if (tradingSchedule.phaseForTime(periodStart).isTradingSession()) {
            history.record(result);
            featuresRecorder.accept(result);
            PERIODIC_FEATURE_LOGGER.info(result);
            return true;
        }
        return false;
    }

    private Map<String, Object> zeroedOutFeatures() {
        PeriodicFeatureResult features = new PeriodicFeatureResult(Time.now(), Time.now());

        featuresRecorder.accept(features);
        Map<String, Object> original = features.getFeatures();
        Map<String, Object> filler = new HashMap<>(original.size());

        for (Map.Entry<String, Object> entry : original.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Number) {
                filler.put(entry.getKey(), 0);
            } else if (value instanceof Boolean) {
                filler.put(entry.getKey(), false);
            } else if (value instanceof String) {
                String key = entry.getKey();
                if (!key.equals("periodStart") && !key.equals("periodEnd")) {
                    filler.put(key, null);
                }
            } else {
                filler.put(entry.getKey(), null);
            }
        }

        filler.put("filler", true);

        return filler;
    }

    private boolean logFiller(Map<String, Object> filler, Instant periodStart, Instant periodEnd) {
        PeriodicFeatureResult result = new PeriodicFeatureResult(periodStart, periodEnd);

        if (tradingSchedule.phaseForTime(periodStart).isTradingSession()) {
            history.record(result);
            result.recordAll(filler);
            PERIODIC_FEATURE_LOGGER.info(result);
            return true;
        }
        return false;
    }

    boolean updatedInLastCycle() {
        return updated;
    }

    FeatureManager<PeriodicFeatureResult> getManager() {
        return manager;
    }

    History<PeriodicFeatureResult> getHistory() {
        return history;
    }

    Optional<PeriodicFeatureResult> getLatestResult() {
        return history.getLatest();
    }
}
