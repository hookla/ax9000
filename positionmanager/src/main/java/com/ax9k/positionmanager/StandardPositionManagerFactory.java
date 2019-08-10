package com.ax9k.positionmanager;

import com.ax9k.core.time.Time;
import com.ax9k.utils.config.Configuration;
import com.ax9k.utils.logging.AsynchronousAppender;
import com.ax9k.utils.logging.FunctionLayout;
import com.ax9k.utils.logging.SlackAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;

import static java.lang.String.format;

public class StandardPositionManagerFactory {
    private static final String POSITION_MANAGER_SLACK_HOOK =
            "https://hooks.slack.com/services/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";
    private static final String POSITION_LOG = "positionLogger";
    private static final Layout<String> TIME_AND_MESSAGE_LAYOUT = new FunctionLayout(
            event -> format("[P] %s: %s", Time.currentTime(), event.getMessage().getFormattedMessage()));

    public StandardPositionManager create(Configuration riskManagerConfig,
                                          boolean backtestingMode,
                                          boolean exitBetweenTradingSessions) {
        Logger logger = LogManager.getLogger(POSITION_LOG);
        if (!backtestingMode) {
            addSlackAppender(logger);
        }

        return new StandardPositionManager(riskManagerConfig, logger, exitBetweenTradingSessions);
    }

    private static void addSlackAppender(Logger logger) {
        Appender slackAppender = new AsynchronousAppender(new SlackAppender("position-slack",
                                                                            POSITION_MANAGER_SLACK_HOOK,
                                                                            TIME_AND_MESSAGE_LAYOUT));
        slackAppender.start();
        org.apache.logging.log4j.core.Logger extendedLogger = (org.apache.logging.log4j.core.Logger) logger;
        extendedLogger.addAppender(slackAppender);
    }
}
