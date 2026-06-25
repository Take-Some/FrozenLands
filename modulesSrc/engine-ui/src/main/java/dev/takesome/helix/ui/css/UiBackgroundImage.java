package dev.takesome.helix.ui.css;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
/** Typed CSS background-image value. */
public record UiBackgroundImage(String source) {
    private static final UiBackgroundImage NONE = new UiBackgroundImage("");

    public UiBackgroundImage {
        source = trimToEmpty(source);
    }

    public static UiBackgroundImage none() {
        return NONE;
    }

    public boolean noneValue() {
        return source.isBlank();
    }
}
