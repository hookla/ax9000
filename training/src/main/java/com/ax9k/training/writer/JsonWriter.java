package com.ax9k.training.writer;

import com.ax9k.training.Schema;
import com.ax9k.utils.json.JsonUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class JsonWriter implements Writer {
    private final BufferedWriter output;

    private String date;

    public JsonWriter(BufferedWriter output) {
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
        output.append(JsonUtils.toJsonString(object));
        output.newLine();
    }

    @Override
    public void writeFormatToSchema(Schema schema) {
        schema.setFormat("JSON", false);
    }

    @Override
    public String resolveFileName(String prefix, String suffix, LocalDate defaultDate) {
        String dateAffix = "";
        if (date != null) {
            dateAffix = "_" + date;
        } else if (defaultDate != null) {
            dateAffix = "_" + defaultDate.toString();
        }
        return prefix + dateAffix + ".json" + suffix;
    }
}
