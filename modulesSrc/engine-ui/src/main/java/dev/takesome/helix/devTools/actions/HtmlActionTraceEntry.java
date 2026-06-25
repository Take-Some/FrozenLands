package dev.takesome.helix.devTools.actions;

import java.io.Serializable;
import java.util.Map;

public record HtmlActionTraceEntry(String actionId, int sourceNodeId, Map<String, String> data, long timestampMillis) implements Serializable {
    public HtmlActionTraceEntry {
        actionId = actionId == null ? "" : actionId.trim();
        data = data == null ? Map.of() : Map.copyOf(data);
        timestampMillis = Math.max(0L, timestampMillis);
    }
}
