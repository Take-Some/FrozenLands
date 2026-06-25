package dev.takesome.helix.ui.uiComponents.combo;

import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.Node;

/** Pointer state machine for combo boxes and their dropdown overlay. */
final class ComboBoxInputController {
    interface Callbacks {
        void selectOption(int index);
        void setOpen(boolean open);
        void markDirty();
    }

    private boolean open;
    private boolean hovered;
    private int hoveredOption = -1;

    boolean open() { return open; }
    boolean hovered() { return hovered; }
    int hoveredOption() { return hoveredOption; }

    UiComboBoxState state(boolean enabled) {
        if (!enabled) return UiComboBoxState.DISABLED;
        if (open) return UiComboBoxState.OPEN;
        if (hovered) return UiComboBoxState.HOVERED;
        return UiComboBoxState.CLOSED;
    }

    void setOpen(boolean open, Callbacks callbacks) {
        if (this.open == open) return;
        this.open = open;
        hoveredOption = -1;
        callbacks.markDirty();
    }

    void forceClosed(Callbacks callbacks) {
        open = false;
        hoveredOption = -1;
        callbacks.markDirty();
    }

    boolean handleMain(UiInputEvent event, Node owner, UiRect b, ComboBoxModel model, Callbacks callbacks) {
        if (event == null || !event.isPointerEvent()) return false;
        boolean inside = owner.containsAbsolute(event.mouseX(), event.mouseY());
        int optionIndex = ComboBoxRenderer.optionIndexAt(event.mouseX(), event.mouseY(), b, model, open);
        if (event.isMouseMove()) {
            setHovered(inside || optionIndex >= 0, callbacks);
            setHoveredOption(optionIndex, callbacks);
            return false;
        }
        if (event.isMouseDown() || event.isMouseClick()) {
            if (open && optionIndex >= 0) {
                callbacks.selectOption(optionIndex);
                return true;
            }
            if (inside) {
                callbacks.setOpen(!open);
                setHovered(true, callbacks);
                return true;
            }
            if (open) {
                callbacks.setOpen(false);
                return true;
            }
        }
        return false;
    }

    boolean handleOverlay(UiInputEvent event, UiRect base, ComboBoxModel model, Callbacks callbacks) {
        if (event == null || !open || !event.isPointerEvent()) return false;
        int optionIndex = ComboBoxRenderer.optionIndexAt(event.mouseX(), event.mouseY(), base, model, open);
        if (event.isMouseMove()) {
            setHoveredOption(optionIndex, callbacks);
            return optionIndex >= 0;
        }
        if ((event.isMouseDown() || event.isMouseClick()) && optionIndex >= 0) {
            callbacks.selectOption(optionIndex);
            return true;
        }
        return false;
    }

    private void setHovered(boolean hovered, Callbacks callbacks) {
        if (this.hovered == hovered) return;
        this.hovered = hovered;
        callbacks.markDirty();
    }

    private void setHoveredOption(int hoveredOption, Callbacks callbacks) {
        if (this.hoveredOption == hoveredOption) return;
        this.hoveredOption = hoveredOption;
        callbacks.markDirty();
    }
}
