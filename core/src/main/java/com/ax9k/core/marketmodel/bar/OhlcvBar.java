package com.ax9k.core.marketmodel.bar;

import com.ax9k.core.event.Event;
import com.ax9k.core.event.EventType;
import com.ax9k.core.time.Time;
import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.commons.lang3.Validate.notNull;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NON_PRIVATE)
public final class OhlcvBar implements Event {
    public static final OhlcvBar EMPTY = new OhlcvBar(Instant.EPOCH, Instant.EPOCH, 0, 0, 0, 0, 0);

    private final Instant eventTimestamp, createdTimestamp;
    private final double open, high, low, close, volume;

    private OhlcvBar(Instant eventTimestamp, double open, double high, double low, double close, double volume) {
        this(eventTimestamp, Time.now(), open, high, low, close, volume);
    }

    private OhlcvBar(Instant eventTimestamp,
                     Instant createdTimestamp,
                     double open,
                     double high,
                     double low,
                     double close,
                     double volume) {
        this.eventTimestamp = notNull(eventTimestamp, "eventTimestamp");
        this.createdTimestamp = notNull(createdTimestamp, "createdTimestamp");
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public static OhlcvBar of(Instant timestamp, double open, double high, double low, double close, double volume) {
        return new OhlcvBar(timestamp, open, high, low, close, volume);
    }

    @JsonCreator
    private static OhlcvBar of(
            @JsonProperty(value = "localisedEventTimestamp", required = true) String localisedTimestamp,
            @JsonProperty(value = "localisedCreatedTimestamp", required = true) String localisedCreatedTimestamp,
            @JsonProperty(value = "open", required = true) double open,
            @JsonProperty(value = "high", required = true) double high,
            @JsonProperty(value = "low", required = true) double low,
            @JsonProperty(value = "close", required = true) double close,
            @JsonProperty(value = "volume", required = true) double volume) {
        Instant eventTimestamp = Time.internationalise(LocalDateTime.parse(localisedTimestamp));
        Instant createdTimestamp = Time.internationalise(LocalDateTime.parse(localisedCreatedTimestamp));
        return new OhlcvBar(eventTimestamp, createdTimestamp, open, high, low, close, volume);
    }

    public OhlcvBar aggregate(OhlcvBar that) {
        if (this == EMPTY) {
            return that;
        }

        return new OhlcvBar(that.eventTimestamp,
                            this.open,
                            max(this.high, that.high),
                            min(this.low, that.low),
                            that.close,
                            this.volume + that.volume);
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public double getVolume() {
        return volume;
    }

    LocalDateTime getLocalisedCreatedTimestamp() {
        return Time.localise(createdTimestamp);
    }

    Duration getLag() {
        return Duration.between(eventTimestamp, createdTimestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        OhlcvBar ohlcvBar = (OhlcvBar) o;
        return Double.compare(ohlcvBar.open, open) == 0 &&
               Double.compare(ohlcvBar.high, high) == 0 &&
               Double.compare(ohlcvBar.low, low) == 0 &&
               Double.compare(ohlcvBar.close, close) == 0 &&
               Double.compare(ohlcvBar.volume, volume) == 0 &&
               Objects.equals(eventTimestamp, ohlcvBar.eventTimestamp) &&
               Objects.equals(createdTimestamp, ohlcvBar.createdTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventTimestamp, createdTimestamp, open, high, low, close, volume);
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }

    @Override
    @JsonIgnore
    public Instant getTimestamp() {
        return eventTimestamp;
    }

    @Override
    @JsonIgnore
    public EventType getType() {
        return EventType.OHLCV_BAR;
    }
}
