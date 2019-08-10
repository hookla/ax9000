package com.ax9k.training;

import com.ax9k.training.writer.Writer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

class TrainingDataGenerator {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String NO_PROPERTY_ERROR = "object does not contain %s property. node: %s";
    private static final String INVALID_TIME_ERROR = "%s is not a valid LocalTime for property %s";
    private static final char OBJECT_START = '{';
    private static final Pattern OBJECT_END = Pattern.compile("^}[,]?[\\s]*$");
    private static final BiFunction<String, Object, Object> TIME_TO_STRING = (__, value) -> String.valueOf(value);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final BufferedReader input;
    private final Map<String, PropertyValidator> validators;
    private final Map<Target, TargetFilter> targetFilters;
    private final String eventTimeProperty;
    private final boolean filterInvalidRows;
    private final List<FutureDataBuffer> buffers;

    private boolean prettyPrinted;

    private Map<Target, Integer> filteredCount;
    private int invalidCount, processedCount;

    private Writer currentWriter;

    TrainingDataGenerator(BufferedReader input,
                          String eventTimeProperty,
                          Collection<String> properties,
                          Collection<String> validation,
                          String targetFilters,
                          String periods,
                          double tolerance,
                          boolean filterInvalidRows
    ) {
        this.input = requireNonNull(input, "TrainingDataGenerator input");
        this.eventTimeProperty = requireNonNull(eventTimeProperty, "TrainingDataGenerator eventTimeProperty");
        this.filterInvalidRows = filterInvalidRows;
        this.targetFilters = TargetFilter.parseAll(targetFilters);
        this.validators = requireNonNull(validation, "TrainingDataGenerator validation").stream()
                                                                                        .map(PropertyValidator::parse)
                                                                                        .collect(Collectors
                                                                                                         .toMap(
                                                                                                                 PropertyValidator::getIdentifier,
                                                                                                                 validator -> validator
                                                                                                         ));
        requireNonNull(properties, "TrainingDataGenerator properties");

        LOGGER.info("Validators: {}", validators);
        LOGGER.info("Properties: {} ", properties);
        LOGGER.info("Event Time Property: {} ", eventTimeProperty);
        LOGGER.info("Periods: {} ", periods);
        LOGGER.info("Tolerance: {} ", tolerance);
        LOGGER.info("Filters: {}", this.targetFilters);

        buffers = Stream.of(periods.split(","))
                        .map(String::trim)
                        .sorted()
                        .mapToInt(Integer::valueOf)
                        .mapToObj(Duration::ofMinutes)
                        .map((period) -> new FutureDataBuffer(properties, tolerance, period, eventTimeProperty))
                        .collect(Collectors.toList());
    }

    Schema generate(Writer output) throws IOException {
        requireNonNull(output, "generate output");

        currentWriter = output;

        LOGGER.info("Generating training data ...");

        filteredCount = new EnumMap<>(Target.class);
        processedCount = invalidCount = 0;

        skipDateDeclaration(input, output);

        Schema schema = new Schema();
        currentWriter.writeFormatToSchema(schema);

        boolean schemaInitialised = false;
        Map<String, Object> current;
        while ((current = readObject(input)) != null) {
            if (!schemaInitialised) {
                initialiseSchema(schema, current);
                schemaInitialised = true;
            }
            shiftValues(current);
        }

        LOGGER.info("Done!");
        LOGGER.info("Lines processed: {}. Invalid lines: {}", processedCount, invalidCount);

        LOGGER.info("Changes:");
        for (FutureDataBuffer buffer : buffers) {
            LOGGER.info(buffer);
            buffer.clear();
        }

        LOGGER.info("Filtered targets: {}", filteredCount);

        currentWriter = null;

        return schema;
    }

    private Schema initialiseSchema(Schema schema, Map<String, Object> exampleObject) {
        for (Map.Entry<String, Object> property : exampleObject.entrySet()) {
            schema.addAttribute(property.getKey(), property.getValue().getClass());
        }

        schema.excludeAttributeFromTraining(eventTimeProperty);
        schema.excludeAttributeFromTraining("filler");

        for (FutureDataBuffer buffer : buffers) {
            buffer.writeTargetToSchema(schema);
        }

        return schema;
    }

    private void skipDateDeclaration(BufferedReader input, Writer output) throws IOException {
        input.mark(2);
        int character = input.read();
        input.reset();

        if (character != OBJECT_START) {
            String date = input.readLine();
            if (date != null) {
                output.writeDate(date);
            } else {
                LOGGER.warn("Training source file is empty or has already been read");
            }
        }
    }

