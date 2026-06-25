package dev.takesome.helix.ui.component;

public final class UiComponentException extends RuntimeException {
    public UiComponentException(String message) {
        super(message);
    }

    public UiComponentException(String message, Throwable cause) {
        super(message, cause);
    }
}
