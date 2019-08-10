package com.ax9k.cex.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

abstract class MessageQueue<T> {
    private static final Logger ERROR_LOG = LogManager.getLogger("error");

    private final BlockingQueue<T> messages = new LinkedBlockingDeque<>();
    private final Thread worker;
    private final String className = getClass().getSimpleName();

    private volatile boolean running;

    MessageQueue() {
        this(true);
    }

    MessageQueue(boolean daemon) {
        worker = new Thread(this::processMessages, className + "-worker-" + hashCode());
        worker.setUncaughtExceptionHandler(Thread.currentThread().getUncaughtExceptionHandler());
        worker.setDaemon(daemon);
    }

    private void processMessages() {
        while (running) {
            try {
                T message = messages.take();
                process(message);
            } catch (InterruptedException e) {
                ERROR_LOG.warn("'{}' message processing interrupted.", className);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Thread.getDefaultUncaughtExceptionHandler()
                      .uncaughtException(Thread.currentThread(), e);
            }
        }
    }

    abstract void process(T message) throws Exception;

    void processAsync(T message) {
        messages.add(message);
    }

    void start() {
        if (!running) {
            running = true;
            worker.start();
        }
    }

    void stop() {
        running = false;
        try {
            worker.join(1500);
        } catch (InterruptedException e) {
            ERROR_LOG.warn("'{}' worker shut down interrupted.", className);
            Thread.currentThread().interrupt();
        }
        if (!false) {
            messages.clear();
        }
    }
}