    private Map<String, Object> readObject(BufferedReader reader) throws IOException {
        if (prettyPrinted) {
            return readPrettyPrinted(reader);
        } else {
            try {
                return readSingleLine(reader);
            } catch (IOException expected) {
                prettyPrinted = true;
                return readPrettyPrintedWithStart(reader);
            }
        }
    }

    private Map<String, Object> readSingleLine(BufferedReader reader) throws IOException {
        String json = reader.readLine();
        return json != null ? toMap(JSON_MAPPER.readTree(json)) : null;
    }

    private Map<String, Object> readPrettyPrinted(BufferedReader reader) throws IOException {
        return readPrettyPrinted(reader, new StringBuilder(100));
    }

    private Map<String, Object> readPrettyPrintedWithStart(BufferedReader reader) throws IOException {
        return readPrettyPrinted(reader, new StringBuilder(100).append(OBJECT_START));
    }

    private Map<String, Object> readPrettyPrinted(BufferedReader reader, StringBuilder jsonObject) throws IOException {
        String currentLine;
        while ((currentLine = reader.readLine()) != null) {
            jsonObject.append(currentLine);
            if (OBJECT_END.matcher(currentLine).matches()) {
                return toMap(JSON_MAPPER.readTree(jsonObject.toString()));
            }
        }
        return null;
    }

    private Map<String, Object> toMap(JsonNode node) {
        Map<String, Object> result = new LinkedHashMap<>();

        ensureContainsRequiredProperties(node);
        LocalTime eventTime = getTimeProperty(eventTimeProperty, node);
        result.put(eventTimeProperty, eventTime);

        for (Map.Entry<String, JsonNode> field : toIterable(node.fields())) {
            String name = field.getKey();
            try {
                result.put(name, unwrap(field.getValue()));
            } catch (NumberFormatException ignored) {
            }
        }

        return result;
    }

    private Object unwrap(JsonNode node) {
        if (node.isNumber()) {
            return node.asDouble();
        }

        if (node.isBoolean()) {
            return node.asBoolean();
        }

        return Double.parseDouble(node.asText());
    }

    private <T> Iterable<T> toIterable(Iterator<T> iterator) {
        return () -> iterator;
    }

    private LocalTime getTimeProperty(String property, JsonNode node) {
        try {
            return LocalTime.parse(node.get(property).asText());
        } catch (NullPointerException noProperty) {
            throw new IllegalArgumentException(format(NO_PROPERTY_ERROR, property, node.toString()));
        } catch (DateTimeParseException invalidTime) {
            throw new IllegalArgumentException(format(INVALID_TIME_ERROR, node.get(property), property));
        }
    }

    private void ensureContainsRequiredProperties(JsonNode node) {
        for (String property : validators.keySet()) {
            ensurePropertyExists(property, node);
        }
    }

    private void ensurePropertyExists(String property, JsonNode node) {
        if (!node.has(property)) {
            throw new IllegalArgumentException(format(NO_PROPERTY_ERROR, property, node.toString()));
        }
    }

    private void shiftValues(Map<String, Object> current) {

        if (!isValid(current) && filterInvalidRows) {
            return;
        }

        buffers.get(0).shiftIn(current);
        for (int i = 0; i < buffers.size(); i++) {
            FutureDataBuffer currentBuffer = buffers.get(i);

            if (i == buffers.size() - 1) {
                currentBuffer.fillAndShift(current, this::writeObject);
            } else {
                FutureDataBuffer nextBuffer = buffers.get(i + 1);
                currentBuffer.fillAndShift(current, nextBuffer::shiftIn);
            }
        }
    }

    private void writeObject(Map<String, Object> object) {
        boolean passed = testTargetFilters(object);
        if (!passed) {
            return;
        }

        object.compute(eventTimeProperty, TIME_TO_STRING);
        try {
            currentWriter.writeObject(object);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean testTargetFilters(Map<String, Object> object) {
        for (var property : object.entrySet()) {
            if (property.getKey().contains("_TARGET")) {
                Target target = (Target) property.getValue();
                boolean passed = targetFilters.get(target).test();
                if (!passed) {
                    filteredCount.merge(target, 1, Integer::sum);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValid(Map<String, Object> current) {
        for (Map.Entry<String, Object> property : current.entrySet()) {
            PropertyValidator validator = validators.get(property.getKey());

            if (validator != null) {
                OptionalDouble validValue = validator.validate(property.getValue());

                if (!validValue.isPresent()) {
                    invalidCount++;
                    return false;
                }

                property.setValue(validValue.getAsDouble());
            }
        }

        processedCount++;
        return true;
    }
}
