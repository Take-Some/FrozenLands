package dev.takesome.helix.ui.uiComponents.combo;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
/** Immutable combo-box option descriptor compiled from markup. */
public record UiComboBoxOption(String value, String label, boolean disabled) {
    public UiComboBoxOption {
        value = trimToEmpty(value);
        label = label == null ? value : label.trim();
    }
}
