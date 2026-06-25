package dev.takesome.helix.ui.layout;

public enum UiAnchor {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER;

    public static UiAnchor parse(String raw) {
        if (raw == null) return TOP_LEFT;

        String key = raw.trim()
                .replace("-", "")
                .replace("_", "")
                .toLowerCase();

        if ("topleft".equals(key)) return TOP_LEFT;
        if ("topright".equals(key)) return TOP_RIGHT;
        if ("bottomleft".equals(key)) return BOTTOM_LEFT;
        if ("bottomright".equals(key)) return BOTTOM_RIGHT;
        if ("center".equals(key)) return CENTER;

        throw new IllegalArgumentException("Unknown UI anchor: " + raw);
    }
}
