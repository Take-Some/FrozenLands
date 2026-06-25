package dev.takesome.helix.ui.uiComponents.button;

import java.util.Locale;

/** HTML-compatible semantic button type used for canonical UI component events. */
public enum UiButtonType {
    BUTTON("button", "click"),
    SUBMIT("submit", "submit"),
    RESET("reset", "reset");

    private final String attributeValue;
    private final String activationInteraction;

    UiButtonType(String attributeValue, String activationInteraction) {
        this.attributeValue = attributeValue;
        this.activationInteraction = activationInteraction;
    }

    public String attributeValue() {
        return attributeValue;
    }

    public String activationInteraction() {
        return activationInteraction;
    }

    public static UiButtonType fromMarkupType(String raw) {
        if (raw == null || raw.isBlank()) return BUTTON;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "submit" -> SUBMIT;
            case "reset" -> RESET;
            case "button" -> BUTTON;
            default -> BUTTON;
        };
    }
}
