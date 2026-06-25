package dev.takesome.helix.ui.markup.internal.action;

import dev.takesome.helix.events.bus.EventBus;
import dev.takesome.helix.ui.command.EventBusUiCommandBus;
import dev.takesome.helix.ui.command.UiCommand;
import dev.takesome.helix.ui.command.UiCommandBus;
import dev.takesome.helix.ui.command.UiCommandId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Binds markup actions to the typed UI command bus. */
public final class UiMarkupActionBinder {
    private static final Set<String> RESERVED = Set.of(
            "id", "class", "x", "y", "w", "h", "width", "height",
            "skin", "button-style", "buttonStyle",
            "frame", "font", "font-family", "scale", "font-size", "color", "align", "text",
            "icon", "fa", "font-awesome", "fontAwesome", "icon-scale", "iconScale", "icon-size", "iconSize",
            "icon-gap", "iconGap", "icon-inset", "iconInset",
            "appear", "appear-animation", "appearAnimation", "appear-ms", "appearMs",
            "appear-offset-y", "appearOffsetY", "overlay-color", "overlay-fade-ms", "overlayFadeMs", "modal-fade-ms",
            "action", "command", "data-command", "data-action", "data-module", "data-scene", "data-target", "event", "visible", "disabled", "overlay", "data-overlay", "stylesheet",
            "i18n-key", "data-i18n-key", "i18n-args", "data-i18n-args", "i18n-format", "data-i18n-format", "data-title-key", "data-message-key"
    );

    private final UiCommandBus commands;

    public UiMarkupActionBinder(EventBus events) {
        this(new EventBusUiCommandBus(events));
    }

    public UiMarkupActionBinder(UiCommandBus commands) {
        this.commands = commands;
    }

    public Runnable bind(Map<String, String> style) {
        String commandId = actionId(style);
        if (commandId.isBlank()) return null;
        UiCommand command = new UiCommand(new UiCommandId(commandId), commandArguments(style));
        return () -> dispatch(command);
    }

    public void dispatch(String commandId, Map<String, String> arguments) {
        if (commandId == null || commandId.isBlank()) return;
        dispatch(new UiCommand(new UiCommandId(commandId), arguments));
    }

    public String actionId(Map<String, String> style) {
        return first(style, "data-command", "action", "command", "data-action", "event");
    }

    public Map<String, String> commandArguments(Map<String, String> style) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (style == null || style.isEmpty()) return out;
        for (Map.Entry<String, String> entry : style.entrySet()) {
            String key = entry.getKey();
            if (key == null || RESERVED.contains(key)) continue;
            String value = entry.getValue();
            if (value != null && !value.isBlank()) out.put(key, value);
        }
        aliasDataArgument(out, style, "data-module", "module");
        aliasDataArgument(out, style, "data-scene", "scene");
        aliasDataArgument(out, style, "data-target", "target");
        return out;
    }

    private void aliasDataArgument(Map<String, String> out, Map<String, String> style, String dataKey, String commandKey) {
        if (out == null || style == null || commandKey == null || commandKey.isBlank()) return;
        if (out.containsKey(commandKey)) return;
        String value = style.getOrDefault(dataKey, "");
        if (value != null && !value.isBlank()) out.put(commandKey, value.trim());
    }

    private void dispatch(UiCommand command) {
        if (commands == null || command == null) return;
        commands.dispatch(command);
    }

    private String first(Map<String, String> style, String... keys) {
        if (style == null || style.isEmpty()) return "";
        for (String key : keys) {
            String value = style.getOrDefault(key, "").trim();
            if (!value.isBlank()) return value;
        }
        return "";
    }
}
