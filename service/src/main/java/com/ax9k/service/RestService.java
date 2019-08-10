package com.ax9k.service;

import com.ax9k.algo.Algo;
import com.ax9k.algo.trading.TradingAlgo;
import com.ax9k.broker.Broker;
import com.ax9k.core.marketmodel.TradingDay;
import com.ax9k.core.time.Time;
import com.ax9k.positionmanager.OrderReceiver;
import com.ax9k.positionmanager.PositionManager;
import com.ax9k.positionmanager.PositionReporter;
import com.ax9k.provider.MarketDataProvider;
import com.ax9k.service.paths.Paths;
import com.ax9k.utils.json.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.initExceptionHandler;
import static spark.Spark.options;
import static spark.Spark.staticFiles;

@SuppressWarnings("unused")
public final class RestService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String REQUEST_THREAD_NAME = "REST-Interface";
    private static final int ERROR = 500;
    private static final int UNSUPPORTED = 501;
    private static final int OK = 200;
    private static final int BAD_REQUEST = 400;
    private final Runnable shutDownProcedure;
    private final OrderReceiver orderReceiver;
    private final PositionReporter positionReporter;
    private final TradingDay tradingDay;
    private final Algo algo;
    private final MarketDataProvider provider;
    private final Broker broker;
    private Instant startTime;
    private String logFileDirectoryPath;

    public RestService(Runnable shutDownProcedure,
                       PositionManager positionManager,
                       TradingDay tradingDay,
                       Algo algo,
                       MarketDataProvider provider,
                       Broker broker) {
        this.shutDownProcedure = shutDownProcedure;
        this.orderReceiver = positionManager.getOrderReceiver();
        this.positionReporter = positionManager.getPositionReporter();
        this.tradingDay = tradingDay;
        this.algo = algo;
        this.provider = provider;
        this.broker = broker;
    }

    public void start() {
        logFileDirectoryPath = findLogFileDirectory();
        LOGGER.info("Exposing static files at: {}", logFileDirectoryPath);
        staticFiles.externalLocation(logFileDirectoryPath);

        initExceptionHandler(RestService::reportError);

        exception(RuntimeException.class, this::handleUncaughtException);

        options(
                "/*",
                (request, response) -> {

                    String accessControlRequestHeaders = request
                            .headers("Access-Control-Request-Headers");
                    if (accessControlRequestHeaders != null) {
                        response.header(
                                "Access-Control-Allow-Headers",
                                accessControlRequestHeaders
                        );
                    }

                    String accessControlRequestMethod = request
                            .headers("Access-Control-Request-Method");
                    if (accessControlRequestMethod != null) {
                        response.header(
                                "Access-Control-Allow-Methods",
                                accessControlRequestMethod
                        );
                    }

                    return "OK";
                }
        );

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            Thread currentThread = Thread.currentThread();
            if (!currentThread.getName().equals(REQUEST_THREAD_NAME)) {
                currentThread.setName(REQUEST_THREAD_NAME);
            }
        });

        get(Paths.CURRENT_BOOK, this::sendCurrentBookJson);
        get(Paths.FEATURES, this::sendFeaturesJson);

        get(Paths.ALGO, this::sendAlgoJson);
        get(Paths.PERIODIC_FEATURES, this::sendPeriodicFeatures);

        get(Paths.TRADING_DAY, this::sendTradingDayJson);
        get(Paths.ORDERS, this::sendOrdersJson);
        get(Paths.MARKET_LAST_TRADE, this::sendMarketLastTradeJson);
        get(Paths.LATEST_BAR, this::sendLatestBarJson);

        get(Paths.BROKER, this::sendBrokerJson);
        get(Paths.BROKER_LAST_TRADE, this::sendBrokerLastTradeJson);
        get(Paths.BROKER_CONNECT, this::brokerConnect);
        get(Paths.BROKER_DISCONNECT, this::brokerDisconnect);

        get(Paths.EXIT_POSITION, this::forceExitPosition);
        get(Paths.POSITION_MANAGER_BUY, this::positionManagerBuy);
        get(Paths.POSITION_MANAGER_SELL, this::positionManagerSell);

        get(Paths.PROVIDER, this::sendProviderJson);
        get(Paths.PROVIDER_START_REQUEST, this::providerStartRequest);
        get(Paths.PROVIDER_STOP_REQUEST, this::providerStopRequest);
        get(Paths.POSITION, this::sendPositionManagerJson);
        get(Paths.POSITION_CANCEL_ALL_PENDING_ORDERS, this::cancelAllPendingOrders);

        get(Paths.ASKS, this::sendAsksJson);
        get(Paths.BIDS, this::sendBidsJson);
        get(Paths.HEART_BEAT, this::sendHeartbeat);
        get(Paths.CLOCK, this::sendClockJson);

        get(Paths.RESUME_TRADING, this::forceResumeTrading);
        get(Paths.BROKER_REQUEST_DATA, this::brokerRequestData);

        get(Paths.SHUT_DOWN, this::shutDown);

        startTime = Instant.now();
    }

    private static String findLogFileDirectory() {
        Path workingDir = java.nio.file.Paths.get(System.getProperty("user.dir"));

        try {
            return Files.walk(workingDir, 6)
                        .filter(Files::isDirectory)
                        .filter((path) -> path.getFileName().toString().equals("temp"))
                        .map(Path::toAbsolutePath)
                        .map(Path::toString)
                        .findFirst()
                        .orElseThrow();
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't find static file directory", e);
        }
    }

    private String forceResumeTrading(Request request, Response response) {
        if (algo instanceof TradingAlgo) {
            ((TradingAlgo) algo).forceResumeTrading("REST_API");
            return ok(response);
        } else {
            reportError(new UnsupportedOperationException(algo.getAlgoName() + " is not a trading algo"));
            response.status(UNSUPPORTED);
            return "Not Implemented";
        }
    }

    private static void reportError(Throwable throwable) {
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), throwable);
    }

    private String shutDown(Request request, Response response) {
        shutDownProcedure.run();

        Thread shutDown = new Thread(() -> {
            Spark.stop();
            Runtime.getRuntime().halt(0);
        }, "REST-Shutdown");

        shutDown.start();

        return ok(response);
    }

    private void handleUncaughtException(Exception exception, Request request, Response response) {
        reportError(exception);
        response.status(ERROR);
        response.body("Internal Server Error");
    }

    private String sendClockJson(Request ignored, Response response) {
        response.header("Content-Type", "application/json");
        var values = Map.of(
                "localDate", Time.currentDate().toString(),
                "localTime", Time.currentTime().toString(),
                "utcTimestamp", Time.now().toString()
        );

        return JsonUtils.toPrettyJsonString(values);
    }

    private String sendHeartbeat(Request request, Response response) {
        return json(response, this);
    }

    private String sendPeriodicFeatures(Request request, Response response) {
        return json(response, algo.getPeriodicUpdates());
    }

    private String sendProviderJson(Request request, Response response) {
        return json(response, provider);
    }

    private String providerStartRequest(Request request, Response response) {
        response.header("Content-Type", "application/json");
        algo.getAlgoLogger().info("Requesting start request from provider");

        provider.startRequest(false);
        return ok(response);
    }

    private String providerStopRequest(Request request, Response response) {
        response.header("Content-Type", "application/json");
        algo.getAlgoLogger().info("Requesting stop request from provider");

        provider.stopRequest();
        return ok(response);
    }

    private String brokerDisconnect(Request request, Response response) {
        response.header("Content-Type", "application/json");
        broker.disconnect();
        return ok(response);
    }

    private String brokerConnect(Request request, Response response) {
        response.header("Content-Type", "application/json");
        broker.connect();
        return ok(response);
    }

    private static String ok(Response response) {
        response.status(OK);
        return "OK";
    }

    private String sendAsksJson(Request request, Response response) {
        return json(response, tradingDay.getCurrentBook().getAsks());
    }

    private static String json(Response response, Object[] content) {
        response.header("Content-Type", "application/json");
        return JsonUtils.toPrettyJsonString(content);
    }

    private String sendBidsJson(Request request, Response response) {
        return json(response, tradingDay.getCurrentBook().getBids());
    }

    private String sendPositionManagerJson(Request request, Response response) {
        return json(response, positionReporter);
    }

    private String sendBrokerJson(Request request, Response response) {
        return json(response, broker);
    }

    private String sendCurrentBookJson(Request request, Response response) {
        return json(response, tradingDay.getCurrentBook());
    }

    private static String json(Response response, Object content) {
        response.header("Content-Type", "application/json");
        return content.toString();
    }

    private String sendFeaturesJson(Request request, Response response) {
        return json(response, algo.getFeaturesJson());
    }

    private String sendAlgoJson(Request request, Response response) {
        response.header("Content-Type", "application/json");
        return algo.toString();
    }

    private String sendLatestBarJson(Request request, Response response) {
        return json(response, tradingDay.getLatestBar());
    }

    private String sendTradingDayJson(Request request, Response response) {
        return json(response, tradingDay);
    }

    private String sendOrdersJson(Request request, Response response) {
        return json(response, positionReporter.getCurrentPosition().getOrdersJson());
    }

    private String sendMarketLastTradeJson(Request request, Response response) {
        return json(response, tradingDay.getLastTrade());
    }

    private String sendBrokerLastTradeJson(Request request, Response response) {
        return json(response, positionReporter.getLastTrade());
    }

    private String brokerRequestData(Request request, Response response) {
        broker.requestData();
        algo.getAlgoLogger().info("Requested data refresh from broker");
        return ok(response);
    }

    private String forceExitPosition(Request request, Response response) {
        orderReceiver.exitPosition("request sent from GUI");
        return ok(response);
    }

    private String cancelAllPendingOrders(Request request, Response response) {
        orderReceiver.cancelAllPendingOrders("request sent from GUI");
        return ok(response);
    }

    private String positionManagerBuy(Request request, Response response) {
        double quantity = 0.1;

        String quantityText = request.queryParams("quantity");
        try {
            if (quantityText != null) {
                quantity = Double.valueOf(quantityText);
            }
        } catch (NumberFormatException invalidValue) {
            return invalidQuantity(response, quantityText);
        }

        algo.getAlgoLogger().info("Sending buy order for {} ...", quantity);
        orderReceiver.buy("REST_API", null, quantity, -1);
        return ok(response);
    }

    private String invalidQuantity(Response response, String text) {
        response.status(BAD_REQUEST);
        return BAD_REQUEST + " Invalid Quantity: " + text;
    }

    private String positionManagerSell(Request request, Response response) {
        double quantity = 0.1;

        String quantityText = request.queryParams("quantity");
        try {
            if (quantityText != null) {
                quantity = Double.valueOf(quantityText);
            }
        } catch (NumberFormatException invalidValue) {
            return invalidQuantity(response, quantityText);
        }

        algo.getAlgoLogger().info("Sending sell order for {} ...", quantity);
        orderReceiver.sell("REST_API", null, quantity, -1);
        return ok(response);
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }

    public void stop() {
        Spark.stop();
    }

    public String getStartTime() {
        return Time.localise(startTime).format(ISO_LOCAL_DATE_TIME);
    }

    public long getRunningTimeMinutes() {
        return Duration.between(startTime, Instant.now()).toMinutes();
    }

    public long getPID() {
        return ProcessHandle.current().pid();
    }

    public String getVersion() {
        String VERSION = "0.7.42";
        return VERSION;
    }

    public long getTotalMemory() {
        return toMegabytes(Runtime.getRuntime().totalMemory());
    }

    private long toMegabytes(long bytes) {
        return Math.round(bytes / 1024d / 1024d);
    }

    public long getUsedMemory() {
        return toMegabytes(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    }

    public long getFreeMemory() {
        return toMegabytes(Runtime.getRuntime().freeMemory());
    }

    public String getLogFilesDirectory() {
        return logFileDirectoryPath;
    }
}
