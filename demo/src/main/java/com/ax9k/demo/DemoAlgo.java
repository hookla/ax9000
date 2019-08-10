package com.ax9k.demo;

import com.ax9k.algo.trading.TradingAlgo;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.time.Time;
import com.ax9k.positionmanager.PositionManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalTime;

import static com.ax9k.algo.features.StandardFeatures.AVERAGE;
import static com.ax9k.algo.features.StandardFeatures.SPREAD;
import static com.ax9k.algo.features.StandardFeatures.SUM;
import static com.ax9k.algo.features.StandardFeatures.TRADE_COUNT;
import static com.ax9k.algo.trading.StandardTradingStrategy.tradeUntil;

public class DemoAlgo extends TradingAlgo {
    private static final Duration THIRTY_SECONDS = Duration.ofSeconds(30);
    private static final Duration TEN_SECONDS = Duration.ofSeconds(10);

    DemoAlgo(PositionManager positionManager, TradingDay tradingDay, Logger algoLogger) {
        super("0.1",
              positionManager,
              tradingDay,
              algoLogger,
              tradeUntil(Time.schedule().lastTradingSession().getEnd(), 1),
              true,
              true);

        tradingDay.getHeartBeat().setDuration(Duration.ofMinutes(5));

        registerFeatureLogging(Duration.ofMinutes(5),
                               LocalTime.of(9, 15),
                               (result) -> result.record("volume",
                                                         tradeFeatures.get(SUM, TRADE_COUNT, THIRTY_SECONDS)));
    }

    private void thirtySecondUpdate() {
        double averageSpreadThirtySeconds = bookFeatures.get(AVERAGE, SPREAD, THIRTY_SECONDS);
    }
/*
    @Override
    public void onMorningSession() {
        if (positionReporter.getCurrentPosition().getContractPosition() == 0 &&
            Time.currentTime().isAfter(FIFTEEN_AFTER_START)) {
            buyAtMarket(2, tradingDay.getBid0());
        }
    }

    @Override
    public void initialiseMorningSession() {
    }

    public void initialiseAfternoonSession() {
        sellAtMarket(1, tradingDay.getBid0());
    }

    @Override
    public void onAfternoonSession() {

    }*/

    @Override
    public void calculateFeatures() {

    }
}
