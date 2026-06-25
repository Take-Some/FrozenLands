package dev.takesome.helix.ui.component;

/** Registry id of a UI component type: button, text, image, slider, combo_box, etc. */
public record UiComponentTypeId(String value) {
    public UiComponentTypeId {
        if (value == null || value.isBlank()) {
            throw new UiComponentException("UI component type id must not be blank");
        }
        value = value.trim();
    }

    public static UiComponentTypeId of(String value) {
        return new UiComponentTypeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
