package com.ax9k.cex.client;

import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CexClient {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Logger ERROR_LOG = LogManager.getLogger("error");
    private static final URI CEX_ENDPOINT = URI.create("wss://ws.cex.io/ws/");
    private static final MessageType GET_BALANCE = MessageType.of("get-balance");
    private static final String PONG_MESSAGE = JsonMapper.write(Map.of("e", "pong"));
    private static final String GET_BALANCE_MESSAGE = JsonMapper.write(Map.of("e", "get-balance",
                                                                              "oid", "12345_get-balance"));
    private static final String CONNECTED_MESSAGE = "{\"e\":\"connected\"}";
    private static final String PING_MESSAGE_TYPE = "{\"e\":\"ping\"";
    private static final int KEEP_ALIVE_INTERVAL = 14;

    private final ScheduledExecutorService keepAliveExecutor;
    private final Map<Class<?>, Consumer<CexClient>> reconnectionCallbacks = new ConcurrentHashMap<>();
    private final ConcurrentHolder<Boolean> signal = new ConcurrentHolder<>();
    private final ClientResponseDelegator responseHandler = new ClientResponseDelegator(signal);
    private final HandlerQueue handlerQueue = new HandlerQueue(responseHandler);
    private final ParserQueue parserQueue = new ParserQueue(handlerQueue);
    private final SignatureGenerator signatureGenerator;

    private volatile boolean running;

    private CexWebSocket socket;

    public CexClient(SignatureGenerator signatureGenerator) {
        this.signatureGenerator = signatureGenerator;

        ThreadFactory keepAliveThreadFactory = new BasicThreadFactory.Builder()
                .daemon(true)
                .namingPattern("cex-keep-alive-thread-%s")
                .uncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler())
                .build();
        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor(keepAliveThreadFactory);
    }

    public void disconnect() {
        running = false;
        keepAliveExecutor.shutdownNow();
        try {
            handlerQueue.stop();
            parserQueue.stop();
            socket.closeBlocking();
        } catch (InterruptedException e) {
            ERROR_LOG.warn("CexClient connection termination interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    public void makeRequest(CexRequest request) {
        makeRawRequest(request.toJson());
    }

    public void makeRawRequest(String request) {
        connect();
        socket.send(request);
    }

    public void connect() {
        if (running) {
            return;
        }

        handlerQueue.start();
        parserQueue.start();
        socket = new CexWebSocket();
        running = true;
        boolean connected;
        try {
            socket.connectBlocking();
            LOGGER.info("Awaiting connection ...");
            connected = signal.await();
            LOGGER.info("Waiting complete.");
        } catch (InterruptedException e) {
            ERROR_LOG.info("Client interrupted while waiting for connection to be established");
            Thread.currentThread().interrupt();
            connected = false;
        }
        if (!connected) {
            ERROR_LOG.error("Connection failed.");
            running = false;
            return;
        }

        responseHandler.registerHandler(GET_BALANCE, NoOpHandler.INSTANCE);
        keepAliveExecutor.scheduleAtFixedRate(() -> socket.send(GET_BALANCE_MESSAGE),
                                              KEEP_ALIVE_INTERVAL,
                                              KEEP_ALIVE_INTERVAL,
                                              TimeUnit.SECONDS);
    }

    public void onDisconnectionRecovery(Class<?> callerType, Consumer<CexClient> callback) {
        reconnectionCallbacks.put(callerType, callback);
    }

    public void cancelDisconnectionRecovery(Class<?> callerType) {
        reconnectionCallbacks.remove(callerType);
    }

    public void registerResponseHandler(MessageType type, ResponseHandler handler) {
        responseHandler.registerHandler(type, handler);
    }

    public boolean isConnected() {
        return running;
    }

    private class CexWebSocket extends WebSocketClient {
        CexWebSocket() {
            super(CexClient.CEX_ENDPOINT);
        }

        @Override
        public void onOpen(ServerHandshake handshakeData) {
            LOGGER.info("WebSocket connection opened. Handshake data: {}",
                        handshakeData.getHttpStatusMessage());

            long timestampSeconds = Instant.now().getEpochSecond();
            String signature = signatureGenerator.generate(timestampSeconds);

            ObjectNode authenticationParams = JsonMapper.createObjectNode();
            authenticationParams.put("key", signatureGenerator.getApiKey());
            authenticationParams.put("signature", signature);
            authenticationParams.put("timestamp", timestampSeconds);

            ObjectNode authenticationRequest = JsonMapper.createObjectNode();
            authenticationRequest.put("e", "auth");
            authenticationRequest.set("auth", authenticationParams);

            String request = JsonUtils.toPrettyJsonString(authenticationRequest);
            LOGGER.info("Authentication Request: {}", request);
            send(request);
        }

        @Override
        public void onMessage(String message) {
            if (message.contains("\"ohlcv1m\"")) {
                LOGGER.info(message);
            }

            if (message.startsWith(PING_MESSAGE_TYPE)) {
                send(PONG_MESSAGE);
            } else if (!message.equals(CONNECTED_MESSAGE)) {
                parserQueue.processAsync(message);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (running) {
                ERROR_LOG.warn("WebSocket connection terminated unexpectedly. Code: {}, Reason: {}, Source: {}",
                               code, reason, remote ? "Remote host" : "Local");
                ERROR_LOG.warn("Attempting reconnect ...");
                running = false;
                CexClient.this.connect();
                reconnectionCallbacks.values().forEach(callback -> callback.accept(CexClient.this));
            } else {
                LOGGER.info("WebSocket connection terminated.");
            }
        }

        @Override
        public void onError(Exception exception) {
            Thread.getDefaultUncaughtExceptionHandler()
                  .uncaughtException(Thread.currentThread(), exception);
        }
    }
}
