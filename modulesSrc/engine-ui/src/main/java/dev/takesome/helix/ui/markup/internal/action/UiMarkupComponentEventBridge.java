package dev.takesome.helix.ui.markup.internal.action;

import dev.takesome.helix.ui.component.event.UiComponentEvent;
import dev.takesome.helix.ui.component.event.UiComponentEventSink;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bridges retained component events to markup action/event dispatch. */
public final class UiMarkupComponentEventBridge implements UiComponentEventSink {
    private final UiMarkupActionBinder actions;
    private final Map<String, String> style;
    private final Map<String, String> extraArguments;

    public UiMarkupComponentEventBridge(UiMarkupActionBinder actions, Map<String, String> style) {
        this(actions, style, Map.of());
    }

    public UiMarkupComponentEventBridge(UiMarkupActionBinder actions, Map<String, String> style, Map<String, String> extraArguments) {
        this.actions = actions;
        this.style = style == null ? Map.of() : Map.copyOf(style);
        this.extraArguments = extraArguments == null ? Map.of() : Map.copyOf(extraArguments);
    }

    @Override
    public void emit(UiComponentEvent event) {
        if (actions == null || event == null) return;
        LinkedHashMap<String, String> args = new LinkedHashMap<>(actions.commandArguments(style));
        args.putAll(extraArguments);
        args.putAll(event.arguments());
        String id = style.getOrDefault("id", "");
        if (!id.isBlank()) args.putIfAbsent("sourceId", id);
        String action = actions.actionId(style);
        if (!action.isBlank()) {
            args.putIfAbsent("sourceAction", action);
            args.putIfAbsent("action", action);
        }
        actions.dispatch(UiComponentEvent.EVENT_ID, args);
        actions.dispatch(event.aliasEventId(), args);
        if (!action.isBlank() && shouldDispatchAction(event)) {
            actions.dispatch(action, args);
        }
    }

    private static boolean shouldDispatchAction(UiComponentEvent event) {
        String interaction = event.interaction();
        return "click".equals(interaction)
                || "changed".equals(interaction)
                || "captured".equals(interaction)
                || "cancelled".equals(interaction)
                || "submit".equals(interaction)
                || "reset".equals(interaction);
    }
}
