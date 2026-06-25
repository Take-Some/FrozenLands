package dev.takesome.helix.devTools.css;

import java.io.Serializable;

public record HtmlCssPropertySnapshot(String name, String value, String origin, boolean overridden, boolean disabled) implements Serializable {
    public HtmlCssPropertySnapshot {
        name = name == null ? "" : name.trim();
        value = value == null ? "" : value.trim();
        origin = origin == null ? "" : origin.trim();
    }

    public HtmlCssPropertySnapshot(String name, String value, String origin, boolean overridden) {
        this(name, value, origin, overridden, false);
    }

    public HtmlCssPropertySnapshot withDisabled(boolean nextDisabled, String nextValue) {
        return new HtmlCssPropertySnapshot(name, nextValue == null ? value : nextValue, origin, overridden, nextDisabled);
    }
}
