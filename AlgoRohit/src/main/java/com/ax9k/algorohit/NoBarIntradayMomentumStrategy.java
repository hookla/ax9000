package com.ax9k.algorohit;

import com.ax9k.algo.Algo;
import com.ax9k.algo.PeriodicFeatureResult;
import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.FeatureManager;
import com.ax9k.algo.features.Parameters;
import com.ax9k.algo.features.set.SetFeature;
import com.ax9k.algo.trading.PeriodicSignalChangeStrategy;
import com.ax9k.algo.trading.SignalContext;
import com.ax9k.cex.data.Ticker;
import com.ax9k.core.history.History;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;

import static com.ax9k.algo.Algo.Signal.NONE;
import static com.ax9k.algo.features.Parameters.params;
import static com.ax9k.algo.features.StandardFeatures.CLOSE;
import static com.ax9k.algo.features.StandardFeatures.EXPONENTIAL_WEIGHTED_MOVING_AVERAGE;
import static com.ax9k.algo.features.StandardFeatures.MID;
import static com.ax9k.algo.features.StandardFeatures.RSI;

public class NoBarIntradayMomentumStrategy implements PeriodicSignalChangeStrategy {
    private static final Parameters ROUNDING_PRECISION = params("precision", 5);
    private static final Feature<PeriodicFeatureResult> LAST_MID_PRICE = result -> result.get("lastMidPrice");
    private static final Feature<PeriodicFeatureResult> LAST_TRADE_PRICE = result -> result.get("lastTradePrice");

    private static final Predicate<PeriodicFeatureResult> NOT_FILLER = result -> !result.isFiller();

    private final Logger logger;
    private final FeatureManager<OrderBook> bookFeatures;
    private final Duration updatePeriod;
    private final LocalTime entryTime;
    private final History<Ticker> trades;

    private final double emaPosCrossTolerance;
    private final double emaNegCrossTolerance;

    private Algo.Signal regime = NONE;
    private SignalContext currentSignals = new SignalContext(NONE, NONE);

    NoBarIntradayMomentumStrategy(Logger logger,
                                  FeatureManager<OrderBook> bookFeatures,
                                  History<Ticker> trades,
                                  Duration updatePeriod,
                                  LocalTime entryTime,
                                  double emaCrossTolerance) {
        this.logger = logger;
        this.bookFeatures = bookFeatures;
        this.trades = trades;
        this.updatePeriod = updatePeriod;
        this.entryTime = entryTime;
        this.emaPosCrossTolerance = emaCrossTolerance;
        this.emaNegCrossTolerance = -1 * emaCrossTolerance;
    }

    @Override
    public void recordFeatures(PeriodicFeatureResult result, FeatureManager<PeriodicFeatureResult> periodicFeatures) {
        Optional<Ticker> lastTrade = trades.getLatest();

        Feature<PeriodicFeatureResult> singleValueFeature;
        if (lastTrade.isPresent()) {
            double price = lastTrade.get().getPrice();
            result.record("lastTradePrice", price);
            singleValueFeature = LAST_TRADE_PRICE;
        } else {
            double lastMidPrice = bookFeatures.get(CLOSE, MID, updatePeriod);
            result.record("lastMidPrice", lastMidPrice);
            singleValueFeature = LAST_MID_PRICE;
        }

        result.record("EMA_1", periodicFeatures
                .get(EXPONENTIAL_WEIGHTED_MOVING_AVERAGE, singleValueFeature,
                     10, NOT_FILLER,
                     ROUNDING_PRECISION));
        result.record("EMA_2", periodicFeatures
                .get(EXPONENTIAL_WEIGHTED_MOVING_AVERAGE, singleValueFeature,
                     20, NOT_FILLER,
                     ROUNDING_PRECISION));
        result.record("EMA_3", periodicFeatures
                .get(EXPONENTIAL_WEIGHTED_MOVING_AVERAGE, singleValueFeature,
                     30, NOT_FILLER,
                     ROUNDING_PRECISION));
        result.record("RSI", periodicFeatures
                .get(RSI, singleValueFeature, 14, NOT_FILLER, ROUNDING_PRECISION));
    }

    @Override
    public SignalContext decideSignal(PeriodicFeatureResult recordedFeatures) {
        double rsi = recordedFeatures.get("RSI");

        double ema1 = recordedFeatures.get("EMA_1");
        double ema2 = recordedFeatures.get("EMA_2");
        double ema3 = recordedFeatures.get("EMA_3");

        Algo.Signal newEnterSignal = NONE;
        Algo.Signal newExitSignal = NONE;
        if (validFeatureResults(ema1, ema2, ema3)) {
            double diff1 = ema1 - ema2;
            double diff2 = ema2 - ema3;

            if (diff1 > emaPosCrossTolerance && diff2 > emaPosCrossTolerance && rsiInRange(rsi)) {
                newExitSignal = Algo.Signal.BUY;

                if (regime != Algo.Signal.BUY) {
                    newEnterSignal = Algo.Signal.BUY;
                }
                setRegime(Algo.Signal.BUY);
            } else if (diff1 < emaNegCrossTolerance && diff2 < emaNegCrossTolerance && rsiInRange(rsi)) {
                newExitSignal = Algo.Signal.SELL;
                if (regime != Algo.Signal.SELL) {
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
}
