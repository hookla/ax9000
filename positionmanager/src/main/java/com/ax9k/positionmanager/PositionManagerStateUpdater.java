package com.ax9k.positionmanager;

import com.ax9k.core.marketmodel.Phase;

public interface PositionManagerStateUpdater extends PositionReporter {

    void setExitingPosition(boolean value);

    void setLastTrade(Order order);

    void setBrokerRealisedPnl(double brokerRealisedPnl);

    void setBrokerUnrealisedPnl(double brokerUnrealisedPnl);

    void setBrokerDailyPnl(double brokerDailyPnl);

    void setDailyLowestPnl(double dailyLowestPnl);

    void setDailyHighestPnl(double dailyHighestPnl);

    void setPnl(double pnl);

    void recordEndOfSessionPnL(Phase tradingSession);

    void setTopOfBook(double bid0, double ask0);

    void increaseTradeCount();

    void increaseLosingTrades();

    void increaseWinningTrades();

    void increasePendingSellOrders();

    void decreasePendingSellOrders();

    void increasePendingBuyOrders();

    void decreasePendingBuyOrders();

    void resetPendingSellOrders();

    void resetPendingBuyOrders();

    void setOrderReceiver(OrderReceiver orderReceiver);
}
