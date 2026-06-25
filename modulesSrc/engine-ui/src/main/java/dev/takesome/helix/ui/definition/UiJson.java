package dev.takesome.helix.ui.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.takesome.helix.data.gson.GsonDataLoader;

import java.util.Map;
import dev.takesome.helix.ui.layout.UiAnchor;
import dev.takesome.helix.ui.layout.UiLayout;
import dev.takesome.helix.ui.layout.UiPanelLayout;

/** JSON hydration for engine-owned UI layout and theme documents. */
public final class UiJson {
    private static final UiPanelLayout EMPTY_PANEL = new UiPanelLayout(UiAnchor.TOP_LEFT, 0f, 0f, 0f, 0f);

    private UiJson() {}

    public static UiLayout layoutOrDefault(String internalPath, UiLayout fallback) {
        UiLayout base = fallback == null ? UiLayout.builder().build() : fallback;
        try {
            JsonObject root = GsonDataLoader.readObject(internalPath);
            UiLayout.Builder builder = UiLayout.builder().copyOf(base);
            for (String id : base.panelIds()) {
                builder.panel(id, panel(root, id, base.panel(id)));
            }
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if ("theme".equals(entry.getKey())) continue;
                if (!base.hasPanel(entry.getKey()) && entry.getValue().isJsonObject()) {
                    builder.panel(entry.getKey(), panel(entry.getValue().getAsJsonObject(), EMPTY_PANEL));
                }
            }
            return builder.build();
        } catch (RuntimeException ex) {
            return base;
        }
    }

    public static UiTheme themeOrDefault(String internalPath, UiTheme fallback) {
        try {
            JsonObject root = GsonDataLoader.readObject(internalPath);
            JsonObject theme = root.has("theme") && root.get("theme").isJsonObject()
                    ? root.getAsJsonObject("theme")
                    : null;
            return UiTheme.fromJson(theme, fallback);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static UiPanelLayout panel(JsonObject root, String name, UiPanelLayout fallback) {
        if (!root.has(name) || !root.get(name).isJsonObject()) return fallback;
        return panel(root.getAsJsonObject(name), fallback);
    }

    private static UiPanelLayout panel(JsonObject object, UiPanelLayout fallback) {
        UiAnchor anchor = UiAnchor.parse(string(object, "anchor", fallback.anchor.name()));
        return new UiPanelLayout(
                anchor,
                number(object, "x", fallback.x),
                number(object, "y", fallback.y),
                number(object, "w", fallback.w),
                number(object, "h", fallback.h),
                string(object, "mode", fallback.mode)
        );
    }

    private static float number(JsonObject object, String name, float fallback) {
        if (!object.has(name) || object.get(name).isJsonNull()) return fallback;
        return object.get(name).getAsFloat();
    }

    private static String string(JsonObject object, String name, String fallback) {
        if (!object.has(name) || object.get(name).isJsonNull()) return fallback;
        return object.get(name).getAsString();
    }
}
