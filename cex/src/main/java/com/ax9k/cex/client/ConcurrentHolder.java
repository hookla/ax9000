package com.ax9k.cex.client;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class ConcurrentHolder<T> {
    private final Lock lock = new ReentrantLock();
    private final Condition holding = lock.newCondition();

    private volatile T held;

    public T await() throws InterruptedException {
        lock.lock();
        held = null;
        try {
            while (held == null) {
                holding.await();
            }
        } finally {
            lock.unlock();
        }
        return held;
    }

    public Optional<T> await(Duration timeout) throws InterruptedException {
        lock.lock();
        held = null;
        try {
            holding.awaitNanos(timeout.toNanos());
        } finally {
            lock.unlock();
        }
        return Optional.ofNullable(held);
    }

    public void hold(T toHold) {
        lock.lock();
        try {
            held = toHold;
            holding.signal();
        } finally {
            lock.unlock();
        }
    }
}
