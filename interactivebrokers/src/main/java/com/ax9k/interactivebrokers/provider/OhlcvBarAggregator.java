package com.ax9k.interactivebrokers.provider;

import com.ax9k.core.history.BasicHistory;
import com.ax9k.core.history.History;
import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.utils.compare.ComparableUtils;
import com.ax9k.utils.logging.ImmutableObjectMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;

class OhlcvBarAggregator {
    static final Duration INPUT_BAR_PERIOD_LENGTH = Duration.ofSeconds(5);

    private static final Logger BARS_LOGGER = LogManager.getLogger("barLogger");
    private static final OhlcvBar INCOMPLETE = null;
    private static final Duration OUTDATED_THRESHOLD = Duration.ofMinutes(30);
    private static final Duration OUTPUT_BAR_PERIOD_LENGTH = Duration.ofMinutes(1);

    private final History<OhlcvBar> history = new BasicHistory<>();

    private OhlcvBar aggregatingData;
    private Instant outputPeriodStart;

    OhlcvBarAggregator() {}

    OhlcvBar aggregateData(OhlcvBar latestBar) {
        Instant inputPeriodStart = latestBar.getTimestamp();
        Instant inputPeriodEnd = inputPeriodStart.plus(INPUT_BAR_PERIOD_LENGTH);

        BARS_LOGGER.info(new ImmutableObjectMessage(latestBar));

        if (outputPeriodStart == null) {
            if (isFirstBarInOhlcvPeriod(latestBar)) {
                outputPeriodStart = inputPeriodStart;
                aggregatingData = latestBar;
            }
            return INCOMPLETE;
        }

        Duration timeSincePeriodStart = Duration.between(outputPeriodStart, inputPeriodEnd);
        if (currentDataIsOutdated(timeSincePeriodStart)) {
            outputPeriodStart = inputPeriodStart;
            aggregatingData = latestBar;
            return INCOMPLETE;
        } else if (aggregatingData == null) {
            aggregatingData = latestBar;
            return INCOMPLETE;
        }

        OhlcvBar completeBar = INCOMPLETE;
        if (atEndOfOhlcvPeriod(timeSincePeriodStart)) {
            completeBar = aggregatingData.aggregate(latestBar);
            history.record(completeBar);
            outputPeriodStart = inputPeriodEnd;
            aggregatingData = null;
        } else if (pastEndOfOhlcvPeriod(timeSincePeriodStart)) {
            completeBar = aggregatingData;
            history.record(completeBar);
            outputPeriodStart = inputPeriodEnd;
            aggregatingData = latestBar;
        } else {
            aggregatingData = aggregatingData.aggregate(latestBar);
        }

        return completeBar;
    }

    private boolean currentDataIsOutdated(Duration timeSincePeriodStart) {
        return ComparableUtils.greaterThanOrEqual(timeSincePeriodStart, OUTDATED_THRESHOLD);
    }

    private boolean pastEndOfOhlcvPeriod(Duration timeSincePeriodStart) {
        return timeSincePeriodStart.compareTo(OUTPUT_BAR_PERIOD_LENGTH) > 0;
    }

    private static boolean atEndOfOhlcvPeriod(Duration timeSincePeriodStart) {
        return timeSincePeriodStart.compareTo(OUTPUT_BAR_PERIOD_LENGTH) == 0;
    }

    private static boolean isFirstBarInOhlcvPeriod(OhlcvBar latestBar) {
        return latestBar.getLocalisedTimestamp().getSecond() == 0;
    }

    History<OhlcvBar> getHistory() {
        return history;
    }
}
