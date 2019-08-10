package com.ax9k.app;

import com.ax9k.algo.Algo;
import com.ax9k.algo.AlgoFactory;
import com.ax9k.algo.trading.TradingAlgo;
import com.ax9k.app.contract.JsonContract;
import com.ax9k.backtesting.AutoFillBroker;
import com.ax9k.broker.Broker;
import com.ax9k.broker.BrokerCallbackReceiver;
import com.ax9k.broker.BrokerFactory;
import com.ax9k.core.marketmodel.Contract;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.time.Time;
import com.ax9k.core.time.TimestampedLayout;
import com.ax9k.positionmanager.PositionManager;
import com.ax9k.positionmanager.StandardPositionManagerFactory;
import com.ax9k.provider.MarketDataProvider;
import com.ax9k.provider.MarketDataProviderFactory;
import com.ax9k.service.RestService;
import com.ax9k.utils.config.Configuration;
import com.ax9k.utils.config.Configurations;
import com.ax9k.utils.logging.AsynchronousAppender;
import com.ax9k.utils.logging.DelayedSlackAppender;
import com.ax9k.utils.logging.FunctionLayout;
import com.ax9k.utils.logging.SlackAppender;
import com.ax9k.utils.path.PathLoader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Observer;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;

import static com.ax9k.app.SupportedOptions.ALGO_NAME;
import static com.ax9k.app.SupportedOptions.ALGO_PROPERTIES;
import static com.ax9k.app.SupportedOptions.BROKER_NAME;
import static com.ax9k.app.SupportedOptions.BROKER_PROPERTIES;
import static com.ax9k.app.SupportedOptions.DONT_COPY_LOGS;
import static com.ax9k.app.SupportedOptions.EXIT_BETWEEN_TRADING_SESSIONS;
import static com.ax9k.app.SupportedOptions.PROVIDER_NAME;
import static com.ax9k.app.SupportedOptions.PROVIDER_PROPERTIES;
import static com.ax9k.app.SupportedOptions.REST_SERVICE;
import static com.ax9k.app.SupportedOptions.RISK_MANAGER_CONFIG;
import static com.ax9k.app.SupportedOptions.SLACK_ERROR_LOG;
import static com.ax9k.app.SupportedOptions.TESTING_MODE;
import static com.ax9k.app.SupportedOptions.TRAINING_CONFIG;
import static java.lang.String.format;

public class Main {
    private static final Logger LOGGER = LogManager.getRootLogger();
    private static final String ERROR_CHANNEL_HOOK =
            "https://hooks.slack.com/services/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";
    private static final Layout<String> TIME_THREAD_AND_MESSAGE_LAYOUT = new FunctionLayout(
            event -> format("%s [%s] %s: %s",
                            Time.currentTime(),
                            event.getThreadName(),
                            event.getLevel(),
                            event.getMessage().getFormattedMessage()));
    private static final String STARTUP_AND_SHUTDOWN_CHANNEL_HOOK =
            "https://hooks.slack.com/services/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";

    private static boolean backTesting;

