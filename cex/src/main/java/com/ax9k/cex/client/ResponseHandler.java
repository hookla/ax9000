package com.ax9k.cex.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.ax9k.utils.json.JsonUtils.toPrettyJsonString;

public interface ResponseHandler {
    Logger ERROR_LOG = LogManager.getLogger("error");

    void ok(CexResponse response);

    default void error(CexResponse response) {
        ERROR_LOG.error("CEX.io returned non-OK response: {}", toPrettyJsonString(response));
    }
}
