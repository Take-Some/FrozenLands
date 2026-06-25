package dev.takesome.helix.ui.pipeline;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.takesome.helix.ui.legacy.render.NineSlice;
import dev.takesome.helix.ui.legacy.render.NineSliceRenderer;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.legacy.render.UiNineSliceResolver;
import dev.takesome.helix.ui.definition.UiPanelDefinition;
import dev.takesome.helix.ui.legacy.render.UiPanelRenderEntry;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.legacy.render.UiWidgetRenderers;

import java.util.List;
import java.util.Locale;

/** Renders resolved panels, panel backgrounds and delegates widget rendering. */
public final class UiPanelRenderPass {
    private final NineSliceRenderer<TextureRegion> nine;
    private final UiNineSliceResolver resolver;
    private final UiWidgetRenderers widgets;
    private final UiWidgetRenderPass widgetRenderPass;

    public UiPanelRenderPass(
            NineSliceRenderer<TextureRegion> nine,
            UiNineSliceResolver resolver,
            UiWidgetRenderers widgets,
            UiWidgetRenderPass widgetRenderPass
    ) {
        this.nine = nine;
        this.resolver = resolver;
        this.widgets = widgets;
        this.widgetRenderPass = widgetRenderPass;
    }

    public void render(
            UiDocument document,
            UiBindingSource binding,
            SpriteBatch batch,
            BitmapFont font,
            List<UiPanelRenderEntry> panels,
            float viewportW,
            float viewportH,
            float uiTime,
            boolean drawTracers
    ) {
        if (panels == null) return;
        for (UiPanelRenderEntry entry : panels) {
            if (entry == null) continue;
            drawPanel(document, binding, batch, font, entry, viewportW, viewportH, uiTime, drawTracers);
        }
    }

    private void drawPanel(
            UiDocument document,
            UiBindingSource binding,
            SpriteBatch batch,
            BitmapFont font,
            UiPanelRenderEntry entry,
            float viewportW,
            float viewportH,
            float uiTime,
            boolean drawTracers
    ) {
        UiPanelDefinition panel = entry.panel;
        UiRect rect = entry.rect;
        String panelKey = UiPanelLayoutPass.panelKey(panel, entry.index);
        if (panel.overlay && panel.fillColor != null) {
            widgets.fillRect(batch, 0f, 0f, viewportW, viewportH, panel.fillColor, panel.alpha);
        } else if (panel.fillColor != null) {
            widgets.fillRect(batch, rect.x, rect.y, rect.w, rect.h, panel.fillColor, panel.alpha);
        }

        if (panel.background != null && !panel.background.isBlank()) {
            NineSlice<TextureRegion> background = resolver.slice(document, panel.background);
            if (background != null) nine.draw(background, rect.x, rect.y, rect.w, rect.h);
        }

        if (drawTracers) widgetRenderPass.drawTracer(batch, rect, 0);
        if (clipsChildren(panel)) {
            renderWidgetsClipped(binding, batch, font, rect, panel, panelKey, viewportW, viewportH, uiTime, drawTracers);
        } else {
            widgetRenderPass.renderWidgets(binding, batch, font, rect, panel.widgets, panelKey, uiTime, drawTracers);
        }
    }

    private void renderWidgetsClipped(
            UiBindingSource binding,
            SpriteBatch batch,
            BitmapFont font,
            UiRect rect,
            UiPanelDefinition panel,
            String panelKey,
            float viewportW,
            float viewportH,
            float uiTime,
            boolean drawTracers
    ) {
        UiRect clip = clippedToViewport(UiPanelLayoutPass.contentRect(rect, panel), viewportW, viewportH);
        if (clip.w <= 0f || clip.h <= 0f) return;
        if (batch == null || Gdx.gl == null) {
            widgetRenderPass.renderWidgets(binding, batch, font, rect, panel.widgets, panelKey, uiTime, drawTracers);
            return;
        }
        batch.flush();
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(Math.round(clip.x), Math.round(clip.y), Math.round(clip.w), Math.round(clip.h));
        try {
            widgetRenderPass.renderWidgets(binding, batch, font, rect, panel.widgets, panelKey, uiTime, drawTracers);
        } finally {
            batch.flush();
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        }
    }

    private UiRect clippedToViewport(UiRect rect, float viewportW, float viewportH) {
        if (rect == null) return new UiRect(0f, 0f, 0f, 0f);
        float x1 = Math.max(0f, rect.x);
        float y1 = Math.max(0f, rect.y);
        float x2 = Math.min(Math.max(0f, viewportW), rect.right());
        float y2 = Math.min(Math.max(0f, viewportH), rect.top());
        return new UiRect(x1, y1, Math.max(0f, x2 - x1), Math.max(0f, y2 - y1));
    }

    private boolean clipsChildren(UiPanelDefinition panel) {
        if (panel == null || !panel.clipChildren) return false;
        String overflow = panel.overflow == null ? "hidden" : panel.overflow.trim().toLowerCase(Locale.ROOT);
        return !("visible".equals(overflow) || "none".equals(overflow));
    }
}
