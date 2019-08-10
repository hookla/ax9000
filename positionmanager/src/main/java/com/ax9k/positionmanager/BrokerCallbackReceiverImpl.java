package com.ax9k.positionmanager;

import com.ax9k.broker.BrokerCallbackReceiver;
import com.ax9k.core.marketmodel.BidAsk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.Lock;

import static com.ax9k.positionmanager.Position.ALREADY_FILLED;
import static com.ax9k.positionmanager.Position.FILL_ERROR;

class BrokerCallbackReceiverImpl implements BrokerCallbackReceiver {
    private static final Logger ERROR_LOG = LogManager.getLogger("error");

    private final PositionManagerStateUpdater state;
    private final Lock lock;
    private final Logger logger;

    BrokerCallbackReceiverImpl(PositionManagerStateUpdater state, Lock lock, Logger logger) {
        this.state = state;
        this.lock = lock;
        this.logger = logger;
    }

    @Override
    public void orderFilled(Instant fillTimestamp, int orderId, double avgFillPrice, double quantity) {
        if (avgFillPrice <= 0 || quantity <= 0) {
            throw new IllegalStateException(
                    String.format("cant fill with that price/quantity %s/%s", avgFillPrice, quantity));
        }

        lock.lock();
        try {
            List<Order> filledOrders = state.getCurrentPosition().fillOrder(fillTimestamp,
                                                                            orderId,
                                                                            avgFillPrice,
                                                                            quantity);

            if (state.isExitingPosition() && state.getCurrentPosition().noPosition()) {
                logger.info("Order filled. We no longer have a position");
                state.setExitingPosition(false);
            }

            if (filledOrders == ALREADY_FILLED) {
                ERROR_LOG.error("already filled order: {}", orderId);
                return;
            } else if (filledOrders == FILL_ERROR) {
                state.setLastTrade(Order.ERROR);
                return;
            }

            Order lastOrder = filledOrders.get(filledOrders.size() - 1);
            if (lastOrder.getSide() == BidAsk.ASK) {
                state.decreasePendingSellOrders();
            } else if (lastOrder.getSide() == BidAsk.BID) {
                state.decreasePendingBuyOrders();
            } else {
                ERROR_LOG.error("Unsupported side: {}", lastOrder);
            }

            state.setLastTrade(lastOrder);

            filledOrders.forEach(this::updateDailyStats);

            if (lastOrder.getSide() == BidAsk.ASK) {
                logger.info("Received a SELL callback: {}@{}. Order ID: {}, PnL: {}, Expected PnL: {}",
                            quantity, avgFillPrice, orderId, state.getPnl(), state.getUnrealisedPnL()
                );
            } else if (lastOrder.getSide() == BidAsk.BID) {
                logger.info("Received a Buy callback: {}@{}. Order ID: {}, Pnl {}",
                            quantity, avgFillPrice, orderId, state.getPnl()
                );
            } else {
                throw new IllegalStateException("should only be doing bid or ask here");
            }

            if (!state.getCurrentPosition().hasPosition()) {
                logger.info("Exited Position. new PnL inc fees: {}", state.getNetPnl());
            }
        } finally {
            lock.unlock();
        }
    }

    private void updateDailyStats(Order filledOrder) {
        state.increaseTradeCount();
        if (filledOrder.getPositionAction() == Order.PositionAction.EXIT) {
            state.setPnl(state.getPnl() + filledOrder.getOrderPnl());
            if (state.getPnl() < state.getDailyLowestPnl()) {
                state.setDailyLowestPnl(state.getPnl());
            }
            if (state.getPnl() > state.getDailyHighestPnl()) {
                state.setDailyHighestPnl(state.getPnl());
            }

            if (filledOrder.getOrderPnl() >= 0) {
                state.increaseWinningTrades();
            } else {
                state.increaseLosingTrades();
            }
        }
    }

    @Override
    public void orderCancelled(int orderId) {
        lock.lock();
        try {
            Order canceledOrder = state.getCurrentPosition().cancelOrder(orderId);
            if (canceledOrder != null) {
                if (canceledOrder.getSide() == BidAsk.ASK) {
                    state.decreasePendingSellOrders();
                } else if (canceledOrder.getSide() == BidAsk.BID) {
                    state.decreasePendingBuyOrders();
                } else {
                    ERROR_LOG.error("Unsupported side: {}", canceledOrder);
                }
            } else {
                ERROR_LOG.error("Did not cancel order : {}", orderId);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void pnlUpdate(double unrealised, double realised, double daily) {
        lock.lock();
        try {
            state.setBrokerUnrealisedPnl(unrealised);
            state.setBrokerRealisedPnl(realised);
            state.setBrokerDailyPnl(daily);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void positionUpdate(double position, double averageCost) {
        lock.lock();
        try {
            state.getCurrentPosition().setContractPosition(position);

            double entryPrice = averageCost / state.getContractMultiplier();
            if (!state.getCurrentPosition().getPositionInitialised()) {
                logger.info("Initialising position: {}@{}", position, entryPrice);
                Order initialOrder = state.getCurrentPosition().initialisePosition(position,
                                                                                   entryPrice,
                                                                                   state.getContractMultiplier());
                state.setLastTrade(initialOrder);
            }
            logger.info("New position: {}, entry price: {}", position, entryPrice);
        } finally {
            lock.unlock();
        }
    }
}
