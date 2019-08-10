package com.ax9k.algorohit;

import com.ax9k.algo.Algo;
import com.ax9k.algo.PeriodicFeatureResult;
import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.FeatureManager;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.time.Time;
import com.ax9k.positionmanager.PositionManager;
import org.apache.logging.log4j.LogManager;

import java.time.Duration;
import java.time.LocalTime;

import static com.ax9k.algo.features.StandardFeatures.ASK_BOOK_VALUE_PRICE_RATIO;
import static com.ax9k.algo.features.StandardFeatures.BID_ASK_VALUE_RATIO;
import static com.ax9k.algo.features.StandardFeatures.BID_BOOK_VALUE_PRICE_RATIO;
import static com.ax9k.algo.features.StandardFeatures.CLOSE;
import static com.ax9k.algo.features.StandardFeatures.EXPONENTIAL_WEIGHTED_MOVING_AVERAGE;
import static com.ax9k.algo.features.StandardFeatures.MAX;
import static com.ax9k.algo.features.StandardFeatures.MIN;
import static com.ax9k.algo.features.StandardFeatures.RSI;
import static com.ax9k.algo.features.StandardFeatures.STANDARD_DEVIATION;
import static com.ax9k.algo.features.StandardFeatures.SUM;
import static com.ax9k.algo.features.StandardFeatures.TOTAL_BOOK_VALUE_MID_PRICE_RATIO;
import static com.ax9k.algo.features.StandardFeatures.TRADE_COUNT;
import static com.ax9k.algo.features.StandardFeatures.TRADE_PRICE;
import static com.ax9k.algo.features.StandardFeatures.TRADE_QUANTITY;

final class TrainingAlgo extends Algo {
    private static final Feature<PeriodicFeatureResult> CLOSE_TRADE_PRICE = result -> result.get("close");
    private static final Duration PERIODIC_UPDATE_DURATION = Duration.ofMinutes(1);

    TrainingAlgo(PositionManager positionManager,
                 TradingDay tradingDay) {
        super("0.1",
              positionManager,
              tradingDay,
              LogManager.getLogger("algoLogger"),
              true,
              true);
        tradingDay.getHeartBeat().setDuration(Duration.ofSeconds(6));
        registerFeatureLogging(PERIODIC_UPDATE_DURATION, LocalTime.of(9, 15), this::recordFeatures);
    }

    private void recordFeatures(PeriodicFeatureResult result) {
        FeatureManager<PeriodicFeatureResult> periodicFeatures1mins = getFeatureManager(PERIODIC_UPDATE_DURATION);

        double minutes = Math.floor(Time.currentTime().toSecondOfDay() / 60d);
        double close = tradeFeatures.get(CLOSE, TRADE_PRICE, PERIODIC_UPDATE_DURATION);
        int periodCount = periodicUpdates.getHistory(PERIODIC_UPDATE_DURATION).getSize();

        double EMA10mTradePrice =
                periodicFeatures1mins.get(EXPONENTIAL_WEIGHTED_MOVING_AVERAGE, CLOSE_TRADE_PRICE, 100);

        double RSI14mTradePrice = INVALID_FEATURE_VALUE;
        if (periodCount >= 140) {
            RSI14mTradePrice = periodicFeatures1mins.get(RSI, CLOSE_TRADE_PRICE, 140);
        }

        double EMA20mTradePrice = INVALID_FEATURE_VALUE;
        if (periodCount >= 200) {
            EMA20mTradePrice = periodicFeatures1mins.get(EXPONENTIAL_WEIGHTED_MOVING_AVERAGE, CLOSE_TRADE_PRICE, 200);
        }

        double EMA30mTradePrice = INVALID_FEATURE_VALUE;
        if (periodCount >= 300) {
            EMA30mTradePrice = periodicFeatures1mins.get(EXPONENTIAL_WEIGHTED_MOVING_AVERAGE, CLOSE_TRADE_PRICE, 300);
        }

        double EMA10MidRatio = INVALID_FEATURE_VALUE;
        if (EMA10mTradePrice != INVALID_FEATURE_VALUE && EMA10mTradePrice != 0 && close != 0) {
            EMA10MidRatio = Math.log(EMA10mTradePrice / close);
        }

        double EMA10EMA20Ratio = INVALID_FEATURE_VALUE;
        if (EMA10mTradePrice != INVALID_FEATURE_VALUE && EMA20mTradePrice != INVALID_FEATURE_VALUE &&
            EMA10mTradePrice != 0 && EMA20mTradePrice != 0) {
            EMA10EMA20Ratio = Math.log(EMA10mTradePrice / EMA20mTradePrice);
        }

        double EMA20EMA30Ratio = INVALID_FEATURE_VALUE;
        if (EMA30mTradePrice != INVALID_FEATURE_VALUE && EMA20mTradePrice != INVALID_FEATURE_VALUE &&
            EMA30mTradePrice != 0 && EMA20mTradePrice != 0) {
            EMA20EMA30Ratio = Math.log(EMA20mTradePrice / EMA30mTradePrice);
        }

        result.record("MINUTES", minutes);
        result.record("close", tradeFeatures.get(CLOSE, TRADE_PRICE, PERIODIC_UPDATE_DURATION));
        result.record("EMA10MidRatio", EMA10MidRatio);
        result.record("EMA10EMA20Ratio", EMA10EMA20Ratio);
        result.record("EMA20EMA30Ratio", EMA20EMA30Ratio);
        result.record("RSI14mTradePrice", RSI14mTradePrice);
        result.record("BID_ASK_VALUE_RATIO", bookFeatures.get(BID_ASK_VALUE_RATIO));
        result.record("TOTAL_BOOK_VALUE_MID_PRICE_RATIO", bookFeatures.get(TOTAL_BOOK_VALUE_MID_PRICE_RATIO));
        result.record("ASK_BOOK_VALUE_PRICE_RATIO", bookFeatures.get(ASK_BOOK_VALUE_PRICE_RATIO));
        result.record("BID_BOOK_VALUE_PRICE_RATIO", bookFeatures.get(BID_BOOK_VALUE_PRICE_RATIO));

        double high = tradeFeatures.get(MAX, TRADE_PRICE, PERIODIC_UPDATE_DURATION);
        double low = tradeFeatures.get(MIN, TRADE_PRICE, PERIODIC_UPDATE_DURATION);
        result.record("range", high - low);
        result.record("volume", tradeFeatures.get(SUM, TRADE_COUNT, PERIODIC_UPDATE_DURATION));
        double sumQuantity = tradeFeatures.get(SUM, TRADE_QUANTITY, PERIODIC_UPDATE_DURATION);
        result.record("sumTradeQuantity", sumQuantity);

        result.record(
                "standardDeviationTradePrice",
                tradeFeatures.get(STANDARD_DEVIATION, TRADE_PRICE, PERIODIC_UPDATE_DURATION)
        );
        result.record("STDDEV10mTradePrice", periodicFeatures1mins.get(STANDARD_DEVIATION, CLOSE_TRADE_PRICE, 100));
    }
}
