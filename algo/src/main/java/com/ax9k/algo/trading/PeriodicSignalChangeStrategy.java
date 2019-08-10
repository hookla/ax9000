package com.ax9k.algo.trading;

import com.ax9k.algo.PeriodicFeatureResult;
import com.ax9k.algo.features.FeatureManager;
import com.ax9k.core.event.EventType;

import java.time.Duration;
import java.time.LocalTime;

public interface PeriodicSignalChangeStrategy {
    void recordFeatures(PeriodicFeatureResult emptyResult, FeatureManager<PeriodicFeatureResult> periodicFeatures);

    SignalContext decideSignal(PeriodicFeatureResult recordedFeatures);

    Duration getUpdatePeriod();

    LocalTime getEntryTime();

    default boolean shouldAttemptSignalChange(EventType lastEvent) {
        return true;
    }
}
