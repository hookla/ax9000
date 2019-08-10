package com.ax9k.utils.logging;

import com.ax9k.utils.json.JsonUtils;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;

import java.nio.charset.StandardCharsets;

@Plugin(name = "EventLogLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class EventLogLayout extends AbstractStringLayout {
    private static final String NEW_LINE = "\n";

    private EventLogLayout() {
        super(StandardCharsets.UTF_8);
    }

    @PluginFactory
    public static EventLogLayout createLayout() {
        return new EventLogLayout();
    }

    @Override
    public String toSerializable(LogEvent event) {
        Message message = event.getMessage();

        if (message instanceof ObjectMessage) {
            Object object = ((ObjectMessage) message).getParameter();
            return JsonUtils.toJsonString(object).concat(NEW_LINE);
        }

        return message.getFormattedMessage().concat(NEW_LINE);
    }
}
