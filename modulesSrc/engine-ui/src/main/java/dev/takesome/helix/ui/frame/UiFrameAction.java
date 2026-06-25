package dev.takesome.helix.ui.frame;

import dev.takesome.helix.ui.command.UiCommandId;

import java.util.LinkedHashMap;
import java.util.Map;

/** Compiled action attached to a UI IR node. */
public final class UiFrameAction {
    private final UiCommandId commandId;
    private final Map<String, String> arguments;

    public UiFrameAction(UiCommandId commandId, Map<String, String> arguments) {
        if (commandId == null) throw new IllegalArgumentException("commandId must not be null");
        this.commandId = commandId;
        this.arguments = Map.copyOf(arguments == null ? Map.of() : new LinkedHashMap<>(arguments));
    }

    public UiCommandId commandId() {
        return commandId;
    }

    public Map<String, String> arguments() {
        return arguments;
    }
}
