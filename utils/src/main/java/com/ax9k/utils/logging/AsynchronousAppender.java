package com.ax9k.utils.logging;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Plugin(name = "Asynchronous", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class AsynchronousAppender extends AbstractAppender {
    private final BlockingQueue<LogEvent> messages = new LinkedBlockingDeque<>();
    private final Appender base;
    private final Thread worker;

    private volatile boolean running;

    public AsynchronousAppender(Appender base) {
        this("Async_" + base.getName(), base);
    }

    private AsynchronousAppender(String name, Appender base) {
        super(name, null, base.getLayout());
        worker = new Thread(this::processMessages, name + "_Worker");
        worker.setDaemon(true);
        this.base = base;
    }

    private void processMessages() {
        while (running) {
            try {
                base.append(messages.take());
            } catch (InterruptedException e) {
                LOGGER.warn("Message processing interrupted.");
                Thread.currentThread().interrupt();
            }
        }
    }

    @PluginFactory
    public static AsynchronousAppender createAppender(@PluginAttribute("name") String name,
                                                      @PluginElement("base") Appender base) {
        return new AsynchronousAppender(name, base);
    }

    @Override
    public void append(LogEvent event) {
        messages.add(event);
    }

    @Override
    public void start() {
        super.start();
        running = true;
        worker.start();
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        running = false;
        try {
            worker.join(timeUnit.toMillis(timeout));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        messages.forEach(base::append);

        return super.stop(timeout, timeUnit);
    }
}
