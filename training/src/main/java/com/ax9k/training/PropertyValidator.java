package com.ax9k.training;

import com.ax9k.utils.json.JsonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

import static java.lang.Double.compare;

final class PropertyValidator {
    private static final String INVALID_FORMAT_ERROR =
            "line is not in the format of {property},{minimumValue},{maximumValue}: ";
    private static final Pattern SEPARATOR = Pattern.compile(",");
    private static final OptionalDouble INVALID = OptionalDouble.empty();
    private static final double INVALID_VALUE = Double.MIN_VALUE;
    private static final int NUM_VALUES = 3, INDEX_IDENTIFIER = 0, INDEX_MIN = 1, INDEX_MAX = 2;

    private final String identifier;
    private final OptionalDouble minimumValue, maximumValue;

    private PropertyValidator(String identifier, OptionalDouble minimumValue, OptionalDouble maximumValue) {
        this.identifier = identifier;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
    }

    static PropertyValidator parse(String line) {
        line = line.trim();
        String[] parts = SEPARATOR.split(line, -1);
        if (parts.length != NUM_VALUES) {
            throw new IllegalArgumentException(INVALID_FORMAT_ERROR + line);
        }

        String id = ensureNotEmpty(parts[INDEX_IDENTIFIER]);
        OptionalDouble min = extractValue(parts[INDEX_MIN], "minimumValue");
        OptionalDouble max = extractValue(parts[INDEX_MAX], "maximumValue");

        return new PropertyValidator(id, min, max);
    }

    private static String ensureNotEmpty(String text) {
        text = text.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("property identifier is empty");
        }
        return text;
    }

    private static OptionalDouble extractValue(String text, String errorIdentifier) {
        text = text.trim();
        if (text.isEmpty()) {
            return OptionalDouble.empty();
        } else {
            try {
                return OptionalDouble.of(Double.parseDouble(text));
            } catch (NumberFormatException e) {
                throw new NumberFormatException(errorIdentifier);
            }
        }
    }

    OptionalDouble validate(Object value) {
        if (!(value instanceof Double)) {
            return INVALID;
        }
        return validate((double) value);
    }

    private OptionalDouble validate(double value) {
        if (compare(value, INVALID_VALUE) == 0) {
            return INVALID;
        } else if (minimumValue.isPresent() && compare(value, minimumValue.getAsDouble()) < 0) {
            return minimumValue;
        } else if (maximumValue.isPresent() && compare(value, maximumValue.getAsDouble()) > 0) {
            return maximumValue;
        }

        return OptionalDouble.of(value);
    }

    String getIdentifier() {
        return identifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, minimumValue, maximumValue);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        PropertyValidator that = (PropertyValidator) other;
        return Objects.equals(identifier, that.identifier) &&
               Objects.equals(minimumValue, that.minimumValue) &&
               Objects.equals(maximumValue, that.maximumValue);
    }

    @Override
    public String toString() {
        var minMaxValues = new HashMap<String, Double>(2);
        minimumValue.ifPresent(min -> minMaxValues.put("min", min));
        maximumValue.ifPresent(max -> minMaxValues.put("max", max));

        var resultHierarchy = Map.of(identifier, minMaxValues);
        return JsonUtils.toJsonString(resultHierarchy);
    }
}
