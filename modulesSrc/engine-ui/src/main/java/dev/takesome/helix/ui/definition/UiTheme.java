package dev.takesome.helix.ui.definition;

import com.badlogic.gdx.graphics.Color;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Engine-owned HUD theme values shared by game-specific renderers.
 */
public final class UiTheme {
    public final float padding;
    public final float panelHeight;
    public final float resourceBlockHeight;
    public final float barHeight;
    public final float iconSize;
    public final float fontScaleTitle;
    public final float fontScaleSmall;
    public final Color textPrimary;
    public final Color textSecondary;
    public final Color panelShadow;

    public UiTheme(
            float padding,
            float panelHeight,
            float resourceBlockHeight,
            float barHeight,
            float iconSize,
            float fontScaleTitle,
            float fontScaleSmall,
            Color textPrimary,
            Color textSecondary,
            Color panelShadow
    ) {
        this.padding = padding;
        this.panelHeight = panelHeight;
        this.resourceBlockHeight = resourceBlockHeight;
        this.barHeight = barHeight;
        this.iconSize = iconSize;
        this.fontScaleTitle = fontScaleTitle;
        this.fontScaleSmall = fontScaleSmall;
        this.textPrimary = new Color(textPrimary);
        this.textSecondary = new Color(textSecondary);
        this.panelShadow = new Color(panelShadow);
    }

    public static UiTheme fromJson(JsonObject object, UiTheme fallback) {
        if (fallback == null) throw new IllegalArgumentException("fallback must not be null");
        if (object == null) return fallback;

        return new UiTheme(
                number(object, "padding", fallback.padding),
                number(object, "panelHeight", fallback.panelHeight),
                number(object, "resourceBlockHeight", fallback.resourceBlockHeight),
                number(object, "barHeight", fallback.barHeight),
                number(object, "iconSize", fallback.iconSize),
                number(object, "fontScaleTitle", fallback.fontScaleTitle),
                number(object, "fontScaleSmall", fallback.fontScaleSmall),
                color(object, "textPrimary", fallback.textPrimary),
                color(object, "textSecondary", fallback.textSecondary),
                color(object, "panelShadow", fallback.panelShadow)
        );
    }

    private static float number(JsonObject object, String name, float fallback) {
        if (!object.has(name) || object.get(name).isJsonNull()) return fallback;
        return object.get(name).getAsFloat();
    }

    private static Color color(JsonObject object, String name, Color fallback) {
        if (!object.has(name) || !object.get(name).isJsonArray()) return new Color(fallback);
        JsonArray array = object.getAsJsonArray(name);
        float r = array.size() > 0 ? array.get(0).getAsFloat() : fallback.r;
        float g = array.size() > 1 ? array.get(1).getAsFloat() : fallback.g;
        float b = array.size() > 2 ? array.get(2).getAsFloat() : fallback.b;
        float a = array.size() > 3 ? array.get(3).getAsFloat() : fallback.a;
        return new Color(r, g, b, a);
    }
}
