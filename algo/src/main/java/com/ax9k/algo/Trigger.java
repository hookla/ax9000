package com.ax9k.algo;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class Trigger {
    private static final Runnable NO_EVENT = null;
    private static final Consumer<Runnable> RUN = Runnable::run;

    private final Optional<Runnable> onTrigger;
    private final AtomicBoolean triggered = new AtomicBoolean();

    public Trigger() {
        this(NO_EVENT);
    }

    public Trigger(Runnable onTrigger) {
        this.onTrigger = Optional.ofNullable(onTrigger);
    }

    public boolean isTriggered() {
        return triggered.get();
    }

    public boolean trigger() {
        boolean changed = triggered.compareAndSet(false, true);

        if (changed) {
            onTrigger.ifPresent(RUN);
        }
        return changed;
    }

    public boolean reset() {
        return triggered.compareAndSet(true, false);
    }

    @Override
    public int hashCode() {
        return Objects.hash(onTrigger, triggered);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        Trigger that = (Trigger) other;
        return triggered.equals(that.triggered) &&
               onTrigger.equals(that.onTrigger);
    }
}
