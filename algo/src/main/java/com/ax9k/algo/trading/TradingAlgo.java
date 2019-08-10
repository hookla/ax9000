package com.ax9k.algo.trading;

import com.ax9k.algo.Algo;
import com.ax9k.algo.Trigger;
import com.ax9k.core.event.EventType;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.core.time.Time;
import com.ax9k.positionmanager.OrderReceiver;
import com.ax9k.positionmanager.PositionManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Observable;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class TradingAlgo extends Algo {
    protected final Trigger signalChanged = new Trigger();
    private final TradingStrategy tradingStrategy;
    private final OrderReceiver orderReceiver;
    private final Trigger aboutToCloseTrigger;
    protected SignalContext signals = new SignalContext(Signal.NONE, Signal.NONE);
    protected double cashoutUpperLevel = Double.MAX_VALUE;
    protected double cashoutLowerLevel = Integer.MIN_VALUE;
    protected double maxGiveBack = Double.MAX_VALUE;
    protected int stopBuffer = 100;
    private LocalTime tenMinutesToClose;
    private boolean tradingSuspended;
    private int hitStopCount = 0;
    private double stopPrice;

    public TradingAlgo(String version,
                       PositionManager positionManager,
                       TradingDay tradingDay,
                       Logger algoLogger,
                       TradingStrategy tradingStrategy,
                       boolean logFeatureArray,
                       boolean logFeatureArrayOnHeartBeat) {
        super(version,
              positionManager,
              tradingDay,
              algoLogger,
              logFeatureArray,
              logFeatureArrayOnHeartBeat);

        this.orderReceiver = positionManager.getOrderReceiver();
        this.tradingStrategy = tradingStrategy;

        aboutToCloseTrigger = new Trigger(() -> {
            if (hasPosition()) {
                algoLogger.info(SPACER);
                exitPosition("Closing open position before close");
                tradingSuspended = true;
                algoLogger.info(SPACER);
            }
        });

        TradingSchedule tradingSchedule = Time.schedule();
        Phase closingPhase = tradingSchedule.firstMatchingPhase(Phase.IS_AFTER_MARKET_CLOSE);
        if (closingPhase != null) {
            tenMinutesToClose = closingPhase.getStart().getTime().minus(Duration.ofMinutes(10));
            Phase aboutToClosePhase = tradingSchedule.previousPhase(closingPhase);
            if (aboutToClosePhase != null) {
                registerPhaseEventCallback(aboutToClosePhase, this::fireAboutToCloseTrigger);
            }
        }
    }

    private boolean hasPosition() {
        return !noPosition();
    }

    private boolean noPosition() {
        return positionReporter.getCurrentPosition().noPosition();
    }

    public void exitPosition(String reason) {
        if (positionReporter.isExitingPosition()) {
            return;
        }
        lock.lock();
        try {
            algoLogger.info("Requested to exit position because {}. Current position: {}",
                            reason, getContractPosition());
            logFeaturesToAlgoLogger();
            orderReceiver.exitPosition(getAlgoName());
        } finally {
            lock.unlock();
        }
    }

    private double getContractPosition() {
        return positionReporter.getCurrentPosition().getContractPosition();
    }

    private void fireAboutToCloseTrigger() {
        if (Time.currentTime().isAfter(tenMinutesToClose)) {
            aboutToCloseTrigger.trigger();
        }
    }

    @Override
    protected void supplementalStartupInfo() {
        algoLogger.info("Quantity Multiplier: {}", tradingStrategy.getQuantityMultiplier());
    }

    @Override
    public void update(Observable o, Object arg) {
        if (positionReporter.getCurrentPosition()
                            .noPosition()) {  //TODO this only really needs to be set if a trade has been filled.
            setNoStopPrice();
        }

        super.update(o, arg);

        lock.lock();
        try {
            if (!positionReporter.isExitingPosition() && !positionReporter.hasPendingOrders() &&
                positionReporter.getCurrentPosition().hasPosition() && tradingDay.getPhase().isTradingSession()) {
                if (getUnrealisedPnL() >= cashoutUpperLevel && validBook()) {
                    cashout(format("Hit cash-out upper level of %s. Unrealised PnL: %s. Bid0: %s, Ask0: %s",
                                   cashoutUpperLevel, getUnrealisedPnL(), getBid0(), getAsk0()));
                } else if (getUnrealisedPnL() <= cashoutLowerLevel && validBook()) {
                    cashout(format("Hit cash-out lower level of %s. Unrealised PnL: %s. Bid0: %s, Ask0: %s",
                                   cashoutLowerLevel, getUnrealisedPnL(), getBid0(), getAsk0()));
                }

                if (positionReporter.getDailyHighestPnl() > 0 && getUnrealisedPnL() <=
                                                                 (positionReporter.getDailyHighestPnl() -
                                                                  maxGiveBack)) {
                    cashout("Exceeded max giveback");
                }
            }
        } finally {
            lock.unlock();
        }

        lock.lock();
        try {
            executeTradingStrategy();
        } finally {
            lock.unlock();
        }
    }

    private void setNoStopPrice() {
        setStopPrice(-1);
    }

    private void cashout(String reason) {
        if (noPosition() || positionReporter.isExitingPosition()) {
            return;
        }

        lock.lock();
        try {
            algoLogger.info("{}! will sit out rest of the day. PnL: {}", reason, getPnl());
            exitPosition(reason);
            tradingSuspended = true;
        } finally {
            lock.unlock();
        }
    }

    private double getPnl() {
        return positionReporter.getPnl();
    }

    private void executeTradingStrategy() {
        lock.lock();
        try {
            if (hasPosition() && tradingStrategy.isTimeToExit()) {
                aboutToCloseTrigger.trigger();
                return;
            }

            boolean signalWasChanged = this.signalChanged.reset();
            if (signalWasChanged && !tradingSuspended && shouldTrade()) {
                TradeDirective directive =
                        tradingStrategy.decideAction(signals, positionReporter.getCurrentPosition());
                directive.execute(this);
            }
        } finally {
            lock.unlock();
        }
    }

    protected boolean shouldTrade() {
        return tradingDay.getPhase().isTradingSession();
    }

    private double getUnrealisedPnL() {
        return positionReporter.getUnrealisedPnL();
    }

    @Override
    protected void processBookUpdate(EventType eventType) {
        if (tradingDay.getPhase().isTradingSession()) {
            checkStopPrice();
        }
    }

    private void checkStopPrice() {
        lock.lock();
        try {
            if (hasStop() && tradingDay.isReady() &&
                ((isLong() && getBid0() <= stopPrice && positionReporter.getPendingSellOrderCount() == 0) ||
                 (isShort() && getAsk0() >= stopPrice && positionReporter.getPendingBuyOrderCount() == 0))) {
                exitPosition(format("Hit stop price of %s. Bid0: %s, Ask0: %s, Position: %s",
                                    stopPrice,
                                    getBid0(),
                                    getAsk0(),
                                    getContractPosition()));
                hitStopCount++;
            }
        } finally {
            lock.unlock();
        }
    }

    protected boolean isLong() {
        return positionReporter.getCurrentPosition().isLong();
    }

    protected boolean isShort() {
        return positionReporter.getCurrentPosition().isShort();
    }

    @Override
    protected void onFirstEvent() {
        algoLogger.info("Contract Exchange: {}, Symbol : {}",
                        orderReceiver.getContractExchange(),
                        orderReceiver.getContractLocalSymbol());
        algoLogger.info("Stop Buffer: {}", stopBuffer);
    }

    public int getHitStopCount() {
        lock.lock();
        try {
            return hitStopCount;
        } finally {
            lock.unlock();
        }
    }

    public double getStopPrice() {
        lock.lock();
        try {
            return stopPrice;
        } finally {
            lock.unlock();
        }
    }

    protected void setStopPrice(double stopPrice) {
        lock.lock();
        try {
            this.stopPrice = stopPrice;

            if (hasStop()) {
                algoLogger.info("Stop price set to {}", stopPrice);
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean hasStop() {
        return stopPrice != -1;
    }

    public boolean isTradingSuspended() {
        lock.lock();
        try {
            return tradingSuspended;
        } finally {
            lock.unlock();
        }
    }

    public void forceResumeTrading(String source) {
        tradingSuspended = false;
        algoLogger.info("Forced to resume trading by {}", source);
    }

    public void stopTrading(String source) {
        tradingSuspended = true;
        algoLogger.info("Trading suspended by {}", source);
    }

    protected boolean longOrNoPosition() {
        return isLong() || noPosition();
    }

    protected boolean shortOrNoPosition() {
        return isShort() || noPosition();
    }

    protected void setEnterSignal(Signal newSignal) {
        requireNonNull(newSignal, "setEnterSignal newSignal");
        lock.lock();
        try {
            signals = signals.updateEnter(newSignal);

            if (signals.enterJustChanged()) {
                algoLogger.info("Enter signal changed from {} to {}. Bid0: {}, Ask0: {}, Position: {}",
                                signals.getPreviousEnter(),
                                newSignal,
                                getBid0(),
                                getAsk0(),
                                getContractPosition());
            }
            signalChanged.trigger();
        } finally {
            lock.unlock();
        }
    }

    public SignalContext getSignals() {
        lock.lock();
        try {
            return signals;
        } finally {
            lock.unlock();
        }
    }

    public int getStopBuffer() {
        lock.lock();
        try {
            return stopBuffer;
        } finally {
            lock.unlock();
        }
    }

    protected void setExitSignal(Signal newSignal) {
        requireNonNull(newSignal, "setExitSignal newSignal");
        lock.lock();
        try {
            signals = signals.updateExit(newSignal);

            if (signals.exitJustChanged()) {
                algoLogger.info("Exit signal changed from {} to {}. Bid0: {}, Ask0: {}, Position: {}",
                                signals.getPreviousExit(),
                                newSignal,
                                getBid0(),
                                getAsk0(),
                                positionReporter.getCurrentPosition().getContractPosition());
            }
            signalChanged.trigger();
        } finally {
            lock.unlock();
        }
    }

    protected void buyAtMarket(double quantity) {
        lock.lock();
        try {
            buyAtMarket(quantity, Math.max(tradingDay.getAsk0() - stopBuffer, 0));
        } finally {
            lock.unlock();
        }
    }

    protected void buyAtMarket(double quantity, double stopPrice) {
        lock.lock();
        try {
            if (canTrade()) {
                algoLogger.info("Requesting to buy {}@{} ...", quantity, getAsk0());

                logFeaturesToAlgoLogger();

                int orderId = orderReceiver.buy(getAlgoName(), triggeringEvent, quantity, stopPrice);
                if (orderId != -1) {
                    algoLogger.info("Position Manager accepted BUY order for {} contracts. ID: {}",
                                    quantity,
                                    orderId);
                    setStopPrice(stopPrice);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean canTrade() {
        return !tradingSuspended;
    }

    protected void sellAtMarket(double quantity) {
        lock.lock();
        try {
            sellAtMarket(quantity, tradingDay.getBid0() + stopBuffer);
        } finally {
            lock.unlock();
        }
    }

    protected void sellAtMarket(double quantity, double stopPrice) {
        lock.lock();
        try {
            if (canTrade()) {
                algoLogger.info("Requesting to sell {}@{} ...", quantity, getBid0());
                logFeaturesToAlgoLogger();
                int orderId = orderReceiver.sell(getAlgoName(), triggeringEvent, quantity, stopPrice);

                if (orderId != -1) {
                    setStopPrice(stopPrice);
                    algoLogger.info("Position Manager accepted SELL order for {} contracts. ID: {}",
                                    quantity,
                                    orderId);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public double getCashoutUpperLevel() {
        lock.lock();
        try {
            return cashoutUpperLevel;
        } finally {
            lock.unlock();
        }
    }

    public double getCashoutLowerLevel() {
        lock.lock();
        try {
            return cashoutLowerLevel;
        } finally {
            lock.unlock();
        }
    }

    public double getMaxGiveBack() {
        lock.lock();
        try {
            return maxGiveBack;
        } finally {
            lock.unlock();
        }
    }
}
