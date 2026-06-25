package dev.takesome.helix.ui.layout;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import dev.takesome.helix.ui.model.UiRect;

/**
 * Named UI section map owned by helix.
 *
 * Game code names sections such as playerPanel or controlsHint; helix
 * owns resolving, storage and JSON hydration of those sections.
 */
public final class UiLayout {
    private final Map<String, UiPanelLayout> panels;

    private UiLayout(Map<String, UiPanelLayout> panels) {
        this.panels = Collections.unmodifiableMap(new LinkedHashMap<>(panels));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<String> panelIds() {
        return panels.keySet();
    }

    public boolean hasPanel(String id) {
        return panels.containsKey(id);
    }

    public UiPanelLayout panel(String id) {
        UiPanelLayout panel = panels.get(Objects.requireNonNull(id, "id"));
        if (panel == null) {
            throw new IllegalArgumentException("Unknown UI panel: " + id);
        }
        return panel;
    }

    public UiPanelLayout panelOrDefault(String id, UiPanelLayout fallback) {
        UiPanelLayout panel = panels.get(Objects.requireNonNull(id, "id"));
        return panel == null ? fallback : panel;
    }

    public UiRect resolve(String id, float viewportW, float viewportH) {
        return UiLayoutResolver.resolve(panel(id), viewportW, viewportH);
    }

    public UiRect resolveOrDefault(String id, float viewportW, float viewportH, UiPanelLayout fallback) {
        return UiLayoutResolver.resolve(panelOrDefault(id, fallback), viewportW, viewportH);
    }

    public Map<String, UiPanelLayout> asMap() {
        return panels;
    }

    public static final class Builder {
        private final Map<String, UiPanelLayout> panels = new LinkedHashMap<>();

        private Builder() {}

        public Builder panel(String id, UiPanelLayout panel) {
            panels.put(requireId(id), Objects.requireNonNull(panel, "panel"));
            return this;
        }

        public Builder copyOf(UiLayout layout) {
            if (layout != null) {
                panels.putAll(layout.panels);
            }
            return this;
        }

        public UiLayout build() {
            return new UiLayout(panels);
        }

        private static String requireId(String id) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("panel id must not be blank");
            }
            return id.trim();
        }
    }
}