    public static void main(String[] arguments) {
        Set<Appender> startupAppenders = new HashSet<>();
        startupAppenders.add(addFileAppenderToRootLogger("Startup"));

        CommandLine commandLine = new CommandLineArgumentParser(arguments).commandLine();
        backTesting = commandLine.hasOption(TESTING_MODE.getLongOpt());
        Time.setLive(!backTesting);

        if (!backTesting) {
            startupAppenders.add(addSlackAppenderToRootLogger("Startup"));
        }

        LOGGER.info("=======================================================");
        LOGGER.info("Starting AX9K!!");
        LOGGER.info("=======================================================");

        ConfigurationRecorder.save(commandLine);
        configureErrorLog(commandLine);

        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            LOGGER.error("Exception in thread '" + thread.getName() + "'. ", error);

            Logger errorLog = LogManager.getLogger("error");

            StringWriter output = new StringWriter();

            PrintWriter writer = new PrintWriter(output);
            writer.append("Exception in thread '").append(thread.getName()).append("'. ");
            error.printStackTrace(writer);
            writer.flush();

            errorLog.error(output.toString());
        });

        var tradingDay = new TradingDay();

        Configuration riskManagerConfig =
                loadConfigurationFile(commandLine, RISK_MANAGER_CONFIG);
        PositionManager positionManager = new StandardPositionManagerFactory()
                .create(riskManagerConfig,
                        backTesting,
                        commandLine.hasOption(EXIT_BETWEEN_TRADING_SESSIONS.getLongOpt()));

        Broker broker = createBroker(commandLine, tradingDay, positionManager.getBrokerCallbackReceiver());
        LOGGER.info("Found Broker implementation: {}", broker.getClass().getName());

        positionManager.getOrderReceiver().initialiseBroker(broker);
        broker.connect();
        positionManager.getPositionReporter().getContractMultiplier();

        Configuration providerConfig = loadConfigurationOptions(commandLine, PROVIDER_PROPERTIES);
        MarketDataProvider provider = loadService(MarketDataProviderFactory.class, commandLine, PROVIDER_NAME)
                .create(tradingDay, providerConfig);
        LOGGER.info("Found MarketDataProvider implementation: {}", provider.getClass().getName());

        Configuration algoConfig = loadConfigurationOptions(commandLine, ALGO_PROPERTIES);
        Algo algo = loadService(AlgoFactory.class, commandLine, ALGO_NAME)
                .create(positionManager,
                        tradingDay,
                        algoConfig,
                        provider.getExtraDataTypes());
        LOGGER.info("Found Algo implementation: {}", algo.getClass().getName());

        tradingDay.addObserver(algo);
        tradingDay.setBookUpdateConsumer(positionManager.getMarketDataProviderCallbackReceiver()::updateBookValues);

        Runnable shutDownProcedure = () -> {
            Set<Appender> shutdownAppenders = addFileAndSlackAppendersToRootLogger();

            LOGGER.info("=======================================================");
            LOGGER.info("Shutting Down ...");
            LOGGER.info("=======================================================");

            if (algo instanceof TradingAlgo) {
                ((TradingAlgo) algo).exitPosition("AX9k is shutting down.");
            }

            LOGGER.info(algo);
            LOGGER.info(positionManager.getPositionReporter());
            LOGGER.info(tradingDay);

            String symbol = provider.getSymbol();
            Configuration trainingConfig = loadConfigurationFile(commandLine, TRAINING_CONFIG);
            var training = new TrainingPhase(commandLine, trainingConfig, provider.getDate(), symbol);

            training.run();
            if (!commandLine.hasOption(DONT_COPY_LOGS.getLongOpt())) {
                var report = new EndOfDayReport(commandLine,
                                                positionManager.getPositionReporter(),
                                                algo,
                                                provider,
                                                tradingDay);
                report.upload();
            }
            provider.stopRequest();
            broker.disconnect();

            LOGGER.info("=======================================================");
            LOGGER.info("**SHUTDOWN**");
            LOGGER.info("=======================================================");

            removeAppendersFromRootLogger(shutdownAppenders);
        };

        if (commandLine.hasOption(REST_SERVICE.getLongOpt())) {
            RestService service =
                    new RestService(shutDownProcedure, positionManager, tradingDay, algo, provider, broker);
            service.start();
            LOGGER.info("REST Service Started at {}. Version: {}", service.getStartTime(), service.getVersion());
        } else {
            LOGGER.info("REST Service NOT Started");
        }

        if (!backTesting) {
            tradingDay.getHeartBeat().start(Instant.now());
        }

        if (backTesting) {
            Thread shutDown = new Thread(shutDownProcedure);
            shutDown.setName("shut-down");
            Runtime.getRuntime().addShutdownHook(shutDown);
        }

        LOGGER.info("=======================================================");
        LOGGER.info("AX9K is Running... PID: {}.", ProcessHandle.current().pid());
        LOGGER.info("=======================================================");

        removeAppendersFromRootLogger(startupAppenders);

        provider.startRequest(true);
    }

    private static void configureErrorLog(CommandLine commandLine) {
        if (!commandLine.hasOption(SLACK_ERROR_LOG.getLongOpt())) {
            return;
        }

        Appender slackAppender = new AsynchronousAppender(new SlackAppender("error-slack",
                                                                            ERROR_CHANNEL_HOOK,
                                                                            TIME_THREAD_AND_MESSAGE_LAYOUT));
        slackAppender.start();

        org.apache.logging.log4j.core.Logger errorLogger =
                (org.apache.logging.log4j.core.Logger) LogManager.getLogger("error");
        errorLogger.addAppender(slackAppender);
    }

    private static Configuration loadConfigurationFile(CommandLine commandLine,
                                                       Option fileLocation) {
        String locationValue = commandLine.getOptionValue(fileLocation.getLongOpt());
        if (locationValue == null) {
            return Configurations.empty();
        }

        return Configurations.load(locationValue);
    }

    private static Configuration loadConfigurationOptions(CommandLine commandLine, Option dynamicProperties) {
        Configuration baseConfiguration =
                Configurations.load(commandLine.getOptionProperties(dynamicProperties.getOpt()));
        return baseConfiguration.loadExternalFileFromOptions();
    }

    private static Broker createBroker(CommandLine commandLine,
                                       TradingDay tradingDay,
                                       BrokerCallbackReceiver brokerCallbackReceiver) {
        Configuration configuration = loadConfigurationOptions(commandLine, BROKER_PROPERTIES);

        Contract contract = null;
        if (configuration.hasOption("contractFile")) {
            Path configFile = PathLoader.load(configuration.get("contractFile"));
            contract = JsonContract.fromFile(configFile);
            Time.setTradingSchedule(contract.getTradingSchedule());
        }

        Broker broker;
        if (backTesting) {
            configuration.requireOptions("slippage", "contractFile");
            int slippage = configuration.get("slippage", Integer.class);

            broker = new AutoFillBroker(contract,
                                        brokerCallbackReceiver,
                                        slippage);
            return broker;
        } else {
            broker = loadService(BrokerFactory.class, commandLine, BROKER_NAME)
                    .create(contract, brokerCallbackReceiver, configuration);
        }

        if (broker instanceof Observer) {
            tradingDay.addObserver((Observer) broker);
        }

        return broker;
    }

    private static <T> T loadService(Class<T> type, CommandLine commandLine, Option factoryOption) {
        var loader = ServiceLoader.load(type).stream();

        if (commandLine.hasOption(factoryOption.getLongOpt())) {
            String filter = commandLine.getOptionValue(factoryOption.getLongOpt()).trim();
            loader = loader.filter((provider) -> provider.type().getName().contains(filter));
        }

        return loader
                .findFirst()
                .map(ServiceLoader.Provider::get)
                .orElseThrow(noImplementationsFoundError(type));
    }

    private static Supplier<IllegalStateException> noImplementationsFoundError(Class<?> type) {
        return () -> new IllegalStateException("No " + type.getSimpleName() + " implementations found");
    }

    private static Appender addFileAppenderToRootLogger(String name) {
        var extendedLogger = (org.apache.logging.log4j.core.Logger) LOGGER;

        PatternLayout oneMessagePerLine = PatternLayout.newBuilder()
                                                       .withPattern("%m%n")
                                                       .build();
        Appender fileAppender = createFileAppender(name, oneMessagePerLine);
        fileAppender.start();
        extendedLogger.addAppender(fileAppender);
        return fileAppender;
    }

    private static Appender createFileAppender(String baseFileName, StringLayout baseLayout) {
        var timestampedLayout = TimestampedLayout.createLayout(baseLayout);
        String fileName = format("temp/%s.log", baseFileName);

        return FileAppender.newBuilder()
                           .withLayout(timestampedLayout)
                           .withName(baseFileName.concat("Log"))
                           .withFileName(fileName)
                           .withAppend(false)
                           .build();
    }

    private static Appender addSlackAppenderToRootLogger(String name) {
        var extendedLogger = (org.apache.logging.log4j.core.Logger) LOGGER;
        StringLayout timeNameAndMessage = new FunctionLayout(event -> {
            String time;
            try {
                time = Time.currentTime().toString();
            } catch (IllegalStateException e) {
                time = LocalTime.ofInstant(Time.now(), ZoneOffset.UTC) + " UTC";
            }

            return format("%s: %s", time, event.getMessage().getFormattedMessage());
        });
        Appender slackAppender = new DelayedSlackAppender(name,
                                                          STARTUP_AND_SHUTDOWN_CHANNEL_HOOK,
                                                          timeNameAndMessage);
        slackAppender.start();
        extendedLogger.addAppender(slackAppender);

        return slackAppender;
    }

    private static Set<Appender> addFileAndSlackAppendersToRootLogger() {
        Appender fileAppender = addFileAppenderToRootLogger("Shutdown");

        if (!backTesting) {
            Appender slackAppender = addSlackAppenderToRootLogger("Shutdown");
            return Set.of(fileAppender, slackAppender);
        }

        return Set.of(fileAppender);
    }

    private static void removeAppendersFromRootLogger(Collection<Appender> appenders) {
        var extendedLogger = (org.apache.logging.log4j.core.Logger) LOGGER;

        appenders.forEach(appender -> {
            appender.stop();
            extendedLogger.removeAppender(appender);
        });
    }
}
