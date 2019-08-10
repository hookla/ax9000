package com.ax9k.cex.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.Validate;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.lang3.Validate.notNull;

public final class CexResponse {
    private static final String TYPE_FIELD = "e";
    private static final String DATA_FIELD = "data";
    private static final String REQUEST_ID_FIELD = "identifier";

    private final MessageType type;
    private final JsonNode data;
    private final Optional<String> identifier;

    private final Optional<Instant> timestamp;
    private final boolean ok;

    private CexResponse(MessageType type,
                        JsonNode data,
                        String identifier,
                        Instant timestamp, boolean ok) {
        this.type = notNull(type);
        this.data = notNull(data);
        this.identifier = Optional.ofNullable(identifier);
        this.timestamp = Optional.ofNullable(timestamp);
        this.ok = ok;
    }

    static CexResponse of(JsonNode root) {
        JsonNode type = root.get(TYPE_FIELD);
        JsonNode data = root.get(DATA_FIELD);
        String oid = root.hasNonNull(REQUEST_ID_FIELD) ? root.get(REQUEST_ID_FIELD).asText() : null;

        Validate.isTrue(type != null && !type.isNull(), "Invalid 'e' field in JSON response: %s", root);
        Validate.isTrue(data != null && !type.isNull(), "Invalid 'data' field in JSON response: %s", root);

        Instant timestamp = parseTimestamp(root);
        boolean ok = !isError(root);

        return new CexResponse(MessageType.of(type.asText()), data, oid, timestamp, ok);
    }

    private static Instant parseTimestamp(JsonNode root) {
        JsonNode timesampSeconds = root.get("timestamp");
        if (timesampSeconds != null) {
            return Instant.ofEpochSecond(timesampSeconds.longValue());
        }

        JsonNode timestampMillis = root.get("timestamp-ms");
        if (timestampMillis != null) {
            return Instant.ofEpochMilli(timestampMillis.longValue());
        }

        return null;
    }

    private static boolean isError(JsonNode root) {
        JsonNode ok = root.get("ok");

        return ok != null && ok.asText().equals("error");
    }

    public MessageType getType() {
        return type;
    }

    public JsonNode getData() {
        return data;
    }

    public Optional<String> getIdentifier() {
        return identifier;
    }

    public boolean hasIdentifier() {
        return identifier.isPresent();
    }

    public Optional<Instant> getTimestamp() {
        return timestamp;
    }

    boolean isOk() {
        return ok;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        CexResponse that = (CexResponse) o;
        return ok == that.ok &&
               type == that.type &&
               Objects.equals(data, that.data) &&
               Objects.equals(identifier, that.identifier) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, data, identifier, timestamp, ok);
    }

    @Override
    public String toString() {
        return "CexResponse{" +
               "type=" + type +
               ", data=" + data +
               ", identifier=" + identifier +
               ", timestamp=" + timestamp +
               ", ok=" + ok +
               '}';
    }
}
