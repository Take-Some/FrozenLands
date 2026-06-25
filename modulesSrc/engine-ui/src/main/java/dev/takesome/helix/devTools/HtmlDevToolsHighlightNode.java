package dev.takesome.helix.devTools;

import static dev.takesome.helix.validation.EngineValidator.hasPositiveFiniteSize;

import dev.takesome.helix.devTools.dom.HtmlElementSnapshot;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Draws the current external DevTools hover target back into the live game/editor UI frame. */
public final class HtmlDevToolsHighlightNode extends Node {
    private static final UiColor FILL = UiColor.rgba255(14, 165, 233, 44);
    private static final UiColor STROKE = UiColor.rgba255(2, 132, 199, 235);
    private static final UiColor LABEL_BG = UiColor.rgba255(248, 250, 252, 245);
    private static final UiColor LABEL_TEXT = UiColor.rgba255(15, 23, 42, 255);
    private static final String FONT = "engine-ui-system-fs-elliot-pro-bold";

    private final HtmlDevToolsSnapshotFactory snapshots = new HtmlDevToolsSnapshotFactory();

    @Override
    protected void onRender(UiRenderContext ctx) {
        int nodeId = HtmlDevToolsRuntime.highlightedNodeId();
        if (nodeId <= 0) return;

        HtmlDevToolsSession session = new HtmlDevToolsSession(true, HtmlDevToolsTab.ELEMENTS, nodeId, false);
        HtmlDevToolsSnapshot snapshot = snapshots.create(session, HtmlDevToolsRuntime.target());
        HtmlElementSnapshot element = snapshot.selectedElement();
        if (element == null || !hasPositiveFiniteSize(element.width(), element.height())) return;

        UiRect rect = new UiRect(element.x(), element.y(), element.width(), element.height());
        ctx.fill(rect, FILL);
        ctx.stroke(rect, STROKE, 2f);

        String label = element.selector() + "  " + element.dimensionsLabel();
        float labelW = Math.min(Math.max(180f, label.length() * 6.5f + 18f), Math.max(180f, absoluteBounds().w - 24f));
        float labelX = clamp(rect.x, 12f, Math.max(12f, absoluteBounds().w - labelW - 12f));
        float labelY = rect.y + rect.h + 8f;
        if (labelY + 24f > absoluteBounds().h - 8f) labelY = Math.max(8f, rect.y - 28f);
        UiRect labelRect = new UiRect(labelX, labelY, labelW, 22f);
        ctx.fill(labelRect, LABEL_BG);
        ctx.stroke(labelRect, STROKE, 1f);
        ctx.text(truncate(label, (int) ((labelW - 14f) / 6.2f)), new UiRect(labelRect.x + 7f, labelRect.y + 5f, labelRect.w - 14f, 12f), 0.52f, LABEL_TEXT, TextAlign.LEFT, FONT);
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        if (max <= 1) return "…";
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, Math.max(0, max - 1)) + "…";
    }
}
