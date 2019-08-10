package com.ax9k.positionmanager;

import com.ax9k.utils.config.Configuration;

import java.time.Duration;

class RiskManager {
    private final int maxPendingOrders;
    private final int maxPendingBuyLimit;
    private final int maxPendingSellLimit;
    private final int minPosition;
    private final int maxPosition;
    private final int maxTradesFiveMinutes;
    private final int maxTotalDailyTrades;
    private final int maxLosingStreak;
    private final int minPnl;
    private final PositionReporter positionManager;
    private RejectionReason lastRejectionReason = RejectionReason.NOT_REJECTED;

    RiskManager(PositionReporter positionManager, Configuration config) {
        this.positionManager = positionManager;

        maxPendingOrders = config.getOptional("max_pending_orders", 1);
        minPosition = config.getOptional("min_position", -5);
        maxPosition = config.getOptional("max_position", 5);
        maxTradesFiveMinutes = config.getOptional("max_trades_5_minutes", 100);
        maxTotalDailyTrades = config.getOptional("max_total_daily_trades", 1000);
        minPnl = config.getOptional("min_pnl", -500000);
        maxLosingStreak = config.getOptional("max_losing_streak", 9999);

        maxPendingBuyLimit = maxPendingSellLimit = maxPendingOrders;
    }

    int getMinPnL() {
        return minPnl;
    }

    boolean canBuy() {
        return belowMaxPosition() && belowMaxPendingBuyLimit() && belowMaxLosingStreak() &&
               (positionManager.getCurrentPosition().isShort() || underDailyTradeLimit()) &&
               (positionManager.getCurrentPosition().isShort() || under5minTradeLimit()) &&
               (positionManager.getCurrentPosition().isShort() || aboveMinPnl()) &&
               (positionManager.getCurrentPosition().isShort() || belowMaxPendingOrderLimit());
    }

    private boolean belowMaxLosingStreak() {
        if (positionManager.getLosingStreak() > maxLosingStreak) {
            lastRejectionReason = RejectionReason.ABOVE_MAX_LOSING_STREAK;
            return false;
        }
        return true;
    }

    private boolean belowMaxPosition() {
        if (positionManager.getCurrentPosition().getContractPosition() >= maxPosition) {
            lastRejectionReason = RejectionReason.ABOVE_MAX_POSITION;
            return false;
        }
        return true;
    }

    private boolean belowMaxPendingBuyLimit() {
        if (positionManager.getPendingBuyOrderCount() >= maxPendingBuyLimit) {
            lastRejectionReason = RejectionReason.ABOVE_PENDING_BUY_LIMIT;
            return false;
        }
        return true;
    }

    private boolean underDailyTradeLimit() {
        if (positionManager.getTradeCount() >= maxTotalDailyTrades) {
            lastRejectionReason = RejectionReason.OVER_DAILY_TRADE_LIMIT;
            return false;
        }
        return true;
    }

    private boolean under5minTradeLimit() {

        if (positionManager.getTradeCount(Duration.ofMinutes(5)) > maxTradesFiveMinutes) {
            lastRejectionReason = RejectionReason.OVER_5_MIN_TRADE_LIMIT;
            return false;
        }
        return true;
    }

    private boolean aboveMinPnl() {
        if (positionManager.getPnl() < minPnl) {
            lastRejectionReason = RejectionReason.UNDER_MIN_PNL;
            return false;
        }
        return true;
    }

    private boolean belowMaxPendingOrderLimit() {
        if (positionManager.getPendingBuyOrderCount() >= maxPendingOrders ||
            positionManager.getPendingSellOrderCount() >= maxPendingOrders) {
            lastRejectionReason = RejectionReason.ABOVE_PENDING_LIMIT;
            return false;
        }
        return true;
    }

    boolean canSell() {
        return aboveMinPosition() && belowMaxPendingSellLimit() && belowMaxLosingStreak() &&
               (positionManager.getCurrentPosition().isLong() || underDailyTradeLimit()) &&
               (positionManager.getCurrentPosition().isLong() || under5minTradeLimit()) &&
               (positionManager.getCurrentPosition().isLong() || aboveMinPnl()) &&
               (positionManager.getCurrentPosition().isLong() || belowMaxPendingOrderLimit());
    }

    private boolean aboveMinPosition() {
        if (positionManager.getCurrentPosition().getContractPosition() <= minPosition) {
            lastRejectionReason = RejectionReason.BELOW_MIN_POSITION;
            return false;
        }
        return true;
    }

    private boolean belowMaxPendingSellLimit() {
        if (positionManager.getPendingSellOrderCount() >= maxPendingSellLimit) {
            lastRejectionReason = RejectionReason.ABOVE_PENDING_SELL_LIMIT;
            return false;
        }
        return true;
    }

    String getLastRejectionReason() {
        return lastRejectionReason.toString();
    }

    public enum RejectionReason {
        NOT_REJECTED,
        ABOVE_PENDING_BUY_LIMIT,
        ABOVE_PENDING_SELL_LIMIT,
        ABOVE_PENDING_LIMIT,
        UNDER_MIN_PNL,
        OVER_5_MIN_TRADE_LIMIT,
        OVER_DAILY_TRADE_LIMIT,
        BELOW_MIN_POSITION,
        ABOVE_MAX_POSITION,
        ABOVE_MAX_LOSING_STREAK
    }
}
