package com.ax9k.utils.config;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Map.copyOf;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toMap;

public class PropertiesConfiguration implements Configuration {
    private static final Map<Class<?>, Function<String, ?>> PARSERS;

    private static final String UNRECOGNISED_TYPE_ERROR =
            "Could not find parser for given type '%s'. Parseable types: %s";
    private static final String INVALID_VALUE_ERROR =
            "Given '%s' value is not a valid value for type '%s'. Reason: '%s'. Configuration: %s";
    private static final String PROPERTY_NOT_SET_ERROR =
            "Missing required option(s) [%s] in configuration: %s";

    static {
        PARSERS = Map.ofEntries(
                entry(Boolean.class, Boolean::valueOf),
                entry(Byte.class, Byte::valueOf),
                entry(Short.class, Short::valueOf),
                entry(Character.class, value -> value.charAt(0)),
                entry(Integer.class, Integer::valueOf),
                entry(Long.class, Long::valueOf),
                entry(Float.class, Float::valueOf),
                entry(Double.class, Double::valueOf),
                entry(String.class, Function.identity()),
                entry(Path.class, Paths::get),
                entry(File.class, File::new),
                entry(LocalTime.class, LocalTime::parse),
                entry(LocalDate.class, LocalDate::parse),
                entry(LocalDateTime.class, LocalDateTime::parse),
                entry(Instant.class, Instant::parse),
                entry(BigDecimal.class, BigDecimal::new),
                entry(BigInteger.class, BigInteger::new),
                entry(byte[].class, String::getBytes)
        );
    }

    private final Map<String, Object> properties;

    PropertiesConfiguration(Map<String, ?> properties) {
        this.properties = copyOf(properties);
    }

    PropertiesConfiguration(Properties properties) {
        this.properties = properties.entrySet().stream()
                                    .map(entry -> Map.entry(entry.getKey().toString(), entry.getValue()))
                                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Configuration loadExternalFileFromOptions() {
        Optional<String> externalFile = getAliased("config-file", "configFile");
        if (!externalFile.isPresent()) {
            return this;
        }

        Configuration loaded = Configurations.load(externalFile.get());

        Map<String, Object> appendedProperties = new HashMap<>();
        appendedProperties.putAll(((PropertiesConfiguration) loaded).properties);
        appendedProperties.putAll(this.properties);

        return new PropertiesConfiguration(appendedProperties);
    }

    private Optional<String> getAliased(String... aliases) {
        for (String alias : aliases) {
            Optional<String> result = getOptional(alias);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    public Optional<String> getOptional(String option) {
        return Optional.ofNullable(properties.get(option))
                       .map(Object::toString)
                       .map(String::trim);
    }

    public void requireOptions(String... options) {
        Set<Object> missingOptions = new HashSet<>(List.of(options));
        missingOptions.removeAll(properties.keySet());

        if (!missingOptions.isEmpty()) {
            throw new IllegalArgumentException(
                    format(PROPERTY_NOT_SET_ERROR, missingOptions, properties)
            );
        }
    }

    @Override
    public boolean hasOption(String option) {
        return properties.containsKey(option);
    }

    public <T> Optional<T> getOptional(String option, Class<T> type) {
        if (!properties.containsKey(option)) {
            return Optional.empty();
        }

        return Optional.ofNullable(parse(properties.get(option), type));
    }

    @SuppressWarnings("unchecked")
    private <T> T parse(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        String stringValue = value.toString().trim();

        if (Enum.class.isAssignableFrom(type)) {
            return (T) Enum.valueOf((Class) type, stringValue.toUpperCase());
        }

        Function<String, T> parser = (Function<String, T>) PARSERS.get(type);

        if (parser == null) {
            throw new IllegalArgumentException(
                    format(UNRECOGNISED_TYPE_ERROR, type, PARSERS.keySet())
            );
        }

        try {
            return parser.apply(stringValue);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    format(INVALID_VALUE_ERROR, stringValue, type.toString(), e.getMessage(), properties),
                    e
            );
        }
    }

    public String get(String option) {
        if (!properties.containsKey(option)) {
            throw new IllegalArgumentException(
                    format(PROPERTY_NOT_SET_ERROR, option, properties)
            );
        }
        return properties.get(option).toString().trim();
    }

    public <T> T get(String option, Class<T> type) {
        if (!properties.containsKey(option)) {
            throw new IllegalArgumentException(
                    format(PROPERTY_NOT_SET_ERROR, option, properties)
            );
        }

        return parse(properties.get(option), type);
    }
}
