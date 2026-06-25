package dev.takesome.helix.ui.uiComponents.combo;

/** Value-change packet emitted by a combo box. */
public record UiComboBoxValueChangedEvent(
        String nodeId,
        String previousValue,
        String value,
        String label
) {
}
