package com.ax9k.algorohit.ml;

import com.ax9k.algo.PeriodicFeatureResult;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.core.time.Time;
import com.ax9k.positionmanager.PositionManager;

import java.time.Duration;
import java.util.Map;

class AmazonMLAlgo extends GenerateTrainingData {
    private final ModelClient model;
    private final int stopBuffer;
    private final double entryThreshold, exitThreshold;

    AmazonMLAlgo(PositionManager positionManager,
                 TradingDay tradingDay,
                 String modelId,
                 int stopBuffer,
                 int periodicUpdateDurationSeconds,
                 double entryThreshold,
                 double exitThreshold) {
        super(positionManager,
              tradingDay,
              periodicUpdateDurationSeconds);
        tradingDay.getHeartBeat().setDuration(Duration.ofSeconds(6));
        model = new ModelClient(modelId);

        this.stopBuffer = stopBuffer;
        this.entryThreshold = entryThreshold;
        this.exitThreshold = exitThreshold;

        TradingSchedule tradingSchedule = Time.schedule();
        registerPhaseEventCallback(tradingSchedule.phaseForIdentifier("morning"), this::tradeIfFeaturesUpdated);
        registerPhaseEventCallback(tradingSchedule.phaseForIdentifier("afternoon"), this::tradeIfFeaturesUpdated);
    }

    private void tradeIfFeaturesUpdated() {
        ifPeriodicFeaturesUpdated(periodicUpdateDuration, (__) -> tradingLogic());
    }

    private void tradingLogic() {
        double position = positionReporter.getCurrentPosition().getContractPosition();
        double quantity = quantityToTrade();
        updateStopPrice();
        trade(position, quantity);
    }

    private double quantityToTrade() {
        return 1;

        // return position == 0 ? 1 : 2;
        //TODO calculating Pnl when the exiting and entering a position is bugged.
    }

    private void updateStopPrice() {
        double oldStopPrice = getStopPrice();
        if (isShort()) {
            if (oldStopPrice > tradingDay.getAsk0() + stopBuffer) {
                double newStopPrice = tradingDay.getAsk0() + stopBuffer;
                setStopPrice(newStopPrice);
                algoLogger.info("Moved stop price down from {} to {}.", oldStopPrice, newStopPrice);
            }
        } else if (isLong()) {
            if (oldStopPrice < tradingDay.getBid0() - stopBuffer) {
                double newStopPrice = tradingDay.getAsk0() - stopBuffer;
                setStopPrice(newStopPrice);
                algoLogger.info("Moved stop price up from {} to {}.", oldStopPrice, newStopPrice);
            }
        }
    }

    private void trade(double position, double quantity) {
        Prediction prediction = getPrediction(periodicUpdateDuration);
        double probability = prediction.getProbability();
        switch (prediction.getChange()) {
            case UP:
                if (position == 0 && probability > entryThreshold) {
                    buyAtMarket(quantity, tradingDay.getAsk0() - stopBuffer);
                } else if (position == -1 && probability > exitThreshold) {
                    buyAtMarket(quantity, tradingDay.getAsk0() - stopBuffer);
                }
                break;
            case DOWN:
                if (position == 0 && probability > entryThreshold) {
                    sellAtMarket(quantity, tradingDay.getBid0() + stopBuffer);
                } else if (position == 1 && probability > exitThreshold) {
                    sellAtMarket(quantity, tradingDay.getBid0() + stopBuffer);
                }
                break;
            default:
        }
    }

    private Prediction getPrediction(Duration periodicUpdatePeriod) {
        lock.lock();
        try {
            return getLastFeaturesLogged(periodicUpdatePeriod)
                    .map(PeriodicFeatureResult::getFeatures)
                    .map(this::getPrediction)
                    .orElse(Prediction.NONE);
        } finally {
            lock.unlock();
        }
    }

    private Prediction getPrediction(Map<String, ?> predictionFeatures) {
        Phase currentPhase = tradingDay.getPhase();
        if (!currentPhase.isTradingSession()) {
            return Prediction.NONE;
        }

        return model.predict(predictionFeatures);
    }
}
