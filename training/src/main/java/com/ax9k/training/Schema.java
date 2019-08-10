package com.ax9k.training;

import com.ax9k.utils.json.JsonUtils;
import com.fasterxml.jackson.annotation.JsonGetter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Map.entry;

public final class Schema {
    private static final String INVALID_TYPE_ERROR = "Invalid value type: %s. Valid types: %s";
    private static final String TYPE_NUMBER = "NUMERIC";
    private static final String TYPE_TARGET = "CATEGORICAL";
    private static final String TYPE_BOOLEAN = "BINARY";
    private static final String TYPE_STRING = "TEXT";

    private static final Map<Class<?>, String> TYPE_NAMES;

    static {
        TYPE_NAMES = Map.ofEntries(
                entry(Boolean.class, TYPE_BOOLEAN),
                entry(Double.class, TYPE_NUMBER),
                entry(Integer.class, TYPE_NUMBER),
                entry(String.class, TYPE_STRING),
                entry(LocalTime.class, TYPE_STRING)
        );
    }

    private String dataFormat;
    private boolean containsHeader;

    private Set<Attribute> attributes;
    private Set<String> excludedAttributeNames;
    private String targetAttributeName;

    Schema() {
        attributes = new LinkedHashSet<>();
        excludedAttributeNames = new HashSet<>();
    }

    public void setFormat(String format, boolean containsHeader) {
        this.dataFormat = format;
        this.containsHeader = containsHeader;
    }

    public void addAttribute(String name, Class<?> type) {
        String typeName = requireValidType(type);

        attributes.add(new Attribute(name, typeName));
    }

    private String requireValidType(Class<?> type) {
        String typeName = TYPE_NAMES.get(type);

        if (typeName == null) {
            throw new IllegalArgumentException(format(INVALID_TYPE_ERROR, type, TYPE_NAMES.keySet()));
        }

        return typeName;
    }

    public void addTargetAttribute(String name) {
        attributes.add(new Attribute(name, TYPE_TARGET));
        targetAttributeName = name;
    }

    public void excludeAttributeFromTraining(String name) {
        excludedAttributeNames.add(name);
    }

    Path save(Path directory, String fileName) throws IOException {
        Path output = resolveOutputFile(directory, fileName);
        String json = createJson();
        writeToFile(output, json);
        return output;
    }

    private Path resolveOutputFile(Path directory, String fileName) throws IOException {
        Path output = directory.resolve(fileName.concat(".schema"));
        Files.deleteIfExists(output);
        Files.createFile(output);
        return output;
    }

    private String createJson() {
        Map<String, Object> object = new LinkedHashMap<>();

        object.put("version", "0.1");
        object.put("rowId", null);
        object.put("rowWeight", null);
        object.put("targetAttributeName", targetAttributeName);
        object.put("dataFormat", dataFormat);
        object.put("dataFileContainsHeader", containsHeader);
        object.put("attributes", attributes);
        object.put("excludedAttributeNames", excludedAttributeNames);

        return JsonUtils.toPrettyJsonString(object);
    }

    private void writeToFile(Path file, String content) throws IOException {
        try (OutputStream output = Files.newOutputStream(file)) {
            ByteArrayInputStream bytes = new ByteArrayInputStream(content.getBytes());
            bytes.transferTo(output);
            output.flush();
        }
    }

    private static final class Attribute {
        private final String name, type;

        private Attribute(String name, String type) {
            this.name = name;
            this.type = type;
        }

        @SuppressWarnings("unused")
        @JsonGetter("attributeName")
        private String name() {
            return name;
        }

        @SuppressWarnings("unused")
        @JsonGetter("attributeType")
        private String type() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) { return true; }
            if (other == null || getClass() != other.getClass()) { return false; }
            Attribute attribute = (Attribute) other;
            return Objects.equals(name, attribute.name) &&
                   Objects.equals(type, attribute.type);
        }
    }
}
