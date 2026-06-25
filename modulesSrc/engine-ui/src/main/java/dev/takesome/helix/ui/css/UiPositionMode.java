package dev.takesome.helix.ui.css;

/** Typed position mode parsed from CSS position. */
public record UiPositionMode(boolean relative, boolean absolute) {
    public boolean outOfFlow() {
        return absolute;
    }
}
