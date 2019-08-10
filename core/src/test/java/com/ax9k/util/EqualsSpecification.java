package com.ax9k.util;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public interface EqualsSpecification<T> extends ContractSpecification<T> {
    @Test
    default void shouldReturnTrueForEqualValues() {
        assertEquals(getValue(), getEqualValue());
    }

    @Test
    default void shouldReturnFalseForUnequalValues() {
        assertNotEquals(getValue(), getUnequalValue());
    }

    @Test
    default void shouldBeReflexive() {
        assertEquals(getValue(), getValue());
    }

    @Test
    default void shouldBeSymmetric() {
        assertEquals(getValue(), getEqualValue());
        assertEquals(getEqualValue(), getValue());
    }

    @Test
    default void shouldBeTransitive() {
        assertEquals(getValue(), getEqualValue());
        assertEquals(getEqualValue(), getSecondEqualValue());
        assertEquals(getValue(), getSecondEqualValue());
    }

    T getSecondEqualValue();

    @Test
    default void shouldBeConsistent() {
        assertEquals(getValue(), getEqualValue());
        assertEquals(getValue(), getEqualValue());
    }

    @Test
    default void shouldNotBeEqualToNull() {
        assertNotEquals(null, getValue());
    }
}
