package com.ax9k.algo;

import com.ax9k.core.event.Event;
import com.ax9k.core.time.Time;
import com.ax9k.utils.json.JsonUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

import static java.lang.String.format;
import static java.util.Map.entry;

public class PeriodicFeatureResult implements Event {
    private static final String FEATURE_NOT_PRESENT_ERROR = "Feature '%s' not recorded in: %s";
    private static final Map<Class, Class> BOXED_TYPES;

    static {
        BOXED_TYPES = Map.ofEntries(entry(boolean.class, Boolean.class),
                                    entry(byte.class, Byte.class),
                                    entry(short.class, Short.class),
                                    entry(char.class, Character.class),
                                    entry(int.class, Integer.class),
                                    entry(long.class, Long.class),
                                    entry(float.class, Float.class),
                                    entry(double.class, Double.class));
    }

    private final Map<String, Object> features;
    private final Instant start, end;

    PeriodicFeatureResult(Instant start, Instant end) {
        this.start = start;
        this.end = end;
        this.features = createInitialMap();
    }

    private Map<String, Object> createInitialMap() {
        LocalTime startTime = Time.localiseTime(start).withNano(0);
        LocalTime endTime = Time.localiseTime(end).withNano(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("periodStart", startTime.toString());
        result.put("periodEnd", endTime.toString());
        return result;
    }

    public void record(String name, Object value) {
        features.put(name, value);
    }

    void recordAll(Map<String, Object> values) {
        this.features.putAll(values);
    }

    public OptionalDouble getNonFiller(String feature) {
        return !isFiller() ? OptionalDouble.of(get(feature)) : OptionalDouble.empty();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isFiller() {
        Object filler = features.get("filler");

        if (filler instanceof Boolean) {
            return (Boolean) filler;
        }
        return false;
    }

    public double get(String feature) {
        return get(feature, Double.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String feature, Class<T> type) {
        Object result = requireRecordedFeature(feature);

        if (type.isPrimitive()) {
            type = BOXED_TYPES.get(type);
        }

        return type.cast(result);
    }

    private Object requireRecordedFeature(String feature) {
        Objects.requireNonNull(feature, "feature");
        Object result = features.get(feature);

        if (result == null) {
            throw new IllegalStateException(format(FEATURE_NOT_PRESENT_ERROR, feature, features));
        }

        return result;
    }

    public <T> Optional<T> getNonFiller(String feature, Class<T> type) {
        return !isFiller() ? Optional.of(get(feature, type)) : Optional.empty();
    }

    @Override
    public int hashCode() {
        return Objects.hash(features, start, end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        PeriodicFeatureResult that = (PeriodicFeatureResult) o;
        return Objects.equals(start, that.start) &&
               Objects.equals(end, that.end) &&
               Objects.equals(features, that.features);
    }

    @Override
    public String toString() {
        return JsonUtils.toPrettyJsonString(this);
    }

    public Map<String, Object> getFeatures() {
        return features;
    }

    @Override
    public LocalDateTime getLocalisedTimestamp() {
        return Time.localise(getTimestamp());
    }

    @Override
    public Instant getTimestamp() {
        return end;
    }
}
