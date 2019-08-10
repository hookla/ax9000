package com.ax9k.algo;

import com.ax9k.algo.features.FeatureManager;
import com.ax9k.core.history.History;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.utils.json.JsonUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class PeriodicFeatureUpdates {
    private final Map<Duration, PeriodicFeatureUpdate> updates = new HashMap<>();

    public void add(TradingSchedule tradingSchedule,
                    Duration period,
                    Consumer<PeriodicFeatureResult> features,
                    LocalTime periodStart) {
        requireNonNull(period, "period");
        requireNonNull(features, "features");
        requireNonNull(periodStart, "periodStart");
        updates.put(period, new PeriodicFeatureUpdate(tradingSchedule, features, period, periodStart));
    }

    public int getPeriodicFeatureUpdatesCount() {
        return updates.size();
    }

    public boolean hasPeriod(Duration period) {
        return updates.containsKey(period);
    }

    public String getPeriods() {
        return updates.keySet().toString();
    }

    public void cancel(Duration period) {
        updates.remove(requireNonNull(period, "period"));
    }

    public FeatureManager<PeriodicFeatureResult> getFeatures(Duration period) {
        return requireExistingUpdate(period).getManager();
    }

    private PeriodicFeatureUpdate requireExistingUpdate(Duration period) {
        requireNonNull(period, "period");
        PeriodicFeatureUpdate result = updates.get(period);
        if (result == null) {
            throw new IllegalArgumentException("No update exists for period: " + period);
        }

        return result;
    }

    public Optional<PeriodicFeatureResult> getLastResult(Duration period) {
        return requireExistingUpdate(period).getLatestResult();
    }

    public boolean updatedInLastCycle(Duration period) {
        return requireExistingUpdate(period).updatedInLastCycle();
    }

    public History<PeriodicFeatureResult> getHistory(Duration period) {
        return requireExistingUpdate(period).getHistory();
    }

    public void runUpdates(Instant now) {
        for (PeriodicFeatureUpdate update : updates.values()) {
            update.runIfNecessary(now);
        }
    }

    @Override
    public int hashCode() {
        return 31 * updates.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        PeriodicFeatureUpdates that = (PeriodicFeatureUpdates) other;
        return Objects.equals(updates, that.updates);
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(getPeriodicFeatureMap());
    }

    private Map<String, Map<String, Object>> getPeriodicFeatureMap() {
        Map<String, Map<String, Object>> result = new HashMap<>(updates.size());
        for (var update : updates.entrySet()) {
            result.put(update.getKey().toString(),
                       update.getValue()
                             .getLatestResult()
                             .map(PeriodicFeatureResult::getFeatures)
                             .orElse(null));
        }
        return result;
    }
}