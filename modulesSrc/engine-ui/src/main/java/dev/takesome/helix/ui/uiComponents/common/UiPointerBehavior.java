package dev.takesome.helix.ui.uiComponents.common;

import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.Node;

/** Shared pointer routing for click-like and focusable retained UI nodes. */
public final class UiPointerBehavior {
    private final Node owner;
    private final UiInteractiveState state;

    public UiPointerBehavior(Node owner, UiInteractiveState state) {
        this.owner = owner;
        this.state = state;
    }

    public boolean handleActivation(UiInputEvent event, Runnable onActivated) {
        if (!event.isPointerEvent()) return false;
        boolean inside = owner.containsAbsolute(event.mouseX(), event.mouseY());

        if (event.isMouseMove()) {
            state.setHovered(inside);
            return false;
        }

        if (event.isMouseDown()) {
            if (!inside) return false;
            state.setHovered(true);
            state.setPressed(true);
            return true;
        }

        if (event.isMouseUp()) {
            boolean wasPressed = state.pressed();
            state.setPressed(false);
            state.setHovered(inside);
            if (wasPressed && inside) {
                if (onActivated != null) onActivated.run();
                return true;
            }
            return wasPressed;
        }

        if (event.isMouseClick() && inside) {
            if (onActivated != null) onActivated.run();
            return true;
        }

        return false;
    }

    public boolean handleFocus(UiInputEvent event, UiFocusBehavior focus) {
        if (!event.isPointerEvent()) return false;
        boolean inside = owner.containsAbsolute(event.mouseX(), event.mouseY());

        if (event.isMouseMove()) {
            state.setHovered(inside);
            return false;
        }

        if (event.isMouseDown()) {
            if (focus != null) focus.setFocused(inside);
            state.setPressed(inside);
            state.setHovered(inside);
            return inside;
        }

        if (event.isMouseUp()) {
            boolean wasPressed = state.pressed();
            state.setPressed(false);
            state.setHovered(inside);
            return wasPressed && inside;
        }

        if (event.isMouseClick() && inside) {
            if (focus != null) focus.setFocused(true);
            return true;
        }

        return false;
    }
}
