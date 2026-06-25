package dev.takesome.helix.ui.skin;

import java.util.Locale;

/** How the stretchable slice area is filled in target space. */
public enum UiSliceScaleMode {
    STRETCH,
    REPEAT;

    public static UiSliceScaleMode parse(String raw, UiSliceScaleMode fallback) {
        if (raw == null || raw.isBlank()) return fallback == null ? STRETCH : fallback;
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (value) {
            case "repeat", "tile", "tiled" -> REPEAT;
            case "stretch", "scale" -> STRETCH;
            default -> fallback == null ? STRETCH : fallback;
        };
    }
}
