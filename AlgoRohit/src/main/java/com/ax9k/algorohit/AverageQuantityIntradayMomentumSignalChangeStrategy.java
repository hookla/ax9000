package com.ax9k.algorohit;

import com.ax9k.algo.Algo;
import com.ax9k.algo.PeriodicFeatureResult;
import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.FeatureManager;
import com.ax9k.algo.features.StandardFeatures;
import com.ax9k.algo.features.set.SetFeature;
import com.ax9k.algo.trading.PeriodicSignalChangeStrategy;
import com.ax9k.algo.trading.SignalContext;
import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.core.time.Time;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalTime;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;

import static com.ax9k.algo.Algo.Signal.NONE;
import static com.ax9k.algo.features.StandardFeatures.EXPONENTIAL_WEIGHTED_MOVING_AVERAGE;
import static com.ax9k.algo.features.StandardFeatures.RSI;

public class AverageQuantityIntradayMomentumSignalChangeStrategy implements PeriodicSignalChangeStrategy {
    private static final String DEFAULT_QUANTITY_FILE_PATH_FORMAT =
            "s3://ax9000-market-data/volume_data_formatted_for_date/%s_60.csv";

    private static final Feature<PeriodicFeatureResult> CLOSE_TRADE_PRICE = result -> result.get("close");
    private static final Predicate<PeriodicFeatureResult> NOT_FILLER = result -> !result.isFiller();

    private final Logger logger;
    private final FeatureManager<OhlcvBar> ohlcvFeatures;
    private final Duration updatePeriod;
    private final LocalTime entryTime;
    private final String quantityFilePathFormat;

    private final double emaCrossTolerance;

    private AverageQuantityData averageQuantityByTime;
    private Algo.Signal regime = NONE;
    private SignalContext currentSignals = new SignalContext(NONE, NONE);

    AverageQuantityIntradayMomentumSignalChangeStrategy(Logger logger,
                                                        FeatureManager<OhlcvBar> ohlcvFeatures,
                                                        Duration updatePeriod,
                                                        LocalTime entryTime,
                                                        String quantityFilePathFormat,
                                                        double emaCrossTolerance) {
        this.logger = logger;
        this.ohlcvFeatures = ohlcvFeatures;
        this.updatePeriod = updatePeriod;
        this.entryTime = entryTime;
        this.quantityFilePathFormat = quantityFilePathFormat != null ?
                                      quantityFilePathFormat :
                                      DEFAULT_QUANTITY_FILE_PATH_FORMAT;
        this.emaCrossTolerance = emaCrossTolerance;
    }

    @Override
    public void recordFeatures(PeriodicFeatureResult result, FeatureManager<PeriodicFeatureResult> periodicFeatures) {
        if (averageQuantityByTime == null) {
            averageQuantityByTime = AverageQuantityData.load(quantityFilePathFormat, Time.currentDate());
            logger.info("Average quantity file loaded. Number of entries: {}",
                        averageQuantityByTime.getNumberOfEntries());
        }

        double close = ohlcvFeatures.get(StandardFeatures.BAR_CLOSE);
        result.record("close", close);

        double quantity = ohlcvFeatures.get(StandardFeatures.BAR_VOLUME);
        double averageQuantity = averageQuantityByTime.getForTime(Time.currentTime().minusMinutes(1));

        result.record("quantity", quantity);
        result.record("averageQuantity", averageQuantity);
        result.record("aboveAverageQuantity", quantity >= averageQuantity);

        result.record("EMA_1", periodicFeatures
                .get(EXPONENTIAL_WEIGHTED_MOVING_AVERAGE, CLOSE_TRADE_PRICE, 10, NOT_FILLER));
        result.record("EMA_2", periodicFeatures
                .get(EXPONENTIAL_WEIGHTED_MOVING_AVERAGE, CLOSE_TRADE_PRICE, 20, NOT_FILLER));
        result.record("EMA_3", periodicFeatures
                .get(EXPONENTIAL_WEIGHTED_MOVING_AVERAGE, CLOSE_TRADE_PRICE, 30, NOT_FILLER));
        result.record("RSI", periodicFeatures
                .get(RSI, CLOSE_TRADE_PRICE, 14, NOT_FILLER));
    }

    @Override
    public SignalContext decideSignal(PeriodicFeatureResult recordedFeatures) {
        double rsi = recordedFeatures.get("RSI");
        boolean aboveAverageQuantity = recordedFeatures.get("aboveAverageQuantity", Boolean.TYPE);

        double ema1 = recordedFeatures.get("EMA_1");
        double ema2 = recordedFeatures.get("EMA_2");
        double ema3 = recordedFeatures.get("EMA_3");

        Algo.Signal newEnterSignal = NONE;
        Algo.Signal newExitSignal = NONE;
        if (validFeatureResults(ema1, ema2, ema3)) {
            double diff1 = ema1 - ema2;
            double diff2 = ema2 - ema3;

            if (diff1 > emaCrossTolerance && diff2 > emaCrossTolerance && rsiInRange(rsi)) {
                newExitSignal = Algo.Signal.BUY;

                if (regime != Algo.Signal.BUY && aboveAverageQuantity) {
                    newEnterSignal = Algo.Signal.BUY;
                }
                setRegime(Algo.Signal.BUY);
            } else if (diff1 < emaCrossTolerance && diff2 < emaCrossTolerance && rsiInRange(rsi)) {
                newExitSignal = Algo.Signal.SELL;
                if (regime != Algo.Signal.SELL && aboveAverageQuantity) {
                    newEnterSignal = Algo.Signal.SELL;
                }
                setRegime(Algo.Signal.SELL);
            } else {
                newExitSignal = NONE;
                setRegime(NONE);
            }
        }

        recordedFeatures.record("regime", regime);
        return updateSignals(newEnterSignal, newExitSignal);
    }

    private SignalContext updateSignals(Algo.Signal enter, Algo.Signal exit) {
        return currentSignals = currentSignals.update(enter, exit);
    }

    private boolean rsiInRange(double rsi) {
        return rsi >= 20 && rsi <= 80;
    }

    private boolean validFeatureResults(double... results) {
        return DoubleStream.of(results)
                           .allMatch(result -> result != SetFeature.INVALID_RESULT);
    }

    public Algo.Signal getRegime() {
        return regime;
    }

    private void setRegime(Algo.Signal regime) {
        if (this.regime != regime) {
            logger.info("Regime change from {} to {}", this.regime, regime);
            this.regime = regime;
        }
    }

    @Override
    public Duration getUpdatePeriod() {
        return updatePeriod;
    }

    @Override
    public LocalTime getEntryTime() {
        return entryTime;
    }

    @Override
    public boolean shouldAttemptSignalChange(EventType lastEventType) {
        return lastEventType == EventType.OHLCV_BAR;
    }
}
