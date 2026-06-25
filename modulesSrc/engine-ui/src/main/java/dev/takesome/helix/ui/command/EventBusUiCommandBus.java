package dev.takesome.helix.ui.command;

import dev.takesome.helix.events.api.EngineEvent;
import dev.takesome.helix.events.api.EventContext;
import dev.takesome.helix.events.bus.EventBus;

/** EventBus adapter for UI commands. This keeps actions off direct renderer/runtime invocation. */
public final class EventBusUiCommandBus implements UiCommandBus {
    private final EventBus events;

    public EventBusUiCommandBus(EventBus events) {
        this.events = events;
    }

    @Override
    public void dispatch(UiCommand command) {
        if (events == null || command == null) return;
        EventContext ctx = new EventContext();
        command.arguments().forEach(ctx::put);
        events.emit(EngineEvent.highPriority(command.id().value()), ctx);
    }
}
