package com.ax9k.core.time;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static java.lang.String.format;
import static java.util.Objects.requireNonNullElse;

@Plugin(name = "TimestampedLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class TimestampedLayout extends AbstractStringLayout {
    private final StringLayout base;

    private TimestampedLayout(StringLayout base) {
        super(base.getCharset());
        this.base = base;
    }

    @PluginFactory
    public static TimestampedLayout createLayout(@PluginElement("base") StringLayout base) {
        return new TimestampedLayout(requireNonNullElse(base, PatternLayout.createDefaultLayout()));
    }

    @Override
    public String toSerializable(LogEvent event) {
        String time;
        if (Time.now().equals(Instant.EPOCH)) {
            time = "unknown_time";
        } else {
            try {
                time = Time.currentTime().toString();
            } catch (IllegalStateException e) {
                time = LocalTime.ofInstant(Time.now(), ZoneOffset.UTC) + " UTC";
            }
        }

        return format("[%s] %s", time, base.toSerializable(event));
    }
}
