package dev.takesome.helix.ui.uiComponents.common;

/** Listener for retained interactive-state edges. */
public interface UiInteractionListener {
    UiInteractionListener NONE = new UiInteractionListener() {};

    default void onHoverChanged(boolean hovered) {
    }

    default void onPressedChanged(boolean pressed) {
    }

    default void onActiveChanged(boolean active) {
    }
}
