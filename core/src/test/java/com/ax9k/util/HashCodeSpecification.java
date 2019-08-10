package com.ax9k.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public interface HashCodeSpecification<T> extends ContractSpecification<T> {
    @Test
    default void shouldReturnConsistentHashCodes() {
        T value = getValue();
        assertHashCodeEquals(value, value);
    }

    private static void assertHashCodeEquals(Object expected, Object actual) {
        assertEquals(expected.hashCode(), actual.hashCode());
    }

    @Test
    default void shouldReturnSameHashCodeForEqualObjects() {
        T value = getValue();
        T equalValue = getEqualValue();
        assertEquals(value, equalValue);
        assertHashCodeEquals(value, equalValue);
    }

    @Test
    default void shouldReturnDifferentHashCodeForUnequalObjects() {
        T value = getValue();
        T unequalValue = getUnequalValue();
        assertNotEquals(value, unequalValue);
        assertHashCodeNotEquals(value, unequalValue);
    }

    private void assertHashCodeNotEquals(Object expected, Object actual) {
        assertNotEquals(expected.hashCode(), actual.hashCode());
    }
}
