package com.ax9k.core.history;

import java.util.Iterator;
import java.util.NoSuchElementException;

class EmptyIterator<T> implements Iterator<T> {
    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public T next() {
        throw new NoSuchElementException("no elements");
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("cannot modify source data");
    }
}
