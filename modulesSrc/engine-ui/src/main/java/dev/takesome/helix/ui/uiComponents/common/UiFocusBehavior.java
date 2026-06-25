package dev.takesome.helix.ui.uiComponents.common;

/** Shared focus and caret blink lifecycle for focusable UI controls. */
public final class UiFocusBehavior {
    private final UiInteractiveState state;
    private final Runnable dirty;
    private float caretTimer;

    public UiFocusBehavior(UiInteractiveState state, Runnable dirty) {
        this.state = state;
        this.dirty = dirty == null ? () -> {} : dirty;
    }

    public boolean focused() {
        return state.focused();
    }

    public void setFocused(boolean focused) {
        if (state.focused() == focused) return;
        state.setFocused(focused);
        resetCaret();
    }

    public void resetCaret() {
        caretTimer = 0f;
        dirty.run();
    }

    public void update(float dt) {
        if (!focused()) return;
        caretTimer += Math.max(0f, dt);
        if (caretTimer > 1.2f) caretTimer -= 1.2f;
        dirty.run();
    }

    public boolean caretVisible() {
        return caretTimer < 0.6f;
    }
}
