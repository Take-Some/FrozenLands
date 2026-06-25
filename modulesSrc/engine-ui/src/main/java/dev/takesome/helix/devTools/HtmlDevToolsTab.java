package dev.takesome.helix.devTools;

import java.util.Locale;

/** Chromium-like DevTools panels exposed by the engine-ui HTML debugger. */
public enum HtmlDevToolsTab {
    ELEMENTS("elements"),
    STYLES("styles"),
    COMPUTED("computed"),
    LAYOUT("layout"),
    SOURCE("source"),
    ACTIONS("actions"),
    DIAGNOSTICS("diagnostics");

    private final String id;

    HtmlDevToolsTab(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static HtmlDevToolsTab fromId(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (HtmlDevToolsTab tab : values()) {
            if (tab.id.equals(normalized) || tab.name().equalsIgnoreCase(normalized)) return tab;
        }
        return ELEMENTS;
    }

    public boolean cssPanel() {
        return this == STYLES || this == COMPUTED || this == LAYOUT;
    }
}
