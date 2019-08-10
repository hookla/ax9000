package com.ax9k.training;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

class FutureDataBuffer {
    private static final int INITIAL_CAPACITY = 100;

    private final Deque<Map<String, Object>> buffer;
    private final Collection<String> featureProperties;
    private final Duration period;
    private final String futureSuffix;
    private final String targetSuffix;
    private final Map<Target, Integer> targetCount;
    private final double tolerance;
    private final String eventTimeProperty;

    FutureDataBuffer(Collection<String> featureProperties,
                     double tolerance,
                     Duration period,
                     String eventTimeProperty) {
        this.featureProperties = featureProperties;
        this.tolerance = tolerance;
        this.period = period;
        this.eventTimeProperty = eventTimeProperty;
        buffer = new ArrayDeque<>(INITIAL_CAPACITY);
        futureSuffix = "_in" + period.toMinutes() + "m";
        targetSuffix = futureSuffix + "_TARGET";
        targetCount = new EnumMap<>(Target.class);
    }

    void clear() {
        buffer.clear();
        targetCount.clear();
    }

    void shiftIn(Map<String, Object> element) {
        buffer.addLast(element);
    }

    void fillAndShift(Map<String, Object> element, Consumer<Map<String, Object>> next) {
        while (periodElapsedBetween(buffer.peekFirst(), element)) {
            Map<String, Object> earliest = buffer.removeFirst();
            copyFutureData(element, earliest);
            next.accept(earliest);
        }
    }

    private boolean periodElapsedBetween(Map<String, Object> value1, Map<String, Object> value2) {
        if (value1 == null || value2 == null) {
            return false;
        }

        LocalTime time1 = (LocalTime) value1.get(eventTimeProperty);
        LocalTime time2 = (LocalTime) value2.get(eventTimeProperty);

        return Duration.between(time1, time2).abs().compareTo(period) >= 0;
    }

    private void copyFutureData(Map<String, Object> future, Map<String, Object> past) {
        for (String property : featureProperties) {
            copyProperty(future, past, property);
        }
    }

    private void copyProperty(Map<String, Object> future,
                              Map<String, Object> past,
                              String property) {
        ensurePropertiesPresent(past, property, "past");
        ensurePropertiesPresent(future, property, "future");

        double value = (double) past.get(property);
        double futureValue = (double) future.get(property);

        Target change = calculateChange(value, futureValue);
        past.put(resolveFutureProperty(property), futureValue);
        past.put(resolveTargetProperty(property), change);
        targetCount.merge(change, 1, Integer::sum);
    }

    private static void ensurePropertiesPresent(Map<String, Object> object, String property, String time) {
        if (!object.containsKey(property)) {
            throw new IllegalStateException(
                    String.format("No %s value for %s property. Object: %s", time, property, object)
            );
        }
    }

    private Target calculateChange(double value, double futureValue) {
        double absoluteDifference = futureValue - value;
        double percentageDifference = absoluteDifference / value;

        if (percentageDifference >= tolerance) {
            return Target.UP;
        } else if (percentageDifference <= -tolerance) {
            return Target.DOWN;
        } else {
            return Target.NO_CHANGE;
        }
    }

    private String resolveFutureProperty(String property) {
        return property.concat(futureSuffix);
    }

    private String resolveTargetProperty(String property) {
        return property.concat(targetSuffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buffer, featureProperties, period);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        FutureDataBuffer that = (FutureDataBuffer) o;
        return Objects.equals(buffer, that.buffer) &&
               Objects.equals(featureProperties, that.featureProperties) &&
               Objects.equals(period, that.period);
    }

    @Override
    public String toString() {
        return String.format("Period: %s, Target Count: %s", period, targetCount);
    }

    void writeTargetToSchema(Schema schema) {
        for (String property : featureProperties) {
            String futureProperty = resolveFutureProperty(property);
            schema.addAttribute(futureProperty, Double.class);
            schema.excludeAttributeFromTraining(futureProperty);

            schema.addTargetAttribute(resolveTargetProperty(property));
        }
    }
}
