package com.ax9k.utils.math;

public final class MathUtils {
    private MathUtils() {
        throw new AssertionError("MathUtils is not instantiable");
    }

    public static double round(double value, int decimalPlaces) {
        double multiplier = Math.pow(10, decimalPlaces);

        return Math.round(value * multiplier) / multiplier;
    }
}
