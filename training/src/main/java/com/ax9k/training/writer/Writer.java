package com.ax9k.training.writer;

import com.ax9k.training.Schema;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

public interface Writer {
    void writeDate(String line) throws IOException;

    void writeObject(Map<String, Object> object) throws IOException;

    void writeFormatToSchema(Schema schema);

    String resolveFileName(String prefix, String suffix, LocalDate defaultDate);
}