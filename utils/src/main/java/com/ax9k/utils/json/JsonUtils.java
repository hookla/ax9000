package com.ax9k.utils.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.regex.Pattern;

import static java.lang.String.format;

public final class JsonUtils {
    private static final ObjectMapper JSON_MAPPER;
    private static final ObjectWriter JSON_WRITER;
    private static final ObjectWriter JSON_PRETTY_WRITER;

    private static final Pattern OBJECT_END = Pattern.compile("^}[,]?[\\s]*$");
    private static final String OBJECT_START = "{";

    static {
        JSON_MAPPER = new ObjectMapper();

        /*
        FIXME can't yet find a better way to do this.
        For these types, there's no need to write each property individually,
        a user-friendly textual representation is enough.
        */
        JSON_MAPPER.addMixIn(Instant.class, UsingToString.class);
        JSON_MAPPER.addMixIn(Duration.class, UsingToString.class);
        JSON_MAPPER.addMixIn(LocalDateTime.class, UsingToString.class);
        JSON_MAPPER.addMixIn(LocalDate.class, UsingToString.class);
        JSON_MAPPER.addMixIn(LocalTime.class, UsingToString.class);
        JSON_MAPPER.addMixIn(ZoneId.class, UsingToString.class);

        JSON_WRITER = JSON_MAPPER.writer();
        JSON_PRETTY_WRITER = JSON_MAPPER.writerWithDefaultPrettyPrinter();
    }

    private JsonUtils() {
        throw new AssertionError("JsonUtils is not instantiable");
    }

    public static String toPrettyJsonString(Object object) throws UncheckedJsonProcessingException {
        try {
            return JSON_PRETTY_WRITER.writeValueAsString(object);
        } catch (JsonProcessingException invalidObject) {
            throw new UncheckedJsonProcessingException(invalidObject);
        }
    }

    public static String toJsonString(Object object) throws UncheckedJsonProcessingException {
        try {
            return JSON_WRITER.writeValueAsString(object);
        } catch (JsonProcessingException invalidObject) {
            throw new UncheckedJsonProcessingException(invalidObject);
        }
    }

    public static <T> T readLines(BufferedReader lines, Class<T> type) {
        try {
            String line = lines.readLine();
            if (isPrettyPrinted(line)) {
                return readPrettyPrintedLines(lines, type);
            } else {
                return readString(line, type);
            }
        } catch (IOException unhandleable) {
            throw new UncheckedIOException("Error reading input", unhandleable);
        }
    }

    private static boolean isPrettyPrinted(String line) {
        return line.trim().equals(OBJECT_START);
    }

    private static <T> T readPrettyPrintedLines(BufferedReader reader, Class<T> type) throws IOException {
        StringBuilder jsonObject = new StringBuilder(100).append(OBJECT_START);
        String currentLine;
        while ((currentLine = reader.readLine()) != null) {
            currentLine = currentLine.trim();
            jsonObject.append(currentLine);
            if (OBJECT_END.matcher(currentLine).matches()) {
                return readString(jsonObject.toString(), type);
            }
        }

        if (!jsonObject.toString().trim().isEmpty()) {
            throw new UncheckedJsonProcessingException("Incomplete JSON object: " + jsonObject.toString());
        }

        return null;
    }

    public static <T> T readString(String line, java.lang.Class<T> type) {
        try {
            return JSON_MAPPER.readValue(line, type);
        } catch (JsonParseException invalidInput) {
            throw new UncheckedJsonProcessingException("Invalid input line: ".concat(line), invalidInput);
        } catch (JsonMappingException invalidOutputType) {
            throw new UncheckedJsonProcessingException(format("Invalid type '%s' for JSON input: %s", type, line),
                                                       invalidOutputType);
        } catch (IOException wontHappen) {
            throw new UncheckedIOException(wontHappen);
        }
    }

    public static JsonNode readString(String content) {
        try {
            return JSON_MAPPER.readTree(content);
        } catch (JsonParseException invalidInput) {
            throw new UncheckedJsonProcessingException("Invalid input: " + content, invalidInput);
        } catch (IOException wontHappen) {
            throw new UncheckedIOException(wontHappen);
        }
    }

    public static <T> T readFile(Path file, java.lang.Class<T> type) {
        try {
            return JSON_MAPPER.readValue(file.toFile(), type);
        } catch (JsonParseException invalidInput) {
            throw new UncheckedJsonProcessingException("Invalid input file: " + file.toAbsolutePath(), invalidInput);
        } catch (JsonMappingException invalidOutputType) {
            throw new UncheckedJsonProcessingException(format("Invalid type '%s' for JSON input: %s",
                                                              type,
                                                              file.toAbsolutePath()),
                                                       invalidOutputType);
        } catch (IOException wontHappen) {
            throw new UncheckedIOException(wontHappen);
        }
    }

    public static final class UncheckedJsonProcessingException extends RuntimeException {
        UncheckedJsonProcessingException(JsonProcessingException cause) {
            this("Invalid JSON object", cause);
        }

        UncheckedJsonProcessingException(String message, JsonProcessingException cause) {
            super(message, cause);
        }

        UncheckedJsonProcessingException(String message) {
            super(message);
        }
    }

    @JsonSerialize(using = ToStringSerializer.class)
    private static class UsingToString {
    }

    private static class ToStringSerializer extends StdSerializer<Object> {
        public ToStringSerializer() {
            super(Object.class);
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }
}

