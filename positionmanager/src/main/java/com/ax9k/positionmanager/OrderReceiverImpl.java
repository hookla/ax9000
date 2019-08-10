package com.ax9k.positionmanager;

import com.ax9k.broker.Broker;
import com.ax9k.broker.OrderRecord;
import com.ax9k.broker.OrderRequest;
import com.ax9k.core.event.Event;
import com.ax9k.core.marketmodel.BidAsk;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.locks.Lock;

public class OrderReceiverImpl implements OrderReceiver {
    private static final Logger ERROR_LOG = LogManager.getLogger("error");

    private final Logger logger;
    private final PositionManagerStateUpdater state;
    private final RiskManager riskManager;
    private final Lock lock;
    private Broker broker;

    OrderReceiverImpl(PositionManagerStateUpdater state,
                      RiskManager riskManager,
                      Lock lock,
                      Logger logger) {
        this.riskManager = riskManager;
        this.state = state;
        this.lock = lock;
        this.logger = logger;
    }

    @Override
    public void initialiseBroker(Broker broker) {
        Validate.notNull(broker);
        lock.lock();
        try {
            Validate.validState(this.broker == null, "Broker can only be initialised once");
            this.broker = broker;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void exitPosition(String source) {
        lock.lock();
        try {
            checkReady();
            if (isExitingPosition()) {
                return;
            }
            Position currentPosition = state.getCurrentPosition();

            logger.info("Instructed to exit position of {}. Source: {}", currentPosition.getContractPosition(), source);

            if (currentPosition.noPosition()) {
                state.setExitingPosition(false);
                logger.warn("No position to exit");
                return;
            }
            state.setExitingPosition(true);

            cancelAllPendingOrders(source);
            if (currentPosition.isLong()) {
                doSell(source, null, currentPosition.getContractPosition(), -1);
            } else if (currentPosition.isShort()) {
                doBuy(source, null, -currentPosition.getContractPosition(), -1);
            }
        } finally {
            lock.unlock();
        }
    }

    private void checkReady() {
        Validate.validState(broker != null, "Broker not initialised");
    }

    @Override
    public void cancelAllPendingOrders(String source) {
        lock.lock();
        try {
            checkReady();
            logger.info("Instructed to cancel all pending orders by {}", source);
            broker.cancelAllPendingOrders();
            state.resetPendingSellOrders();
            state.resetPendingBuyOrders();
        } finally {
            lock.unlock();
        }
    }

    private int doSell(String source, Event triggeringEvent, double quantity, double stopPrice) {
        double price = state.getBid0();
        OrderRequest request = OrderRequest.of(price, quantity, BidAsk.ASK);
        OrderRecord record = broker.place(request);
        state.getCurrentPosition().addSellOrder(record,
                                                source,
                                                triggeringEvent, quantity,
                                                price,
                                                stopPrice,
                                                state.getContractMultiplier());
        state.increasePendingSellOrders();
        logger.info("Sent SELL Market Order. {}@{}(current price). Order ID: {}",
                    quantity, state.getBid0(), record.getId());
        return record.getId();
    }

    private int doBuy(String source, Event triggeringEvent, double quantity, double stopPrice) {
        double price = state.getAsk0();
        OrderRequest request = OrderRequest.of(price, quantity, BidAsk.BID);
        OrderRecord record = broker.place(request);
        state.getCurrentPosition().addBuyOrder(record,
                                               source,
                                               triggeringEvent, quantity,
                                               state.getAsk0(),
                                               stopPrice,
                                               state.getContractMultiplier());
        state.increasePendingBuyOrders();

        logger.info("Sent BUY Market Order. {}@{}(current price). Order ID: {}",
                    quantity, state.getAsk0(), record.getId());
        return record.getId();
    }

    public boolean isExitingPosition() {
        lock.lock();
        try {
            return state.isExitingPosition();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int sell(String source, Event triggeringEvent, double quantity, double stopPrice) {
        lock.lock();
        try {
            checkReady();
            if (state.getBid0() <= 0d) {
                ERROR_LOG.error("Cannot sell with invalid bid0 value of: {}", state.getBid0());
                return -1;
            } else if (!riskManager.canSell()) {
                ERROR_LOG.error("Risk manager refused SELL order for {}. Reason: {}", quantity,
                                riskManager.getLastRejectionReason());
                return -1;
            }

            return doSell(source, triggeringEvent, quantity, stopPrice);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int buy(String source, Event triggeringEvent, double quantity, double stopPrice) {
        lock.lock();
        try {
            checkReady();
            if (state.getAsk0() <= 0d) {
                ERROR_LOG.error("Cannot buy with invalid ask0 value of: {}", state.getAsk0());
                return -1;
            } else if (!riskManager.canBuy()) {
                ERROR_LOG.error("Risk manager refused BUY order for {}. Reason: {}", quantity,
                                riskManager.getLastRejectionReason());
                return -1;
            }

            return doBuy(source, triggeringEvent, quantity, stopPrice);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getLastRiskManagerRejectReason() {
        lock.lock();
        try {
            return riskManager.getLastRejectionReason();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public double getCostPerTrade() {
        lock.lock();
        try {
            return broker.getContract().getCostPerTrade();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getContractMultiplier() {
        lock.lock();
        try {
            return broker.getContract().getMultiplier();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getContractLocalSymbol() {
        lock.lock();
        try {
            return broker.getContract().getLocalSymbol();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getContractExchange() {
        lock.lock();
        try {
            return broker.getContract().getExchange();
        } finally {
            lock.unlock();
        }
    }
}
