package dev.takesome.helix.devTools;

import java.io.Serializable;
import java.util.Map;

public record HtmlDevToolsRemoteAction(String actionId, int nodeId, Map<String, String> data) implements Serializable {
    public HtmlDevToolsRemoteAction {
        actionId = actionId == null ? "" : actionId.trim();
        nodeId = Math.max(0, nodeId);
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
