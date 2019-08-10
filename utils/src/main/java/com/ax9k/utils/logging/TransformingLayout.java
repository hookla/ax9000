package com.ax9k.utils.logging;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;

public class TransformingLayout extends AbstractStringLayout {
    private final Layout<String> base;
    private final UnaryOperator<String> transformer;

    public TransformingLayout(Layout<String> base, UnaryOperator<String> transformer) {
        super(StandardCharsets.UTF_8);
        this.base = base;
        this.transformer = transformer;
    }

    @Override
    public String toSerializable(LogEvent event) {
        return transformer.apply(base.toSerializable(event));
    }
}
