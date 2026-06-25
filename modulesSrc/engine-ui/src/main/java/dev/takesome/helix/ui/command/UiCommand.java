package dev.takesome.helix.ui.command;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable UI command produced by an action event. */
public final class UiCommand {
    private final UiCommandId id;
    private final Map<String, String> arguments;

    public UiCommand(UiCommandId id, Map<String, String> arguments) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        this.id = id;
        this.arguments = Map.copyOf(arguments == null ? Map.of() : new LinkedHashMap<>(arguments));
    }

    public UiCommandId id() {
        return id;
    }

    public Map<String, String> arguments() {
        return arguments;
    }
}
