package dev.takesome.helix.ui.css;

/** CSS registry/parser/runtime exception. */
public final class UiCssException extends RuntimeException {
    public UiCssException(String message) {
        super(message);
    }

    public UiCssException(String message, Throwable cause) {
        super(message, cause);
    }
}
