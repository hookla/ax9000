package com.ax9k.utils.logging;

import com.ax9k.utils.json.JsonUtils;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.util.concurrent.TimeUnit;

import static com.ax9k.utils.json.JsonUtils.toJsonString;
import static java.lang.String.format;

public class DelayedSlackAppender extends AbstractAppender {
    private static final int OK = 200;
    private static final char NEW_LINE = '\n';

    private final String slackHookEndpoint;
    private final Layout<String> messageLayout;
    private final StringBuilder messageBuffer;

    public DelayedSlackAppender(String name, String slackHookEndpoint, Layout<String> messageLayout) {
        super(name, null, messageLayout);
        this.slackHookEndpoint = slackHookEndpoint;
        this.messageLayout = messageLayout;
        messageBuffer = new StringBuilder("[").append(name).append("]").append(NEW_LINE);
    }

    @Override
    public void append(LogEvent event) {
        messageBuffer.append(messageLayout.toSerializable(event)).append(NEW_LINE);
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        try {
            var response = Unirest.post(slackHookEndpoint)
                                  .header("Content-Type", "application/json")
                                  .body(toSlackPostRequest(messageBuffer.toString()))
                                  .asString();
            if (response.getStatus() != OK) {
                throw new UnirestException(JsonUtils.toJsonString(response));
            }
        } catch (UnirestException e) {
            throw new RuntimeException("Error sending message to Slack channel", e);
        }
        return true;
    }

    private static String toSlackPostRequest(String message) {
        return format("{ \"text\" : %s }", toJsonString(message));
    }
}
