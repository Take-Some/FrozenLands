package dev.takesome.helix.ui.uiComponents.slider;

import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.Node;

/** Pointer interaction state machine for sliders. */
final class SliderInputController {
    interface Callbacks {
        void setFromPointer(float pointerX, boolean committed);
        void emitInteraction(String interaction, double previousValue, double nextValue, boolean committed);
        void markDirty();
    }

    private boolean hovered;
    private boolean dragging;

    boolean hovered() { return hovered; }
    boolean dragging() { return dragging; }

    UiSliderDragState dragState(boolean enabled) {
        if (!enabled) return UiSliderDragState.DISABLED;
        if (dragging) return UiSliderDragState.DRAGGING;
        if (hovered) return UiSliderDragState.HOVERED;
        return UiSliderDragState.IDLE;
    }

    boolean handle(UiInputEvent event, Node owner, SliderModel model, boolean enabled, Callbacks callbacks) {
        if (!enabled || event == null || !event.isPointerEvent()) return false;
        boolean inside = owner.containsAbsolute(event.mouseX(), event.mouseY());
        if (event.isMouseMove()) {
            if (dragging) {
                callbacks.setFromPointer(event.mouseX(), false);
                return true;
            }
            setHovered(inside, callbacks);
            return false;
        }
        if (event.isMouseDown() && inside) {
            dragging = true;
            setHovered(true, callbacks);
            callbacks.emitInteraction("dragStart", model.value(), model.value(), false);
            callbacks.setFromPointer(event.mouseX(), false);
            callbacks.markDirty();
            return true;
        }
        if (event.isMouseUp()) {
            boolean wasDragging = dragging;
            if (wasDragging) {
                double previous = model.value();
                callbacks.setFromPointer(event.mouseX(), true);
                boolean changed = Math.abs(previous - model.value()) >= SliderModel.EPSILON;
                dragging = false;
                setHovered(inside, callbacks);
                callbacks.markDirty();
                if (!changed) callbacks.emitInteraction("commit", previous, model.value(), true);
                return true;
            }
            return false;
        }
        if (event.isMouseClick() && inside) {
            double previous = model.value();
            callbacks.setFromPointer(event.mouseX(), true);
            if (Math.abs(previous - model.value()) < SliderModel.EPSILON) callbacks.emitInteraction("commit", previous, model.value(), true);
            return true;
        }
        return false;
    }

    private void setHovered(boolean hovered, Callbacks callbacks) {
        if (this.hovered == hovered) return;
        this.hovered = hovered;
        callbacks.markDirty();
    }
}
