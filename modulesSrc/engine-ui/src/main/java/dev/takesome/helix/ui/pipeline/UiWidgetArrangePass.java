package dev.takesome.helix.ui.pipeline;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiPanelDefinition;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.definition.UiWidgetBounds;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.layout.UiWidgetMeasure;
import dev.takesome.helix.ui.layout.UiInsets;

/** Arranges widgets inside an already resolved panel rectangle. */
public final class UiWidgetArrangePass {
    private final UiPanelMeasurePass measurePass;

    public UiWidgetArrangePass() {
        this(new UiPanelMeasurePass());
    }

    public UiWidgetArrangePass(UiPanelMeasurePass measurePass) {
        this.measurePass = measurePass == null ? new UiPanelMeasurePass() : measurePass;
    }

    public void arrange(UiPanelDefinition panel, UiBindingSource binding, BitmapFont font, UiRect rect) {
        if (panel == null || panel.widgets == null) return;
        clearWidgetLayouts(panel);
        if (!UiPanelMeasurePass.autoLayout(panel)) return;

        String layout = UiPanelMeasurePass.layoutMode(panel);
        UiInsets padding = UiPanelMeasurePass.panelPadding(panel);
        float gap = UiPanelMeasurePass.panelGap(panel);
        float contentW = Math.max(0f, rect.w - padding.left() - padding.right());
        float contentH = Math.max(0f, rect.h - padding.bottom() - padding.top());

        if (UiPanelMeasurePass.LAYOUT_HBOX.equals(layout)) {
            arrangeHBox(panel, binding, font, contentW, contentH, padding, gap);
        } else {
            arrangeVBox(panel, binding, font, contentW, rect.h, padding, gap);
        }
    }

    private void arrangeHBox(UiPanelDefinition panel, UiBindingSource binding, BitmapFont font, float contentW, float contentH, UiInsets padding, float gap) {
        float x = padding.left();
        for (int i = 0; i < panel.widgets.size(); i++) {
            UiWidgetDefinition widget = panel.widgets.get(i);
            if (widget == null || !UiPanelLayoutPass.visible(UiPanelLayoutPass.widgetTarget(widget, i, "visible"), widget.visible, binding)) continue;
            UiWidgetMeasure child = measurePass.measureWidget(widget, binding, font);
            float childW = Math.min(contentW, child.w());
            float childH = Math.min(contentH, child.h());
            float y = padding.bottom() + Math.max(0f, (contentH - childH) * 0.5f);
            UiWidgetBounds.setResolvedLayout(widget, x, y, childW, childH);
            x += childW + gap;
        }
    }

    private void arrangeVBox(UiPanelDefinition panel, UiBindingSource binding, BitmapFont font, float contentW, float panelHeight, UiInsets padding, float gap) {
        float top = panelHeight - padding.top();
        for (int i = 0; i < panel.widgets.size(); i++) {
            UiWidgetDefinition widget = panel.widgets.get(i);
            if (widget == null || !UiPanelLayoutPass.visible(UiPanelLayoutPass.widgetTarget(widget, i, "visible"), widget.visible, binding)) continue;
            UiWidgetMeasure child = measurePass.measureWidget(widget, binding, font);
            float childH = child.h();
            UiWidgetBounds.setResolvedLayout(widget, padding.left(), top - childH, contentW, childH);
            top -= childH + gap;
        }
    }

    public void clearWidgetLayouts(UiPanelDefinition panel) {
        if (panel == null || panel.widgets == null) return;
        for (UiWidgetDefinition widget : panel.widgets) UiWidgetBounds.clearResolvedLayout(widget);
    }
}
