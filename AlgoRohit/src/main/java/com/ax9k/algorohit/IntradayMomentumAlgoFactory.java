package com.ax9k.algorohit;

import com.ax9k.algo.Algo;
import com.ax9k.algo.AlgoFactory;
import com.ax9k.algo.features.FeatureManager;
import com.ax9k.algo.trading.PeriodicSignalChangeStrategy;
import com.ax9k.algo.trading.TradingStrategy;
import com.ax9k.cex.data.Ticker;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.core.marketmodel.orderbook.OrderBook;
import com.ax9k.core.time.Time;
import com.ax9k.positionmanager.PositionManager;
import com.ax9k.utils.config.Configuration;
import com.ax9k.utils.json.JsonUtils;
import com.ax9k.utils.logging.AsynchronousAppender;
import com.ax9k.utils.logging.FunctionLayout;
import com.ax9k.utils.logging.SlackAppender;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static com.ax9k.algo.trading.StandardTradingStrategy.noExit;
import static com.ax9k.algo.trading.StandardTradingStrategy.tradeNumberOfSessions;
import static java.lang.String.format;

public class IntradayMomentumAlgoFactory implements AlgoFactory {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ALGO_SLACK_HOOK =
            "https://hooks.slack.com/services/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";
    private static final Layout<String> TIME_AND_MESSAGE_LAYOUT = new FunctionLayout(
            event -> format("[A] %s: %s", Time.currentTime(), event.getMessage().getFormattedMessage()));
    private static final Duration UPDATE_PERIOD = Duration.ofMinutes(1);

    @Override
    public Algo create(PositionManager positionManager, TradingDay tradingDay, Configuration configuration) {
        int stopBuffer = configuration.getOptional("stopBuffer", 10000);
        double cashoutUpperLevel = configuration.getOptional("cashoutUpperLevel", 1000000);
        double cashoutLowerLevel = configuration.getOptional("cashoutLowerLevel", -1000000);
        double maxGiveBack = configuration.getOptional("maxGiveBack", Integer.MAX_VALUE);
        double emaCrossTolerance = configuration.getOptional("emaCrossTolerance", 0.0001);
        double quantityMultiplier = configuration.getOptional("quantityMultiplier", 1d);
        int sessionsToTrade = configuration.getOptional("sessionsToTrade", 2);

        LOGGER.info("Constructing algo implementation ...");

        Logger algoLogger = LogManager.getLogger("algoLogger");
        boolean logToSlack = configuration.getOptional("logToSlack", false);
        if (logToSlack) {
            addSlackAppender(algoLogger);
        }

        TradingStrategy tradingStrategy;
        TradingSchedule schedule = Time.schedule();
        Phase closingPhase = schedule.firstMatchingPhase(Phase.IS_AFTER_MARKET_CLOSE);
        if (closingPhase == null) {
            LOGGER.info("Configured 'no exit' trading strategy.");
            tradingStrategy = noExit(quantityMultiplier);
        } else {
            LOGGER.info("Will trade {} sessions.", sessionsToTrade);
            tradingStrategy = tradeNumberOfSessions(sessionsToTrade, quantityMultiplier);
        }

        LocalTime entryTime;
        LocalTime currentTime = Time.currentTime();
        if (schedule.phaseForTime(currentTime).isMarketOpen()) {
            entryTime = currentTime.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        } else {
            Phase firstOpenPhase = schedule.marketOpen();
            Validate.isTrue(firstOpenPhase != null, "Trading schedule does not have an open phase");
            entryTime = firstOpenPhase.getStart().getTime();
        }
        LOGGER.info("Will start trading at {}.", entryTime);

        PeriodicSignalChangeStrategy signalChangeStrategy;
        if (!configuration.getOptional("contractHasVolumes", true)) {
            FeatureManager<OrderBook> bookFeatures = new FeatureManager<>(tradingDay::getOrderBookHistory);
            signalChangeStrategy = new NoBarIntradayMomentumStrategy(algoLogger,
                                                                     bookFeatures,
                                                                     tradingDay.getHistory(Ticker.class),
                                                                     UPDATE_PERIOD,
                                                                     entryTime,
                                                                     emaCrossTolerance);
        } else {
            String quantityFilesLocation = configuration.getOptional("quantityFilesLocation").orElse(null);

            FeatureManager<OhlcvBar> ohlcvFeatures = new FeatureManager<>(tradingDay::getBarHistory);
            signalChangeStrategy = new AverageQuantityIntradayMomentumSignalChangeStrategy(algoLogger,
                                                                                           ohlcvFeatures,
                                                                                           UPDATE_PERIOD,
                                                                                           entryTime,
                                                                                           quantityFilesLocation,
                                                                                           emaCrossTolerance);
        }

        LOGGER.info("Signal change strategy class: {}. Configuration: {}",
                    signalChangeStrategy.getClass(),
                    JsonUtils.toPrettyJsonString(signalChangeStrategy));

        StandardPeriodicFeatureTradingAlgo result = new StandardPeriodicFeatureTradingAlgo(
                positionManager,
                tradingDay,
                algoLogger,
                signalChangeStrategy,
                tradingStrategy,
                stopBuffer,
                cashoutUpperLevel,
                cashoutLowerLevel,
                maxGiveBack);

        if (configuration.getOptional("tradingSuspended", false)) {
            result.stopTrading("algo factory");
        }

        return result;
    }

    private static void addSlackAppender(Logger logger) {
        Appender slackAppender = new AsynchronousAppender(new SlackAppender("algo-slack",
                                                                            ALGO_SLACK_HOOK,
                                                                            TIME_AND_MESSAGE_LAYOUT));
        slackAppender.start();
        org.apache.logging.log4j.core.Logger extendedLogger = (org.apache.logging.log4j.core.Logger) logger;
        extendedLogger.addAppender(slackAppender);
    }
}
