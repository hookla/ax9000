package com.ax9k.algo.trading;

import com.ax9k.algo.Algo.Signal;
import com.fasterxml.jackson.annotation.JsonGetter;

import static com.ax9k.algo.Algo.Signal.NONE;
import static org.apache.commons.lang3.Validate.notNull;

public final class SignalContext {
    private final Signal previousEnter;
    private final Signal enter;
    private final Signal previousExit;
    private final Signal exit;

    public SignalContext(Signal enter, Signal exit) {
        this(NONE, enter, NONE, exit);
    }

    private SignalContext(Signal previousEnter,
                          Signal enter,
                          Signal previousExit,
                          Signal exit) {
        this.previousEnter = notNull(previousEnter);
        this.enter = notNull(enter);
        this.previousExit = notNull(previousExit);
        this.exit = notNull(exit);
    }

    public SignalContext update(Signal newEnter, Signal newExit) {
        return new SignalContext(enter, newEnter, exit, newExit);
    }

    public SignalContext updateEnter(Signal newEnter) {
        return new SignalContext(enter, newEnter, previousExit, exit);
    }

    public SignalContext updateExit(Signal newExit) {
        return new SignalContext(previousEnter, enter, exit, newExit);
    }

    public Signal getPreviousEnter() {
        return previousEnter;
    }

    public Signal getEnter() {
        return enter;
    }

    public Signal getPreviousExit() {
        return previousExit;
    }

    public Signal getExit() {
        return exit;
    }

    @JsonGetter("enterJustChanged")
    public boolean enterJustChanged() {
        return previousEnter != enter;
    }

    @JsonGetter("exitJustChanged")
    public boolean exitJustChanged() {
        return previousExit != exit;
    }
}
