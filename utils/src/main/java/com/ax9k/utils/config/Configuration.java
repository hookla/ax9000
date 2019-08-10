package com.ax9k.utils.config;

import java.util.Optional;
import java.util.function.Function;

public interface Configuration {
    Configuration loadExternalFileFromOptions();

    void requireOptions(String... options);

    boolean hasOption(String option);

    @SuppressWarnings("unchecked")
    default <T> T getOptional(String option, T defaultValue) {
        Optional<T> result = (Optional<T>) getOptional(option, defaultValue.getClass());

        return result.orElse(defaultValue);
    }

    <T> Optional<T> getOptional(String option, Class<T> type);

    default <T> Optional<T> getOptional(String option, Function<String, T> parser) {
        return getOptional(option).map(parser);
    }

    default Optional<String> getOptional(String option) {
        return getOptional(option, String.class);
    }

    default <T> T get(String option, Function<String, T> parser) {
        return parser.apply(get(option));
    }

    default String get(String option) {
        return get(option, String.class);
    }

    <T> T get(String option, Class<T> type);
}
