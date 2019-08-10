package com.ax9k.interactivebrokers.client;

import com.ax9k.core.marketmodel.Contract;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.TradingSchedule;
import com.ax9k.core.time.Time;
import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

@SuppressWarnings("unused") // Jackson properties
@JsonPropertyOrder({ "source", "connected" })
public abstract class IbClient extends EWrapperAdapter {
    protected static final Logger LOGGER = LogManager.getLogger();
    protected static final Logger ERROR_LOG = LogManager.getLogger("error");

    private static final int ONE_SECOND = 1000;
    private static final int TEN_SECONDS = 10;
    private static final int MARKET_DATA_OK = -1;
    private static final int SOCKET_EOF = 507;
    protected final EClientSocket client;
    protected final com.ib.client.Contract requestContract;
    private final Collection<ScheduledFuture<?>> delayedRequests = new ArrayList<>();
    private final EJavaSignal signal;
    private final String gatewayHost;
    private final int gatewayPort;
    private final int clientId;
    private final int contractId;
    private final String exchange;
    private final String className = getClass().getSimpleName();
    protected Contract contract;
    private EReader reader;
    private Thread messageProcessor;
    private ScheduledFuture<?> connectionMonitor;

    protected IbClient(String gatewayHost, int gatewayPort, int clientId, String exchange, int contractId) {
        this.gatewayHost = gatewayHost;
        this.gatewayPort = gatewayPort;
        this.clientId = clientId + 10;
        this.contractId = contractId;
        this.exchange = exchange;
        signal = new EJavaSignal();
        client = new EClientSocket(this, signal);
        client.setAsyncEConnect(false);
        requestContract = initialiseContract();
        LOGGER.info("Instantiated {} client with ID: {}. Exchange: {}, Contract ID: {}",
                    getClass().getSimpleName(),
                    clientId,
                    exchange,
                    contractId);
    }

    private com.ib.client.Contract initialiseContract() {
        com.ib.client.Contract result = new com.ib.client.Contract();
        result.conid(contractId);
        result.exchange(exchange);
        return result;
    }

    private void monitorConnection() {
        if (!client.isConnected()) {
            ERROR_LOG.warn("'{}' IB connection has been terminated", className);
            connectionClosed();
        }
    }

    @Override
    public void connectionClosed() {
        if (!client.isConnected()) {
            ERROR_LOG.warn("'{}' Restablishing connection ...", className);
            connect();
            ERROR_LOG.warn("'{}' Resubscribing to market data ...", className);
            makeDataRequests(true);
        }
    }

    protected void connect() {
        if (client.isConnected()) {
            ERROR_LOG.info("Requested to connect but already connected");
            return;
        }

        establishClientConnection();

        reader = new EReader(client, signal);

        reader.setName(resolveThreadName("reader"));
        messageProcessor = new Thread(this::processMessages, resolveThreadName("message-processor"));

        ThreadFactory monitorThreadFactory = new BasicThreadFactory.Builder()
                .daemon(true)
                .namingPattern(resolveThreadName("monitor"))
                .uncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler())
                .build();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(monitorThreadFactory);
        connectionMonitor = executor.scheduleAtFixedRate(this::monitorConnection,
                                                         1,
                                                         1,
                                                         TimeUnit.MINUTES);

        reader.start();
        messageProcessor.start();
    }

    private void establishClientConnection() {
        LOGGER.info("Connecting '{}' to Interactive Brokers gateway.. Host: {}, Port: {}, Client ID: {}",
                    className, gatewayHost, gatewayPort, clientId);
        client.eConnect(gatewayHost, gatewayPort, clientId);
        try {
            int waitCount = 0;
            while (!client.isConnected()) {
                if (++waitCount == TEN_SECONDS) {
                    ERROR_LOG.warn("{} connection not made. Waiting ... Client ID: {}",
                                   className,
                                   clientId);
                    waitCount = 0;
                } else {
                    LOGGER.warn("{} connection not made. Waiting ... Client ID: {}",
                                className,
                                clientId);
                }
                Thread.sleep(ONE_SECOND);
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Error waiting for client to connect", e);
        }
        LOGGER.info("Client '{}' connected to Interactive Brokers gateway. Client ID: {}",
                    className, clientId);
    }

    private String resolveThreadName(String base) {
        return className.toLowerCase() + "-" + base;
    }

    protected void makeDataRequests(boolean delayUntilMarketOpen) {
        if (!delayUntilMarketOpen) {
            requestData();
            return;
        }

        LocalTime currentTime = Time.localiseTime(Instant.now());

        TradingSchedule tradingSchedule = Time.schedule();
        Phase firstOpenPhase = tradingSchedule.firstMatchingPhase(Phase.MARKET_IS_OPEN);
        Validate.isTrue(firstOpenPhase != null, "Contract must have at least one open phase");
        LOGGER.info("First open phase for contract: {}", firstOpenPhase.getName());
        LocalTime marketOpen = firstOpenPhase.getStart().getTime();
        Duration timeDifference = Duration.between(currentTime, marketOpen);

        if (timeDifference.isZero() ||
            timeDifference.isNegative() ||
            tradingSchedule.phaseForTime(currentTime).isMarketOpen()) {
            requestData();
            return;
        }

        Runnable request = () -> {
            LOGGER.info("Making '{}' data request ...", getClass().getSimpleName());
            requestData();
        };

        ThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern(resolveThreadName("delayed-data-request-%s"))
                .daemon(true)
                .build();
        ScheduledFuture<?> delayedRequest = Executors.newSingleThreadScheduledExecutor(factory)
                                                     .schedule(request, timeDifference.toNanos(), TimeUnit.NANOSECONDS);
        LOGGER.warn("Delayed '{}' request for period of {}", getClass().getSimpleName(), timeDifference);
        delayedRequests.add(delayedRequest);
    }

    protected abstract void requestData();

    private String getGatewayHost() {
        return gatewayHost;
    }

    private int getGatewayPort() {
        return gatewayPort;
    }

    private int getClientId() {
        return clientId;
    }

    private int getContractId() {
        return contractId;
    }

    private void processMessages() {
        while (client.isConnected()) {
            signal.waitForSignal();
            try {
                reader.processMsgs();
            } catch (IOException e) {
                error(e);
            }
        }
    }

    @Override
    public void error(Exception e) {
        ERROR_LOG.error("IB Error. Client ID: {}, Exception: {}, Message: {}",
                        clientId, e.toString(), e.getMessage());
    }

    @Override
    public void error(String message) {
        ERROR_LOG.error("IB Error. Client ID: {}, Message: {}",
                        clientId, message);
    }

    @Override
    public void error(int id, int errorCode, String errorMsg) {
        if (errorCode == SOCKET_EOF) {
            connectionClosed();
        } else if (id != MARKET_DATA_OK) {
            ERROR_LOG.error("IB Error. Client ID: {},  ID: {}, Error Code: {}, Message: {}",
                            clientId, id, errorCode, errorMsg);
        }
    }

    protected void disconnect() {
        if (!client.isConnected()) {
            ERROR_LOG.info("Requested to disconnect but not connected");
            return;
        }

        client.eDisconnect();
        try {
            connectionMonitor.cancel(true);
            reader.join(ONE_SECOND);
            messageProcessor.join(ONE_SECOND);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        delayedRequests.forEach(future -> future.cancel(true));
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }
}
