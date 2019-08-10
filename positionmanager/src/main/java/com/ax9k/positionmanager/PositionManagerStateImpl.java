package com.ax9k.positionmanager;

import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.core.time.Time;
import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.Validate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class PositionManagerStateImpl implements PositionReporter, PositionManagerStateUpdater {

    private final Position currentPosition = new Position();
    private final HashMap<Phase, Double> sessionPnls = new HashMap<>();

    private OrderReceiver orderReceiver;
    private Order lastTrade = Order.EMPTY;

    private int pendingBuyOrderCount;
    private int pendingSellOrderCount;
    private boolean isExitingPosition;

    private double brokerRealisedPnl;
    private double brokerUnrealisedPnl;
    private double brokerDailyPnl;
    private int winningTrades = 0;
    private int losingTrades = 0;
    private int losingStreak = 0;
    private int longestLosingStreak = 0;
    private int tradeCount;
    private double pnl;
    private double dailyLowestPnl = 0;
    private double dailyHighestPnl = 0;

    private double bid0;
    private double ask0;

    public String getContractLocalSymbol() {
        return orderReceiver.getContractLocalSymbol();
    }

    public String getContractExchange() {
        return orderReceiver.getContractExchange();
    }

    @Override
    public void setOrderReceiver(OrderReceiver orderReceiver) {
        this.orderReceiver = orderReceiver;
    }

    @Override
    public double getWinningLoosingRatio() {
        return (getLosingTrades() == 0) ? getWinningTrades() : getWinningTrades() / (double) getLosingTrades();
    }

    @Override
    public int getWinningTrades() {
        return winningTrades;
    }

    @Override
    public int getLosingTrades() {
        return losingTrades;
    }

    @Override
    public double getNetPnl() {
        return getPnl() - getTradingFees();
    }

    @Override
    public double getPnl() {
        return getGrossPnl();
    }

    @Override
    public double getGrossPnl() {
        return pnl;
    }

    @Override
    public void setPnl(double pnl) {
        this.pnl = pnl;
    }

    @Override
    public double getTradingFees() {
        return getCostPerTrade() * tradeCount;
    }

    @Override
    public double getCostPerTrade() {
        return orderReceiver.getCostPerTrade();
    }

    @Override
    public void recordEndOfSessionPnL(Phase tradingSession) {
        sessionPnls.put(notNull(tradingSession), getGrossPnl());
    }

    @Override
    public double getPnlAtEndOfSession(Phase tradingSession) {
        return sessionPnls.getOrDefault(tradingSession, 0d);
    }

    @Override
    public double getPnlGainForSession(Phase tradingSession) {
        if (!sessionPnls.containsKey(tradingSession)) {
            return 0d;
        }

        Phase previousSession = getPreviousTradingSession(tradingSession);
        if (previousSession == null) {
            return sessionPnls.get(tradingSession);
        }

        return sessionPnls.get(tradingSession) - sessionPnls.get(previousSession);
    }

    private Phase getPreviousTradingSession(Phase tradingSession) {
        TradingSchedule tradingSchedule = Time.schedule();

        int tradingSessionIndex = tradingSchedule.getPhaseIndex(tradingSession, Phase.IS_TRADING_SESSION);

        Validate.isTrue(tradingSessionIndex > 0, "'%s' is not a valid trading session", tradingSession.getName());

        return tradingSessionIndex > 1 ? tradingSchedule.phaseForIndex(tradingSessionIndex - 1,
                                                                       Phase.IS_TRADING_SESSION) : null;
    }

    @Override
    public Map<Phase, Double> getPnlsAtEndOfSessions() {
        return sessionPnls;
    }

    @Override
    public double getDailyLowestPnl() {
        return dailyLowestPnl;
    }

    @Override
    public void setDailyLowestPnl(double dailyLowestPnl) {
        this.dailyLowestPnl = dailyLowestPnl;
    }

    @Override
    public double getDailyHighestPnl() {
        return dailyHighestPnl;
    }

    @Override
    public void setDailyHighestPnl(double dailyHighestPnl) {
        this.dailyHighestPnl = dailyHighestPnl;
    }

    @Override
    public int getTradeCount() {
        return tradeCount;
    }

    @Override
    public double getBrokerRealisedPnl() {
        return brokerRealisedPnl;
    }

    @Override
    public void setBrokerRealisedPnl(double brokerRealisedPnl) {
        this.brokerRealisedPnl = brokerRealisedPnl;
    }

    public double getBrokerUnrealisedPnl() {
        return brokerUnrealisedPnl;
    }

    @Override
    public void setBrokerUnrealisedPnl(double brokerUnrealisedPnl) {
        this.brokerUnrealisedPnl = brokerUnrealisedPnl;
    }

    public double getBrokerDailyPnl() {
        return brokerDailyPnl;
    }

    @Override
    public void setBrokerDailyPnl(double brokerDailyPnl) {
        this.brokerDailyPnl = brokerDailyPnl;
    }

    @Override
    public boolean hasPendingOrders() {
        return currentPosition.hasPendingOrders();
    }

    @Override
    public boolean validBook() {
        return getBid0() > 0 && getAsk0() > 0;
    }

    @Override
    public double getBid0() {
        return bid0;
    }

    @Override
    public double getAsk0() {
        return ask0;
    }

    @JsonIgnore
    @Override
    public Order getLastTrade() {
        return lastTrade;
    }

    @Override
    public void setLastTrade(Order lastTrade) {
        this.lastTrade = lastTrade;
    }

    @Override
    public void increasePendingSellOrders() {
        pendingSellOrderCount++;
    }

    @Override
    public void decreasePendingSellOrders() {
        pendingSellOrderCount--;
    }

    @Override
    public void increasePendingBuyOrders() {
        pendingBuyOrderCount++;
    }

    @Override
    public void decreasePendingBuyOrders() {
        pendingBuyOrderCount--;
    }

    @Override
    public void resetPendingSellOrders() {
        pendingBuyOrderCount = 0;
    }

    @Override
    public void resetPendingBuyOrders() {
        pendingBuyOrderCount = 0;
    }

    @Override
    public int getPendingBuyOrderCount() {
        return pendingBuyOrderCount;
    }

    @Override
    public int getPendingSellOrderCount() {
        return pendingSellOrderCount;
    }

    @Override
    public long getTradeCount(Duration period) {
        return currentPosition.getTradeCount(period);
    }

    @Override
    public boolean isExitingPosition() {
        return isExitingPosition;
    }

    @Override
    public void setExitingPosition(boolean exitingPosition) {
        isExitingPosition = exitingPosition;
    }

    @Override
    public void setTopOfBook(double bid0, double ask0) {
        this.bid0 = bid0;
        this.ask0 = ask0;
    }

    @Override
    public String getLastRiskManagerRejectReason() {
        return orderReceiver.getLastRiskManagerRejectReason();
    }

    public double getContractPosition() {
        return getCurrentPosition().getContractPosition();
    }

    @Override
    public Position getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public double getUnrealisedPnL() {
        return getPnl() + currentPosition.getUnrealisedPnL(getBid0(), getAsk0(), getContractMultiplier());
    }

    @Override
    public int getContractMultiplier() {
        return orderReceiver.getContractMultiplier();
    }

    @Override
    public void increaseTradeCount() {
        tradeCount++;
    }

    @Override
    public void increaseLosingTrades() {
        losingTrades++;
        increaseLosingStreak();
    }

    private void increaseLosingStreak() {
        losingStreak++;
        if (losingStreak > longestLosingStreak) {
            longestLosingStreak = losingStreak;
        }
    }

    @Override
    public int getLongestLosingStreak() {
        return longestLosingStreak;
    }

    @Override
    public void increaseWinningTrades() {
        winningTrades++;
        losingStreak = 0;
    }

    @Override
    public int getLosingStreak() {
        return losingStreak;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }
}
