package com.ax9k.training;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

class TargetFilter {
    private static final TargetFilter GUARANTEED = new TargetFilter(1d);
    private static final Random RNG = new Random();

    private final double passChance;
    private final boolean alwaysPass;

    private TargetFilter(double passChance) {
        this.passChance = ensureValidPercentage(passChance);
        alwaysPass = isOne(passChance);
    }

    private double ensureValidPercentage(double chance) {
        if (greaterThanZero(chance)) {
            throw new IllegalArgumentException("chance cannot be greater than 100%: " + chance);
        } else if (lessThanZero(chance)) {
            throw new IllegalArgumentException("chance cannot be negative: " + chance);
        }
        return chance;
    }

    private static boolean greaterThanZero(double chance) {
        return Double.compare(chance, 1d) > 0;
    }

    private static boolean lessThanZero(double chance) {
        return Double.compare(chance, 0d) < 0;
    }

    private static boolean isOne(double passChance) {
        return Double.compare(passChance, 1d) == 0;
    }

    static Map<Target, TargetFilter> parseAll(String line) {
        String[] chances = line.split(",", -1);
        if (chances.length != 3) {
            throw new IllegalArgumentException("invalid number of targets: " + line);
        }

        Map<Target, TargetFilter> result = new EnumMap<>(Target.class);

        addFilter(Target.DOWN, chances[0], result);
        addFilter(Target.NO_CHANGE, chances[1], result);
        addFilter(Target.UP, chances[2], result);

        return result;
    }

    private static void addFilter(Target target, String value, Map<Target, TargetFilter> output) {
        if (value.isEmpty()) {
            output.put(target, GUARANTEED);
            return;
        }

        double parsedValue;
        try {
            parsedValue = Double.parseDouble(value);
        } catch (NumberFormatException invalidDecimal) {
            throw new IllegalArgumentException("invalid chance value: " + value);
        }

        output.put(target, new TargetFilter(parsedValue));
    }

    boolean test() {
        return alwaysPass || Double.compare(RNG.nextDouble(), passChance) <= 0;
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(passChance);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        TargetFilter that = (TargetFilter) other;
        return Double.compare(passChance, that.passChance) == 0;
    }

    @Override
    public String toString() {
        return (passChance * 100) + "%";
    }
}
