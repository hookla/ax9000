package com.ax9k.algo.trading;

import com.ax9k.algo.PeriodicFeatureResult;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.positionmanager.PositionManager;
import org.apache.logging.log4j.Logger;

public class PeriodicFeatureTradingAlgo extends TradingAlgo {
    public PeriodicFeatureTradingAlgo(String version,
                                      PositionManager positionManager,
                                      TradingDay tradingDay,
                                      Logger algoLogger,
                                      TradingStrategy tradingStrategy) {
        super(version,
              positionManager,
              tradingDay,
              algoLogger,
              tradingStrategy,
              false,
              false);
    }

    protected void registerSignalChangeStrategy(PeriodicSignalChangeStrategy signalChangeStrategy) {
        registerFeatureLogging(signalChangeStrategy.getUpdatePeriod(),
                               signalChangeStrategy.getEntryTime(),
                               features -> executeSignalChangeStrategy(signalChangeStrategy, features));
    }

    private void executeSignalChangeStrategy(PeriodicSignalChangeStrategy signalChangeStrategy,
                                             PeriodicFeatureResult features) {
        signalChangeStrategy.recordFeatures(features,
                                            getFeatureManager(signalChangeStrategy.getUpdatePeriod()));
        signals = signalChangeStrategy.decideSignal(features);
        features.record("position", positionReporter.getCurrentPosition()
                                                    .getContractPosition());
        features.record("signals", signals);

        if (signals.enterJustChanged()) {
            algoLogger.info("Enter signal changed from {} to {}. Bid0: {}, Ask0: {}, Position: {}",
                            signals.getPreviousEnter(),
                            signals.getEnter(),
                            getBid0(),
                            getAsk0(),
                            positionReporter.getCurrentPosition().getContractPosition());
            signalChanged.trigger();
        }
        if (signals.exitJustChanged()) {
            algoLogger.info("Exit signal changed from {} to {}. Bid0: {}, Ask0: {}, Position: {}",
                            signals.getPreviousExit(),
                            signals.getExit(),
                            getBid0(),
                            getAsk0(),
                            positionReporter.getCurrentPosition().getContractPosition());
            signalChanged.trigger();
        }
    }
}
