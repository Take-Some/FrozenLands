package dev.takesome.helix.ui.skin;

import java.util.Locale;

/** Generic paint skin categories understood by engine-ui. */
public enum UiSkinType {
    IMAGE,
    NINE_SLICE,
    RIBBON,
    THREE_SLICE;

    public static UiSkinType parse(String raw, UiSkinType fallback) {
        if (raw == null || raw.isBlank()) return fallback == null ? IMAGE : fallback;
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (value) {
            case "image", "sprite", "texture" -> IMAGE;
            case "nine_slice", "nineslice", "9slice", "panel", "button" -> NINE_SLICE;
            case "ribbon", "banner", "horizontal_ribbon" -> RIBBON;
            case "three_slice", "threeslice", "3slice", "horizontal_slice", "horizontal" -> THREE_SLICE;
            default -> fallback == null ? IMAGE : fallback;
        };
    }
}
