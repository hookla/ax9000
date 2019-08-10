package com.ax9k.cex.client;

public enum NoOpHandler implements ResponseHandler {
    INSTANCE;

    @Override
    public void ok(CexResponse response) {

    }

    @Override
    public void error(CexResponse response) {

    }
}
