package com.ax9k.positionmanager;

import com.ax9k.core.marketmodel.Phase;

import java.time.Duration;
import java.util.Map;

public interface PositionReporter {
    double getWinningLoosingRatio();

    double getBrokerRealisedPnl();

    double getUnrealisedPnL();

    int getPendingBuyOrderCount();

    int getPendingSellOrderCount();

    int getLosingTrades();

    int getWinningTrades();

    int getLosingStreak();

    int getLongestLosingStreak();

    int getTradeCount();

    boolean hasPendingOrders();

    boolean validBook();

    long getTradeCount(Duration duration);

    double getPnl();

    double getGrossPnl();

    double getNetPnl();

    double getDailyHighestPnl();

    double getDailyLowestPnl();

    double getPnlAtEndOfSession(Phase tradingSession);

    Map<Phase, Double> getPnlsAtEndOfSessions();

    double getPnlGainForSession(Phase tradingSession);

    String getLastRiskManagerRejectReason();

    Position getCurrentPosition();

    boolean isExitingPosition();

    Order getLastTrade();

    int getContractMultiplier();

    double getBid0();

    double getAsk0();

    double getCostPerTrade();

    double getTradingFees();
}
