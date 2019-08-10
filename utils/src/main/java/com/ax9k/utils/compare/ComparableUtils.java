package com.ax9k.utils.compare;

import java.util.Comparator;

import static java.util.Comparator.naturalOrder;

@SuppressWarnings({ "unused", "WeakerAccess" })
public final class ComparableUtils {
    private ComparableUtils() {
        throw new AssertionError("ComparableUtils is not instantiable");
    }

    public static <T extends Comparable<T>> boolean equal(T one, T two) {
        return equal(one, two, naturalOrder());
    }

    public static <T> boolean equal(T one, T two, Comparator<T> comparator) {
        return comparator.compare(one, two) == 0;
    }

    public static <T extends Comparable<T>> boolean greaterThan(T one, T two) {
        return greaterThan(one, two, naturalOrder());
    }

    public static <T> boolean greaterThan(T one, T two, Comparator<T> comparator) {
        return comparator.compare(one, two) > 0;
    }

    public static <T extends Comparable<T>> boolean greaterThanOrEqual(T one, T two) {
        return greaterThanOrEqual(one, two, naturalOrder());
    }

    public static <T> boolean greaterThanOrEqual(T one, T two, Comparator<T> comparator) {
        return comparator.compare(one, two) >= 0;
    }

    public static <T extends Comparable<T>> boolean lessThan(T one, T two) {
        return lessThan(one, two, naturalOrder());
    }

    public static <T> boolean lessThan(T one, T two, Comparator<T> comparator) {
        return comparator.compare(one, two) < 0;
    }

    public static <T extends Comparable<T>> boolean lessThanOrEqual(T one, T two) {
        return lessThanOrEqual(one, two, naturalOrder());
    }

    public static <T> boolean lessThanOrEqual(T one, T two, Comparator<T> comparator) {
        return comparator.compare(one, two) <= 0;
    }
}
