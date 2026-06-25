package dev.takesome.helix.ui.uiComponents.button;

import dev.takesome.helix.ui.uiComponents.common.UiInteractionListener;
import dev.takesome.helix.ui.uiComponents.common.UiInteractiveState;

/** Retained button interaction state and transition weights. */
final class ButtonState {
    private static final float STATE_TRANSITION_SPEED = 18f;

    private final UiInteractiveState interaction;
    private float hoverTransition;
    private float pressTransition;
    private float activeTransition;

    ButtonState(Runnable dirty) {
        this.interaction = new UiInteractiveState(dirty);
    }

    UiInteractiveState interaction() { return interaction; }
    boolean hovered() { return interaction.hovered(); }
    boolean pressed() { return interaction.pressed(); }
    boolean active() { return interaction.active(); }
    float hoverTransition() { return hoverTransition; }
    float pressTransition() { return pressTransition; }
    float activeTransition() { return activeTransition; }

    void setListener(UiInteractionListener listener) { interaction.setListener(listener); }
    void setActive(boolean active) { interaction.setActive(active); }

    boolean update(float dt) {
        boolean changed = false;
        changed |= updateTransition(TransitionKind.HOVER, hovered(), dt);
        changed |= updateTransition(TransitionKind.PRESS, pressed(), dt);
        changed |= updateTransition(TransitionKind.ACTIVE, active(), dt);
        return changed;
    }

    private boolean updateTransition(TransitionKind kind, boolean targetEnabled, float dt) {
        float current = transitionValue(kind);
        float target = targetEnabled ? 1f : 0f;
        float next = approach(current, target, transitionStep(dt));
        if (Math.abs(next - current) <= 0.001f) return false;
        setTransitionValue(kind, next);
        return true;
    }

    private float transitionValue(TransitionKind kind) {
        return switch (kind) {
            case HOVER -> hoverTransition;
            case PRESS -> pressTransition;
            case ACTIVE -> activeTransition;
        };
    }

    private void setTransitionValue(TransitionKind kind, float value) {
        switch (kind) {
            case HOVER -> hoverTransition = value;
            case PRESS -> pressTransition = value;
            case ACTIVE -> activeTransition = value;
        }
    }

    private static float transitionStep(float dt) {
        if (!Float.isFinite(dt) || dt <= 0f) return 1f;
        return Math.max(0f, Math.min(1f, dt * STATE_TRANSITION_SPEED));
    }

    private static float approach(float current, float target, float step) {
        if (current < target) return Math.min(target, current + step);
        if (current > target) return Math.max(target, current - step);
        return target;
    }

    private enum TransitionKind { HOVER, PRESS, ACTIVE }
}
