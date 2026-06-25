package dev.takesome.helix.ui.uiComponents.input;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
/** Text value, placeholder and max-length policy for input fields. */
final class InputTextModel {
    private String value;
    private String placeholder = "";
    private int maxLength = 96;

    InputTextModel(String value) { this.value = emptyIfNull(value); }

    String value() { return value; }
    String placeholder() { return placeholder; }
    int maxLength() { return maxLength; }

    boolean setValue(String value) {
        String next = clamp(emptyIfNull(value));
        if (this.value.equals(next)) return false;
        this.value = next;
        return true;
    }

    void setPlaceholder(String placeholder) { this.placeholder = emptyIfNull(placeholder); }

    void setMaxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
        setValue(value);
    }

    boolean append(String text) {
        if (text == null || text.isEmpty()) return false;
        return setValue(value + text.replace("\r", "").replace("\n", ""));
    }

    boolean backspace() {
        if (value.isEmpty()) return false;
        return setValue(value.substring(0, value.length() - 1));
    }

    private String clamp(String input) {
        if (input == null) return "";
        if (maxLength < 1 || input.length() <= maxLength) return input;
        return input.substring(0, maxLength);
    }
}
