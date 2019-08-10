package com.ax9k.utils.logging;

import com.ax9k.utils.json.JsonUtils;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;
import org.apache.logging.log4j.message.Message;

import java.util.Objects;

import static org.apache.commons.lang3.Validate.notNull;

@AsynchronouslyFormattable
public final class ImmutableObjectMessage implements Message {
    private final Object event;

    private String formatted;

    public ImmutableObjectMessage(Object event) {
        this.event = notNull(event);
    }

    @Override
    public String getFormat() {
        return getFormattedMessage();
    }

    @Override
    public String getFormattedMessage() {
        if (formatted == null) {
            formatted = JsonUtils.toJsonString(event);
        }
        return formatted;
    }

    @Override
    public Object[] getParameters() {
        return new Object[] { event };
    }

    @Override
    public Throwable getThrowable() {
        return event instanceof Throwable ? (Throwable) event : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ImmutableObjectMessage that = (ImmutableObjectMessage) o;
        return Objects.equals(event, that.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event);
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }
}
