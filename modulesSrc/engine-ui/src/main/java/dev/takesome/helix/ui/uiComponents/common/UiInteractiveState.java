package dev.takesome.helix.ui.uiComponents.common;

/** Shared hover/press/focus/active state for retained interactive nodes. */
public final class UiInteractiveState {
    private final Runnable dirty;
    private UiInteractionListener listener = UiInteractionListener.NONE;
    private boolean hovered;
    private boolean pressed;
    private boolean focused;
    private boolean active;

    public UiInteractiveState(Runnable dirty) {
        this.dirty = dirty == null ? () -> {} : dirty;
    }

    public boolean hovered() { return hovered; }
    public boolean pressed() { return pressed; }
    public boolean focused() { return focused; }
    public boolean active() { return active; }

    public void setListener(UiInteractionListener listener) {
        this.listener = listener == null ? UiInteractionListener.NONE : listener;
    }

    public void setHovered(boolean hovered) {
        if (this.hovered == hovered) return;
        this.hovered = hovered;
        dirty.run();
        listener.onHoverChanged(hovered);
    }

    public void setPressed(boolean pressed) {
        if (this.pressed == pressed) return;
        this.pressed = pressed;
        dirty.run();
        listener.onPressedChanged(pressed);
    }

    public void setFocused(boolean focused) {
        if (this.focused == focused) return;
        this.focused = focused;
        dirty.run();
    }

    public void setActive(boolean active) {
        if (this.active == active) return;
        this.active = active;
        dirty.run();
        listener.onActiveChanged(active);
    }
}
