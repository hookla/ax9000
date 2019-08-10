package com.ax9k.cex.client;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

import static org.apache.commons.lang3.Validate.notBlank;

public final class MessageType {
    private final String name;

    private MessageType(String name) {
        this.name = notBlank(name);
    }

    public static MessageType of(String name) {
        return new MessageType(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        MessageType that = (MessageType) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    @JsonValue
    public String toString() {
        return getName();
    }

    String getName() {
        return name;
    }
}
