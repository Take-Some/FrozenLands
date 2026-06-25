package dev.takesome.helix.ui.uiComponents.combo;

/** Interaction packet emitted by a combo box. */
public record UiComboBoxInteractionEvent(
        String nodeId,
        String interaction,
        String previousValue,
        String value,
        String label,
        boolean open
) {
}
