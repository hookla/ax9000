package com.ax9k.core.marketmodel;

import com.ax9k.utils.json.JsonUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public final class HeartBeat {
    private static final Logger ERROR_LOG = LogManager.getLogger("error");
    private static final Logger LOGGER = LogManager.getRootLogger();
    private static final Runtime RUNTIME = Runtime.getRuntime();

    private final Lock lock = new ReentrantLock();
    private final Consumer<Instant> target;
    private final AtomicReference<Duration> period;
    private final AtomicReference<Instant> lastHeartBeat;

    private ScheduledExecutorService executor;
    private volatile boolean running;

    private long lastKnownTotalMemoryMb;

    HeartBeat(Duration period, Consumer<Instant> target) {
        this.period = new AtomicReference<>(period);
        this.target = target;
        lastHeartBeat = new AtomicReference<>();
        lastKnownTotalMemoryMb = toMegabytes(RUNTIME.totalMemory());
        LOGGER.info("Initial total memory: {}", lastKnownTotalMemoryMb);
    }

    private static long toMegabytes(long bytes) {
        return bytes / 1024 / 1024;
    }

    public void start(Instant periodStart) {
        lock.lock();
        try {
            lastHeartBeat.set(periodStart);
            scheduleBeat();
            running = true;
        } finally {
            lock.unlock();
        }
    }

    private void scheduleBeat() {
        long periodNanos = period.get().toNanos();

        ThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern("heartbeat-%s")
                .build();

        executor = Executors.newSingleThreadScheduledExecutor(factory);
        executor.scheduleAtFixedRate(this::beat,
                                     periodNanos,
                                     periodNanos,
                                     TimeUnit.NANOSECONDS);
    }

    private void beat() {
        checkMemoryUsage();
        Instant timestamp = lastHeartBeat.updateAndGet(this::advanceTimestamp);
        target.accept(timestamp);
    }

    private Instant advanceTimestamp(Instant timestamp) {
        return timestamp.plus(period.get());
    }

    private void checkMemoryUsage() {
        long totalMemoryMb = toMegabytes(RUNTIME.totalMemory());
        if (totalMemoryMb != lastKnownTotalMemoryMb) {
            ERROR_LOG.warn("Total memory changed from {} MiB to {} MiB.", lastKnownTotalMemoryMb, totalMemoryMb);
            lastKnownTotalMemoryMb = totalMemoryMb;
        }
    }

    public void stop() {
        lock.lock();
        try {
            executor.shutdownNow();
            running = false;
        } finally {
            lock.unlock();
        }
    }

    public void massageHeart(Instant eventTimestamp) {
        while (advanceTimestamp(lastHeartBeat.get()).isBefore(eventTimestamp)) {
            beat();
        }
    }

    public void setDuration(Duration period) {
        lock.lock();
        try {
            this.period.set(period);
            if (running) {
                restart();
            }
        } finally {
            lock.unlock();
        }
    }

    private void restart() {
        executor.shutdownNow();
        scheduleBeat();
    }

    public Duration getPeriod() {
        return period.get();
    }

    public Instant getLastHeartBeat() {
        return lastHeartBeat.get();
    }

    public void setLastHeartBeat(Instant lastHeartBeat) {
        this.lastHeartBeat.set(lastHeartBeat);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }
}
