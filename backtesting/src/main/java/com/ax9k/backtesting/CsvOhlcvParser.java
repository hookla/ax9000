package com.ax9k.backtesting;

import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.core.time.Time;
import org.apache.commons.lang3.Validate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.function.Function;

import static java.lang.String.format;

class CsvOhlcvParser implements Function<String, OhlcvBar> {
    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter INPUT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String SEPARATOR = ",";
    private static final String ZERO_SECONDS = ":00";
    private static final int INDEX_DATE = 0, INDEX_TIME = 1;
    private static final int INDEX_OPEN = 2, INDEX_HIGH = 3, INDEX_LOW = 4, INDEX_CLOSE = 5, INDEX_VOLUME = 7;
    private static final int MINIMUM_COLUMNS = INDEX_VOLUME + 1;

    @Override
    public OhlcvBar apply(String line) {
        Validate.notBlank(line, "line value is blank: '%s'", line);
        String[] values = line.split(SEPARATOR);
        Validate.isTrue(values.length >= MINIMUM_COLUMNS,
                        "Given line does not have enough columns: ".concat(line));
        LocalDate eventDate = LocalDate.parse(values[INDEX_DATE], INPUT_DATE_FORMAT);
        LocalTime eventTime = LocalTime.parse(normaliseTime(values[INDEX_TIME]), INPUT_TIME_FORMAT);
        Instant eventTimestamp = Time.internationalise(LocalDateTime.of(eventDate, eventTime));

        return OhlcvBar.of(eventTimestamp,
                           parseValue(values, INDEX_OPEN),
                           parseValue(values, INDEX_HIGH),
                           parseValue(values, INDEX_LOW),
                           parseValue(values, INDEX_CLOSE),
                           parseValue(values, INDEX_VOLUME));
    }

    private static String normaliseTime(String time) {
        return secondsMissing(time) ? time.concat(ZERO_SECONDS) : time;
    }

    private static boolean secondsMissing(String time) {
        return time.length() == 5;
    }

    private static double parseValue(String[] values, int index) {
        double result;

        try {
            result = Double.parseDouble(values[index]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(format("Value '%s' at index '%s' is not a valid number: %s",
                                                      values[index], index, Arrays.toString(values)));
        }

        Validate.finite(result, "Value '%s' at index '%s' is not a valid number: %s",
                        values[index], index, Arrays.toString(values));

        return result;
    }
}
