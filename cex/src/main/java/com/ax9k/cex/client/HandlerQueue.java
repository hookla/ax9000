package com.ax9k.cex.client;

final class HandlerQueue extends MessageQueue<CexResponse> {
    private final ResponseHandler handler;

    HandlerQueue(ResponseHandler handler) {
        this.handler = handler;
    }

    @Override
    void process(CexResponse message) {
        handler.ok(message);
    }
}
