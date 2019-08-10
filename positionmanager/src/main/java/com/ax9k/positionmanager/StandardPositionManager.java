package com.ax9k.positionmanager;

import com.ax9k.broker.BrokerCallbackReceiver;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.time.Time;
import com.ax9k.utils.compare.ComparableUtils;
import com.ax9k.utils.config.Configuration;
import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@JsonRootName("positionManager")
public class StandardPositionManager implements PositionManager, MarketDataProviderCallbackReceiver {
    private static final Duration TWO_MINUTES = Duration.ofMinutes(2);

    private final Lock lock = new ReentrantLock();
    private final RiskManager riskManager;
    private final Logger logger;
    private final BrokerCallbackReceiver brokerCallbackReceiver;
    private final PositionManagerStateImpl state;
    private final OrderReceiver orderReceiver;
    private final boolean exitBetweenTradingSessions;

    StandardPositionManager(Configuration riskManagerConfiguration, Logger logger, boolean exitBetweenTradingSessions) {
        this.logger = logger;
        this.exitBetweenTradingSessions = exitBetweenTradingSessions;
        state = new PositionManagerStateImpl();

        riskManager = new RiskManager(state, riskManagerConfiguration);
        orderReceiver = new OrderReceiverImpl(state, riskManager, lock, logger);
        brokerCallbackReceiver = new BrokerCallbackReceiverImpl(state, lock, logger);
        state.setOrderReceiver(orderReceiver);
    }

    public BrokerCallbackReceiver getBrokerCallbackReceiver() {
        return brokerCallbackReceiver;
    }

    public OrderReceiver getOrderReceiver() {
        return orderReceiver;
    }

    public MarketDataProviderCallbackReceiver getMarketDataProviderCallbackReceiver() {
        return this;
    }

    public void updateBookValues(double bid0, double ask0) {
        lock.lock();
        try {

            Phase currentPhase = Time.currentPhase();
            state.setTopOfBook(bid0, ask0);
            if (!state.isExitingPosition() &&
                !state.hasPendingOrders() &&
                state.validBook() &&
                state.getCurrentPosition().hasPosition() &&
                state.getUnrealisedPnL() < riskManager.getMinPnL()) {

                logger.info("Exiting position because unrealised PnL equals {}, which is less than the acceptable " +
                            "minimum of {}. Position value at entry: {}",
                            state.getUnrealisedPnL(),
                            riskManager.getMinPnL(),
                            getPositionReporter().getCurrentPosition().getValueAtEntry());
                orderReceiver.exitPosition("POSITION_MANAGER");
            } else if (tradingSessionEnding(currentPhase)) {
                if (exitBetweenTradingSessions && state.getCurrentPosition().hasPosition() &&
                    !state.isExitingPosition()) {
                    logger.info("Exiting position of {} because phase {} is about to end.",
                                state.getCurrentPosition().getContractPosition(),
                                currentPhase);
                    orderReceiver.exitPosition("POSITION_MANAGER");
                }

                if (!state.getPnlsAtEndOfSessions().containsKey(currentPhase)) {
                    state.recordEndOfSessionPnL(currentPhase);
                    logger.info("PnL at end of phase {}: {}", currentPhase, state.getPnl());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public PositionReporter getPositionReporter() {
        return state;
    }

    private boolean tradingSessionEnding(Phase currentPhase) {
        if (!currentPhase.isTradingSession()) {
            return false;
        }

        LocalTime end = currentPhase.getEnd().getTime();
        LocalTime twoBeforeEnd = end.minus(TWO_MINUTES);
        return timeIsBetween(twoBeforeEnd, end);
    }

    private static boolean timeIsBetween(LocalTime start, LocalTime end) {
        LocalTime currentTime = Time.currentTime();
        return ComparableUtils.greaterThanOrEqual(currentTime, start) &&
               ComparableUtils.lessThanOrEqual(currentTime, end);
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            return JsonUtils.toPrettyJsonString(this);
        } finally {
            lock.unlock();
        }
    }
}




