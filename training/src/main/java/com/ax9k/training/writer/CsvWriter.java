package com.ax9k.training.writer;

import com.ax9k.training.Schema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class CsvWriter implements Writer {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Collector<CharSequence, ?, String> COMMA_SEPARATED = Collectors.joining(",");
    private static final DecimalFormat EIGHT_DECIMAL_PLACES;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setDecimalSeparator('.');

        EIGHT_DECIMAL_PLACES = new DecimalFormat();
        EIGHT_DECIMAL_PLACES.setGroupingUsed(false);
        EIGHT_DECIMAL_PLACES.setMaximumFractionDigits(8);
        EIGHT_DECIMAL_PLACES.setDecimalFormatSymbols(symbols);
    }

    private final BufferedWriter output;

    private String date;
    private boolean headersWritten;
    private Set<String> expectedProperties;

    public CsvWriter(BufferedWriter output) {
        this.output = output;
    }

    @Override
    public void writeDate(String line) {
        requireNonNull(line, "writeDate line");
        date = line;
    }

    @Override
    public void writeObject(Map<String, Object> object) throws IOException {
        requireNonNull(object, "writeObject object");
        if (!headersWritten) {
            writeHeaders(object);
            headersWritten = true;
        }
        writeValues(object);
    }

    private void writeHeaders(Map<String, Object> object) throws IOException {
        expectedProperties = object.keySet();

        String line = expectedProperties.stream()
                                        .collect(COMMA_SEPARATED);
        output.append(line);

        output.newLine();
    }

    private void writeValues(Map<String, Object> object) throws IOException {
        if (object.size() < expectedProperties.size()) {
            return;
        } else if (object.size() > expectedProperties.size()) {
            LOGGER.warn("More columns in object than expected. Columns: {}, Expected: {}",
                        object.keySet(), expectedProperties
            );
            outputOnlyExpectedProperties(object);
        } else {
            outputObject(object);
        }
        output.newLine();
    }

    private void outputOnlyExpectedProperties(Map<String, Object> object) throws IOException {
        Iterator<String> expected = expectedProperties.iterator();

        output.append(formattedString(object.get(expected.next())));
        while (expected.hasNext()) {
            output.append(',').append(formattedString(object.get(expected.next())));
        }
    }

    private static String formattedString(Object object) {
        if (object instanceof Number) {
            return EIGHT_DECIMAL_PLACES.format(object);
        }
        return object.toString();
    }

    private void outputObject(Map<String, Object> object) throws IOException {
        String line = object.values().stream()
                            .map(CsvWriter::formattedString)
                            .collect(COMMA_SEPARATED);
        output.append(line);
    }

    @Override
    public void writeFormatToSchema(Schema schema) {
        schema.setFormat("CSV", true);
    }

    @Override
    public String resolveFileName(String prefix, String suffix, LocalDate defaultDate) {
        String dateAffix = "";
        if (date != null) {
            dateAffix = "_" + date;
        } else if (defaultDate != null) {
            dateAffix = "_" + defaultDate.toString();
        }
        return prefix + dateAffix + ".csv" + suffix;
    }
}
