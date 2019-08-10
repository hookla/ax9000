package com.ax9k.cex.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.regex.Pattern;

import static com.ax9k.cex.client.JsonMapper.createArrayNode;
import static org.apache.commons.lang3.Validate.notBlank;

public final class Pair {
    private static final Pattern VALID_PAIR = Pattern.compile("[A-Z]{3}:[A-Z]{3}");
    private static final String SYMBOL_SEPARATOR = ":";
    private static final int CODE_LENGTH = 3;

    private final String symbol1, symbol2;

    private String toString;

    private Pair(String symbol1, String symbol2) {
        this.symbol1 = notBlank(symbol1);
        this.symbol2 = notBlank(symbol2);
    }

    public static Pair fromString(String pair) {
        Validate.notNull(pair, "pair string is null");
        Validate.isTrue(VALID_PAIR.matcher(pair).matches(),
                        "pair string is not in the format of '%s'", VALID_PAIR.toString());
        return fromTrustedString(pair);
    }

    private static Pair fromTrustedString(String pair) {
        return new Pair(pair.substring(0, CODE_LENGTH), pair.substring(CODE_LENGTH + 1));
    }

    public static Pair fromJsonNode(JsonNode node) {
        Validate.notNull(node);
        if (node.isArray()) {
            return fromJsonArray((ArrayNode) node);
        }
        return fromTrustedString(node.asText());
    }

    private static Pair fromJsonArray(ArrayNode array) {
        return new Pair(array.get(0).asText(), array.get(1).asText());
    }

    public ArrayNode toJsonArray() {
        return createArrayNode().add(symbol1)
                                .add(symbol2);
    }

    public String getSymbol1() {
        return symbol1;
    }

    public String getSymbol2() {
        return symbol2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        Pair pair = (Pair) o;
        return Objects.equals(symbol1, pair.symbol1) &&
               Objects.equals(symbol2, pair.symbol2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol1, symbol2);
    }

    @Override
    public String toString() {
        if (toString == null) {
            toString = symbol1 + SYMBOL_SEPARATOR + symbol2;
        }
        return toString;
    }
}
