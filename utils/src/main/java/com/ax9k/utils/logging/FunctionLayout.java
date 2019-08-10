package com.ax9k.utils.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class FunctionLayout extends AbstractStringLayout {
    private final Function<LogEvent, String> layout;

    public FunctionLayout(Function<LogEvent, String> layout) {
        super(StandardCharsets.UTF_8);
        this.layout = layout;
    }

    @Override
    public String toSerializable(LogEvent event) {
        return layout.apply(event);
    }
}
