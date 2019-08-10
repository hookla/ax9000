package com.ax9k.util;

public interface ContractSpecification<T> {
    T getValue();

    T getEqualValue();

    T getUnequalValue();
}
