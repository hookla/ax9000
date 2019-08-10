package com.ax9k.cex.client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;

public class JsonMapper {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectWriter JSON_WRITER = JSON_MAPPER.writer();

    public static JsonNode read(String json) {
        try {
            return JSON_MAPPER.readTree(json);
        } catch (JsonParseException e) {
            throw new UncheckedJsonProcessingException("Invalid JSON object: " + json, e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String write(Object object) throws UncheckedJsonProcessingException {
        try {
            return JSON_WRITER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new UncheckedJsonProcessingException("Could not write object to JSON: " + object, e);
        }
    }

    public static ObjectNode createObjectNode() {
        return JSON_MAPPER.createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return JSON_MAPPER.createArrayNode();
    }

    public static final class UncheckedJsonProcessingException extends RuntimeException {
        UncheckedJsonProcessingException(String message, JsonProcessingException cause) {
            super(message, cause);
        }
    }
}
