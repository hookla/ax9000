package com.ax9k.cex.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientResponseDelegator implements ResponseHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Logger ERROR_LOG = LogManager.getLogger("error");

    private static final MessageType AUTHENTICATE = MessageType.of("auth");

    private final Map<MessageType, ResponseHandler> handlers = new ConcurrentHashMap<>();

    private final ConcurrentHolder<Boolean> authenticationSignal;

    ClientResponseDelegator(ConcurrentHolder<Boolean> authenticationSignal) {
        this.authenticationSignal = authenticationSignal;
    }

    void registerHandler(MessageType type, ResponseHandler handler) {
        handlers.put(type, handler);
    }

    boolean hasRegisteredHandler(MessageType type) {
        return handlers.containsKey(type);
    }

    @Override
    public void ok(CexResponse response) {
        MessageType type = response.getType();
        if (type.equals(AUTHENTICATE)) {
            if (response.isOk()) {
                LOGGER.info("Successfully authenticated connection.");
                authenticationSignal.hold(Boolean.TRUE);
            } else {
                authenticationSignal.hold(Boolean.FALSE);
            }
            return;
        }

        ResponseHandler responseHandler = handlers.get(type);
        if (responseHandler != null) {
            try {
                if (response.isOk()) {
                    responseHandler.ok(response);
                } else {
                    responseHandler.error(response);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid/unexpected response from CEX: " + response, e);
            }
        }
    }
}
