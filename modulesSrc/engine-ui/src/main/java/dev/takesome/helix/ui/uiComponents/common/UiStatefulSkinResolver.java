package dev.takesome.helix.ui.uiComponents.common;

import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;

/** Resolves colors and external skins from shared interactive state. */
public final class UiStatefulSkinResolver {
    public UiColor color(boolean enabled, UiInteractiveState state, UiColor normal, UiColor hover, UiColor pressed, UiColor disabled) {
        return color(enabled, state, normal, hover, pressed, hover, disabled);
    }

    public UiColor color(boolean enabled, UiInteractiveState state, UiColor normal, UiColor hover, UiColor pressed, UiColor active, UiColor disabled) {
        if (!enabled) return disabled;
        if (state != null && state.pressed()) return pressed;
        if (state != null && state.active()) return active;
        if (state != null && state.hovered()) return hover;
        return normal;
    }

    public UiColor focusColor(boolean enabled, UiInteractiveState state, UiColor normal, UiColor hover, UiColor focused, UiColor disabled) {
        if (!enabled) return disabled;
        if (state != null && state.focused()) return focused;
        if (state != null && state.hovered()) return hover;
        return normal;
    }

    public UiElementSkin element(boolean enabled, UiInteractiveState state, UiElementSkin normal, UiElementSkin hover, UiElementSkin pressed, UiElementSkin disabled) {
        return element(enabled, state, normal, hover, pressed, hover, disabled);
    }

    public UiElementSkin element(boolean enabled, UiInteractiveState state, UiElementSkin normal, UiElementSkin hover, UiElementSkin pressed, UiElementSkin active, UiElementSkin disabled) {
        if (!enabled) return disabled != null ? disabled : normal;
        if (state != null && state.pressed()) return pressed != null ? pressed : normal;
        if (state != null && state.active()) return active != null ? active : normal;
        if (state != null && state.hovered()) return hover != null ? hover : normal;
        return normal;
    }

    public UiElementSkin focusElement(boolean enabled, UiInteractiveState state, UiElementSkin normal, UiElementSkin hover, UiElementSkin focused, UiElementSkin disabled) {
        if (!enabled) return disabled != null ? disabled : normal;
        if (state != null && state.focused()) return focused != null ? focused : normal;
        if (state != null && state.hovered()) return hover != null ? hover : normal;
        return normal;
    }
}
