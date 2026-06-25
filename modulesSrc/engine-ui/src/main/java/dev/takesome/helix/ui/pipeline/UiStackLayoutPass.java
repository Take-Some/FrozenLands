package dev.takesome.helix.ui.pipeline;

import dev.takesome.helix.ui.layout.UiAnchor;
import dev.takesome.helix.ui.definition.UiPanelDefinition;
import dev.takesome.helix.ui.legacy.render.UiPanelRenderEntry;
import dev.takesome.helix.ui.model.UiRect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies stack-group panel placement after individual panel rectangles are resolved. */
public final class UiStackLayoutPass {
    public List<UiPanelRenderEntry> apply(List<UiPanelRenderEntry> panels, float viewportW, float viewportH) {
        if (panels == null || panels.isEmpty()) return List.of();
        ArrayList<UiPanelRenderEntry> output = new ArrayList<>(panels);
        Map<String, ArrayList<UiPanelRenderEntry>> groups = new LinkedHashMap<>();
        for (UiPanelRenderEntry entry : output) {
            String key = stackKey(entry.panel);
            if (key != null) groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
        }
        if (groups.isEmpty()) return output;

        Map<Integer, UiRect> replacementRects = new HashMap<>();
        for (ArrayList<UiPanelRenderEntry> group : groups.values()) {
            applyGroup(group, viewportW, viewportH, replacementRects);
        }

        for (int i = 0; i < output.size(); i++) {
            UiPanelRenderEntry entry = output.get(i);
            UiRect replacement = replacementRects.get(entry.index);
            if (replacement != null) output.set(i, new UiPanelRenderEntry(entry.index, entry.panel, replacement));
        }
        return output;
    }

    private void applyGroup(ArrayList<UiPanelRenderEntry> group, float viewportW, float viewportH, Map<Integer, UiRect> replacementRects) {
        if (group.isEmpty()) return;
        UiPanelDefinition origin = group.get(0).panel;
        boolean equalWidth = false;
        float maxW = 0f;
        for (UiPanelRenderEntry entry : group) {
            equalWidth |= entry.panel.equalStackWidth;
            maxW = Math.max(maxW, entry.rect.w);
        }

        UiAnchor anchor = UiAnchor.parse(origin.anchor);
        float gap = stackGap(origin);
        float cursorTop = viewportH - origin.y;
        float cursorBottom = origin.y;

        for (UiPanelRenderEntry entry : group) {
            float w = equalWidth ? maxW : entry.rect.w;
            float h = entry.rect.h;
            UiRect rect;
            switch (anchor) {
                case TOP_LEFT -> {
                    rect = new UiRect(origin.x, cursorTop - h, w, h);
                    cursorTop = rect.y - gap;
                }
                case TOP_RIGHT -> {
                    rect = new UiRect(viewportW + origin.x - w, cursorTop - h, w, h);
                    cursorTop = rect.y - gap;
                }
                case BOTTOM_LEFT -> {
                    rect = new UiRect(origin.x, cursorBottom, w, h);
                    cursorBottom = rect.top() + gap;
                }
                case BOTTOM_RIGHT -> {
                    rect = new UiRect(viewportW + origin.x - w, cursorBottom, w, h);
                    cursorBottom = rect.top() + gap;
                }
                case CENTER -> rect = entry.rect;
                default -> throw new IllegalStateException("Unhandled anchor: " + anchor);
            }
            replacementRects.put(entry.index, rect);
        }
    }

    private float stackGap(UiPanelDefinition panel) {
        return panel != null && panel.stackGap >= 0f ? panel.stackGap : 8f;
    }

    private String stackKey(UiPanelDefinition panel) {
        if (panel == null || panel.stackGroup == null || panel.stackGroup.isBlank()) return null;
        return panel.stackGroup.trim();
    }
}
