package com.ax9k.cex.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Objects;

import static org.apache.commons.lang3.Validate.notNull;

public class CexRequest {
    private final MessageType type;
    private final JsonNode data;
    private final String oid;

    public CexRequest(MessageType type, JsonNode data, boolean withOid) {
        this.type = notNull(type);
        this.data = data;
        oid = withOid ? Instant.now().toEpochMilli() + "_" + type.getName() : null;
    }

    MessageType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        CexRequest that = (CexRequest) o;
        return type == that.type &&
               Objects.equals(data, that.data) &&
               Objects.equals(oid, that.oid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, data, oid);
    }

    @Override
    public String toString() {
        return toJson();
    }

    String toJson() {
        ObjectNode root = JsonMapper.createObjectNode();
        root.put("e", type.getName());
        if (data != null) {
            root.putPOJO("data", data);
        }
        if (oid != null) {
            root.put("oid", oid);
        }
        return JsonMapper.write(root);
    }
}
