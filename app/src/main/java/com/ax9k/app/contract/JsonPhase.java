package com.ax9k.app.contract;

import com.ax9k.core.marketmodel.Milestone;
import com.ax9k.core.marketmodel.Phase;
import com.ax9k.core.marketmodel.StandardPhase;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;
import java.util.Objects;

public final class JsonPhase implements Phase {
    private final StandardPhase delegate;

    @JsonCreator
    private JsonPhase(@JsonProperty(value = "start", required = true) JsonMilestone start,
                      @JsonProperty(value = "end", required = true) JsonMilestone end,
                      @JsonProperty(value = "name", required = true) String name,
                      @JsonProperty(value = "tradingSession", required = true) boolean tradingSession,
                      @JsonProperty(value = "marketIsOpen", required = true) boolean marketIsOpen,
                      @JsonProperty(value = "afterMarketClose", required = true) boolean afterMarketClose) {
        delegate = new StandardPhase(start, end, name, tradingSession, marketIsOpen, afterMarketClose);
    }

    @Override
    public boolean includes(LocalTime time) {
        return delegate.includes(time);
    }

    @Override
    public Milestone getStart() {
        return delegate.getStart();
    }

    @Override
    public Milestone getEnd() {
        return delegate.getEnd();
    }

    @Override
    public boolean isTradingSession() {
        return delegate.isTradingSession();
    }

    @Override
    public boolean isMarketOpen() {
        return delegate.isMarketOpen();
    }

    @Override
    public boolean isAfterMarketClose() {
        return delegate.isAfterMarketClose();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        JsonPhase jsonPhase = (JsonPhase) o;
        return Objects.equals(delegate, jsonPhase.delegate);
    }

    @Override
    public int hashCode() {
        return 31 + delegate.hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}