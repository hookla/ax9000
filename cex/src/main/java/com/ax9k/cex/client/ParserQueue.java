package com.ax9k.cex.client;

import com.fasterxml.jackson.databind.JsonNode;

final class ParserQueue extends MessageQueue<String> {
    private final HandlerQueue handler;

    ParserQueue(HandlerQueue handler) {
        this.handler = handler;
    }

    @Override
    void process(String message) {
        JsonNode root = JsonMapper.read(message);
        handler.processAsync(CexResponse.of(root));
    }
}
