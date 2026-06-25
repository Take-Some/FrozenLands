package dev.takesome.helix.ui.command;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Validated command registry for UI action handlers. */
public final class UiCommandRegistry implements UiCommandBus {
    private final Map<UiCommandId, UiCommandHandler> handlers = new LinkedHashMap<>();

    public UiCommandRegistry register(String id, UiCommandHandler handler) {
        return register(new UiCommandId(id), handler);
    }

    public UiCommandRegistry register(UiCommandId id, UiCommandHandler handler) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (handler == null) throw new IllegalArgumentException("handler must not be null");
        handlers.put(id, handler);
        return this;
    }

    public Optional<UiCommandHandler> find(UiCommandId id) {
        return Optional.ofNullable(handlers.get(id));
    }

    public UiCommandHandler require(UiCommandId id) {
        UiCommandHandler handler = handlers.get(id);
        if (handler == null) throw new IllegalArgumentException("No UI command handler registered for: " + id);
        return handler;
    }

    @Override
    public void dispatch(UiCommand command) {
        if (command == null) return;
        require(command.id()).handle(command);
    }
}
