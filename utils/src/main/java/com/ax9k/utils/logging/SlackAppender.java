package com.ax9k.utils.logging;

import com.ax9k.utils.json.JsonUtils;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.util.function.UnaryOperator;

import static com.ax9k.utils.json.JsonUtils.toJsonString;
import static java.lang.String.format;

public final class SlackAppender extends AbstractAppender {
    private static final UnaryOperator<String> TO_JSON_OBJECT =
            (message) -> format("{ \"text\" : %s }", toJsonString(message));
    private static final int OK = 200;

    private final String slackHookEndpoint;
    private final Layout<String> jsonObjectLayout;

    public SlackAppender(String name, String slackHookEndpoint, Layout<String> messageLayout) {
        super(name, null, messageLayout);
        this.slackHookEndpoint = slackHookEndpoint;
        jsonObjectLayout = new TransformingLayout(messageLayout, TO_JSON_OBJECT);
    }

    @Override
    public void append(LogEvent event) {
        try {
            var response = Unirest.post(slackHookEndpoint)
                                  .header("Content-Type", "application/json")
                                  .body(jsonObjectLayout.toSerializable(event))
                                  .asString();
            if (response.getStatus() != OK) {
                throw new UnirestException(JsonUtils.toJsonString(response));
            }
        } catch (UnirestException e) {
            throw new RuntimeException("Error sending message to Slack channel", e);
        }
    }
}
