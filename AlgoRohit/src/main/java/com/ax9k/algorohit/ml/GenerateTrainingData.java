package com.ax9k.algorohit.ml;

import com.ax9k.algo.PeriodicFeatureResult;
import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.FeatureManager;
import com.ax9k.algo.trading.NullTradingStrategy;
import com.ax9k.algo.trading.TradingAlgo;
import com.ax9k.algorohit.AverageQuantityData;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.time.Time;
import com.ax9k.positionmanager.PositionManager;
import org.apache.logging.log4j.LogManager;

import java.time.Duration;
import java.time.LocalTime;
import java.util.function.Predicate;

import static com.ax9k.algo.features.StandardFeatures.ASK_BOOK_VALUE_PRICE_RATIO;
import static com.ax9k.algo.features.StandardFeatures.BID_ASK_VALUE_RATIO;
import static com.ax9k.algo.features.StandardFeatures.BID_BOOK_VALUE_PRICE_RATIO;
import static com.ax9k.algo.features.StandardFeatures.CLOSE;
import static com.ax9k.algo.features.StandardFeatures.EXPONENTIAL_WEIGHTED_MOVING_AVERAGE;
import static com.ax9k.algo.features.StandardFeatures.MAX;
import static com.ax9k.algo.features.StandardFeatures.MIN;
import static com.ax9k.algo.features.StandardFeatures.RSI;
import static com.ax9k.algo.features.StandardFeatures.SUM;
import static com.ax9k.algo.features.StandardFeatures.TOTAL_BOOK_VALUE_MID_PRICE_RATIO;
import static com.ax9k.algo.features.StandardFeatures.TRADE_PRICE;
import static com.ax9k.algo.features.StandardFeatures.TRADE_PRICE_X_QUANTITY;
import static com.ax9k.algo.features.StandardFeatures.TRADE_QUANTITY;

class GenerateTrainingData extends TradingAlgo {
    private static final Feature<PeriodicFeatureResult> CLOSE_TRADE_PRICE = result -> result.get("close");
    private static final Predicate<PeriodicFeatureResult> NOT_FILLER = result -> !result.isFiller();

    final Duration periodicUpdateDuration;

    private AverageQuantityData averageQuantityByTime;
    private Signal regime = Signal.NONE;

    GenerateTrainingData(PositionManager positionManager,
                         TradingDay tradingDay,
                         int periodicUpdateDurationSeconds) {
        super("0.11",
              positionManager,
              tradingDay,
              LogManager.getLogger("algoLogger"),
              NullTradingStrategy.INSTANCE,
              false,
              true);
        this.periodicUpdateDuration = Duration.ofSeconds(periodicUpdateDurationSeconds);
        double emaCrossTolerance = 0.01;
        tradingDay.getHeartBeat().setDuration(Duration.ofSeconds(1));
        registerFeatureLogging(periodicUpdateDuration, LocalTime.of(9, 15), this::recordFeatures);
    }

    private void recordFeatures(PeriodicFeatureResult result) {
        double minutes = Math.floor(Time.currentTime().toSecondOfDay() / 60d);
        result.record("MINUTES", minutes);

        double high = tradeFeatures.get(MAX, TRADE_PRICE, periodicUpdateDuration);
        double low = tradeFeatures.get(MIN, TRADE_PRICE, periodicUpdateDuration);
        result.record("range", high - low);
        double sumQuantity = tradeFeatures.get(SUM, TRADE_QUANTITY, periodicUpdateDuration);
        double sumPriceXQuantity = tradeFeatures.get(SUM, TRADE_PRICE_X_QUANTITY, periodicUpdateDuration);
        result.record("sumTradeQuantity", sumQuantity);

        result.record("BID_ASK_VALUE_RATIO", bookFeatures.get(BID_ASK_VALUE_RATIO));
        result.record("TOTAL_BOOK_VALUE_MID_PRICE_RATIO", bookFeatures.get(TOTAL_BOOK_VALUE_MID_PRICE_RATIO));
        result.record("ASK_BOOK_VALUE_PRICE_RATIO", bookFeatures.get(ASK_BOOK_VALUE_PRICE_RATIO));
        result.record("BID_BOOK_VALUE_PRICE_RATIO", bookFeatures.get(BID_BOOK_VALUE_PRICE_RATIO));

        //BASE FEATURES
        double close = tradeFeatures.get(CLOSE, TRADE_PRICE, periodicUpdateDuration);

        result.record("close", close);

        double quantity = tradeFeatures.get(SUM, TRADE_QUANTITY, periodicUpdateDuration);
        double averageQuantity = averageQuantityByTime.getForTime(Time.currentTime().minusMinutes(1));

        result.record("quantity", quantity);
        result.record("averageQuantity", averageQuantity);

        FeatureManager<PeriodicFeatureResult> periodicFeatures = getFeatureManager(periodicUpdateDuration);
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
    public void onFirstEvent() {
        averageQuantityByTime = AverageQuantityData.load(Time.currentDate());
    }
}
