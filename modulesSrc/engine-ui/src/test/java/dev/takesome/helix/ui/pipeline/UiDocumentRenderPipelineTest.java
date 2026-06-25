package dev.takesome.helix.ui.pipeline;


import static dev.takesome.helix.validation.EngineValidator.textOrEmpty;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.definition.UiPanelDefinition;
import dev.takesome.helix.ui.legacy.render.UiPanelRenderEntry;
import dev.takesome.helix.ui.definition.UiWidgetBounds;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.layout.UiWidgetMeasure;
import dev.takesome.helix.ui.binding.UiBindingManifest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.takesome.helix.ui.model.UiRect;

final class UiDocumentRenderPipelineTest {
    @Test
    void panelLayoutPassArrangesVBoxWidgets() {
        UiDocument document = new UiDocument();
        UiPanelDefinition panel = new UiPanelDefinition();
        panel.id = "hud";
        panel.anchor = "bottomLeft";
        panel.layout = "vbox";
        panel.x = 10f;
        panel.y = 20f;
        panel.paddingLeft = 4f;
        panel.paddingTop = 4f;
        panel.paddingBottom = 4f;
        panel.gap = 2f;
        UiWidgetDefinition first = widget("title", 40f, 10f);
        UiWidgetDefinition second = widget("value", 50f, 12f);
        panel.widgets.add(first);
        panel.widgets.add(second);
        document.panels.add(panel);

        List<UiPanelRenderEntry> entries = new UiPanelLayoutPass().layout(document, EmptySource.INSTANCE, null, 800f, 600f);

        assertEquals(1, entries.size());
        assertEquals("hud", UiPanelLayoutPass.panelKey(entries.get(0).panel, entries.get(0).index));
        assertTrue(UiWidgetBounds.w(first) > 0f);
        assertEquals(4f, UiWidgetBounds.x(first), 0.001f);
        assertEquals(4f, UiWidgetBounds.x(second), 0.001f);
        assertTrue(UiWidgetBounds.y(first) > UiWidgetBounds.y(second));
    }


    @Test
    void panelMeasurePassResolvesAutoSize() {
        UiPanelDefinition panel = new UiPanelDefinition();
        panel.layout = "vbox";
        panel.paddingLeft = 3f;
        panel.paddingRight = 5f;
        panel.paddingTop = 7f;
        panel.paddingBottom = 11f;
        panel.gap = 2f;
        panel.widgets.add(widget("a", 20f, 10f));
        panel.widgets.add(widget("b", 30f, 12f));

        UiWidgetMeasure measured = new UiPanelMeasurePass().measurePanel(panel, EmptySource.INSTANCE, null, 800f, 600f);

        assertEquals(38f, measured.w(), 0.001f);
        assertEquals(42f, measured.h(), 0.001f);
    }

    @Test
    void panelMeasurePassIncludesImplicitTextAndBarContent() {
        UiPanelDefinition panel = new UiPanelDefinition();
        panel.layout = "vbox";
        panel.gap = 1f;

        UiWidgetDefinition title = new UiWidgetDefinition();
        title.id = "title";
        title.primitive = "text";
        title.text = "PLAYER";

        UiWidgetDefinition bar = new UiWidgetDefinition();
        bar.id = "status";
        bar.primitive = "bar";
        bar.text = "READY";

        panel.widgets.add(title);
        panel.widgets.add(bar);

        UiWidgetMeasure measured = new UiPanelMeasurePass().measurePanel(panel, EmptySource.INSTANCE, null, 800f, 600f);

        assertEquals(120f, measured.w(), 0.001f);
        assertEquals(37f, measured.h(), 0.001f);
    }

    @Test
    void widgetArrangePassWritesResolvedHBoxBounds() {
        UiPanelDefinition panel = new UiPanelDefinition();
        panel.layout = "hbox";
        panel.paddingLeft = 5f;
        panel.paddingBottom = 4f;
        panel.gap = 3f;
        UiWidgetDefinition left = widget("left", 10f, 8f);
        UiWidgetDefinition right = widget("right", 12f, 6f);
        panel.widgets.add(left);
        panel.widgets.add(right);

        new UiWidgetArrangePass().arrange(panel, EmptySource.INSTANCE, null, new dev.takesome.helix.ui.model.UiRect(0f, 0f, 100f, 20f));

        assertEquals(5f, UiWidgetBounds.x(left), 0.001f);
        assertEquals(18f, UiWidgetBounds.x(right), 0.001f);
        assertTrue(UiWidgetBounds.y(left) >= 4f);
    }

    @Test
    void stackLayoutPassStacksBottomLeftGroup() {
        UiPanelDefinition first = new UiPanelDefinition();
        first.id = "first";
        first.anchor = "bottomLeft";
        first.x = 10f;
        first.y = 20f;
        first.stackGroup = "left-feed";
        first.stackGap = 5f;
        UiPanelDefinition second = new UiPanelDefinition();
        second.id = "second";
        second.anchor = "bottomLeft";
        second.x = 10f;
        second.y = 20f;
        second.stackGroup = "left-feed";

        List<UiPanelRenderEntry> stacked = new UiStackLayoutPass().apply(List.of(
                new UiPanelRenderEntry(0, first, new dev.takesome.helix.ui.model.UiRect(10f, 20f, 50f, 10f)),
                new UiPanelRenderEntry(1, second, new dev.takesome.helix.ui.model.UiRect(10f, 20f, 40f, 12f))
        ), 800f, 600f);

        assertEquals(20f, stacked.get(0).rect.y, 0.001f);
        assertEquals(35f, stacked.get(1).rect.y, 0.001f);
    }
    @Test
    void bindingPreparePassControlsPanelVisibility() {
        UiDocument document = new UiDocument();
        document.bindingPacks.add(UiBindingManifest.of(
                "visibility.bindings",
                UiBindingManifest.binding("hud.visible", "hud.visible", "ui.visible", "boolean", null)
        ));
        UiPanelDefinition panel = new UiPanelDefinition();
        panel.id = "hud";
        panel.w = 100f;
        panel.h = 40f;
        document.panels.add(panel);

        UiBindingSource prepared = new UiBindingPreparePass().prepare(document, new MapSource(Map.of("ui.visible", "false")));
        List<UiPanelRenderEntry> entries = new UiPanelLayoutPass().layout(document, prepared, null, 800f, 600f);

        assertTrue(entries.isEmpty());
    }

    private static UiWidgetDefinition widget(String id, float w, float h) {
        UiWidgetDefinition widget = new UiWidgetDefinition();
        widget.id = id;
        widget.w = w;
        widget.h = h;
        return widget;
    }

    private enum EmptySource implements UiBindingSource {
        INSTANCE;

        @Override
        public String text(String key) { return ""; }

        @Override
        public float number(String key) { return 0f; }

        @Override
        public boolean bool(String key) { return false; }
    }

    private record MapSource(Map<String, String> values) implements UiBindingSource {
        @Override
        public String text(String key) {
            return textOrEmpty(values, item -> item.getOrDefault(key, ""));
        }

        @Override
        public float number(String key) {
            String value = text(key);
            return value.isBlank() ? 0f : Float.parseFloat(value);
        }

        @Override
        public boolean bool(String key) {
            return Boolean.parseBoolean(text(key));
        }
    }
}
