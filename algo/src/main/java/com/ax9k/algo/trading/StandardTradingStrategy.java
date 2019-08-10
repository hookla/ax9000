package com.ax9k.algo.trading;

import com.ax9k.core.marketmodel.Milestone;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.core.time.Time;
import com.ax9k.positionmanager.Position;

import java.time.Duration;
import java.time.LocalTime;

import static com.ax9k.algo.Algo.Signal.BUY;
import static com.ax9k.algo.Algo.Signal.SELL;
import static com.ax9k.algo.trading.Directives.buy;
import static com.ax9k.algo.trading.Directives.exit;
import static com.ax9k.algo.trading.Directives.none;
import static com.ax9k.algo.trading.Directives.sell;
import static com.ax9k.utils.compare.ComparableUtils.greaterThanOrEqual;

public final class StandardTradingStrategy implements TradingStrategy {
    private static final Duration EXIT_TIME_BUFFER = Duration.ofMinutes(5);
    //TODO this should come from the ALGO config and default to 5 mins

    private final LocalTime exitTime;
    private final double quantityMultiplier;

    private StandardTradingStrategy(LocalTime exitTime, double quantityMultiplier) {
        this.exitTime = exitTime;
        this.quantityMultiplier = quantityMultiplier;
    }

    public static StandardTradingStrategy tradeNumberOfSessions(int sessionsToTrade, double quantityMultiplier) {
        TradingSchedule tradingSchedule = Time.schedule();

        Phase nthTradingSession = tradingSchedule.phaseForIndex(sessionsToTrade - 1, Phase.IS_TRADING_SESSION);

        Milestone end = nthTradingSession != null ? nthTradingSession.getEnd() :
                        tradingSchedule.lastTradingSession().getEnd();

        return tradeUntil(end, quantityMultiplier);
    }

    public static StandardTradingStrategy tradeUntil(Milestone milestone, double quantityMultiplier) {
        return exitAt(milestone.getTime().minus(EXIT_TIME_BUFFER), quantityMultiplier);
    }

    public static StandardTradingStrategy exitAt(LocalTime time, double quantityMultiplier) {
        return new StandardTradingStrategy(time, quantityMultiplier);
    }

    public static StandardTradingStrategy noExit(double quantityMultiplier) {
        return new StandardTradingStrategy(null, quantityMultiplier);
    }

    @Override
    public TradeDirective decideAction(SignalContext signals, Position position) {
        if (isTimeToExit()) {
            if (position.hasPosition()) {
                return exit("Hit exit time");
            }
            return none();
        }

        if (position.hasPosition()) {
            if (signals.enterJustChanged()) {
                if (position.isLong() && signals.getPreviousEnter() == BUY && signals.getEnter() == SELL) {
                    return sell(2 * quantityMultiplier);
                } else if (position.isShort() && signals.getPreviousEnter() == SELL && signals.getEnter() == BUY) {
                    return buy(2 * quantityMultiplier);
                }
            } else if (signals.exitJustChanged() && (signals.getExit() != SELL && position.isShort()) ||
                       (signals.getExit() != BUY && position.isLong())) {
                return exit("Exit signal changed to: " + signals.getExit());
            }
        } else if (signals.enterJustChanged()) {
            if (signals.getEnter() == BUY) {
                return buy(quantityMultiplier);
            } else if (signals.getEnter() == SELL) {
                return sell(quantityMultiplier);
            }
        }
        return none();
    }

    @Override
    public boolean isTimeToExit() {
        return exitTime != null && greaterThanOrEqual(Time.currentTime(), exitTime);
    }

    public double getQuantityMultiplier() {
        return quantityMultiplier;
    }
}
