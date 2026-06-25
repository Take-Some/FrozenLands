package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import dev.takesome.helix.ui.layout.UiBox;
import dev.takesome.helix.ui.layout.UiInsets;
import dev.takesome.helix.ui.layout.UiProgressBarLayout;
import dev.takesome.helix.ui.binding.UiBindingRuntimeSource;

import java.util.HashMap;
import java.util.Map;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.definition.UiWidgetBounds;
import dev.takesome.helix.ui.model.UiRect;

final class UiProgressBarRenderer {
    private final UiWidgetImageRenderer images;
    private final UiTextRenderer text;
    private final Map<String, UiProgressState> states = new HashMap<>();
    private final Color ghostColor = new Color();
    private final Color pulseColor = new Color();

    UiProgressBarRenderer(UiWidgetImageRenderer images, UiTextRenderer text) {
        this.images = images;
        this.text = text;
    }

    void reset() {
        states.clear();
    }

    void render(UiBindingSource binding, SpriteBatch batch, BitmapFont font, UiRect panel, UiWidgetDefinition widget, String key, float uiTime) {
        float x = panel.x + UiWidgetBounds.x(widget);
        float y = panel.y + UiWidgetBounds.y(widget);
        float w = UiWidgetBounds.w(widget);
        float h = UiWidgetBounds.h(widget);
        float target = MathUtils.clamp(progressValue(binding, widget), 0f, 1f);
        UiProgressState progress = progressState(key, target, uiTime);

        UiProgressBarLayout.Resolved layout = UiProgressBarLayout.resolve(
                x,
                y,
                w,
                h,
                widget.labelHeight,
                widget.labelGap,
                widget.trackHeight,
                insets(widget.trackInsets, UiProgressBarLayout.DEFAULT_TRACK_INSETS),
                progress.display
        );

        UiBox track = layout.track();
        images.drawStretchedAsset(batch, widget.base, 0, track.x(), track.y(), track.w(), track.h(), null);
        drawGhost(batch, widget, layout, x, y, w, h, progress);
        drawFill(batch, widget, layout, progress);
        drawPulse(batch, widget, layout, progress);
        drawLabel(binding, batch, font, widget, key, layout);
    }

    private float progressValue(UiBindingSource binding, UiWidgetDefinition widget) {
        String fallback = widget.progress == null ? widget.binding : widget.progress;
        if (binding instanceof UiBindingRuntimeSource runtime && widget != null && widget.id != null && !widget.id.isBlank()) {
            return runtime.numberTarget(widget.id + ".progress", fallback);
        }
        return binding.number(fallback);
    }

    private UiProgressState progressState(String key, float target, float uiTime) {
        UiProgressState state = states.computeIfAbsent(key, ignored -> new UiProgressState(target, uiTime));
        return state.update(target, uiTime);
    }

    private void drawGhost(SpriteBatch batch, UiWidgetDefinition widget, UiProgressBarLayout.Resolved layout, float x, float y, float w, float h, UiProgressState progress) {
        if (progress.ghost <= progress.display + 0.002f) return;
        UiProgressBarLayout.Resolved ghostLayout = UiProgressBarLayout.resolve(
                x,
                y,
                w,
                h,
                widget.labelHeight,
                widget.labelGap,
                widget.trackHeight,
                insets(widget.trackInsets, UiProgressBarLayout.DEFAULT_TRACK_INSETS),
                progress.ghost
        );
        UiBox ghostFill = ghostLayout.fill();
        float gx = fillRight(layout.fill());
        float gw = fillRight(ghostFill) - gx;
        if (gw > 0.5f) images.drawStretchedAsset(batch, widget.fill, 0, gx, ghostFill.y(), gw, ghostFill.h(), progressGhostColor());
    }

    private void drawFill(SpriteBatch batch, UiWidgetDefinition widget, UiProgressBarLayout.Resolved layout, UiProgressState progress) {
        UiBox fill = layout.fill();
        if (progress.display > 0f && !fill.empty()) {
            images.drawStretchedAsset(batch, widget.fill, 0, fill.x(), fill.y(), fill.w(), fill.h(), images.color(widget.fillColor, Color.WHITE));
        }
    }

    private void drawPulse(SpriteBatch batch, UiWidgetDefinition widget, UiProgressBarLayout.Resolved layout, UiProgressState progress) {
        UiBox fill = layout.fill();
        if (progress.pulse <= 0f || fill.empty()) return;
        float pulseW = Math.max(4f, Math.min(18f, fill.w() * 0.35f));
        float pulseX = Math.max(fill.x(), fillRight(fill) - pulseW);
        images.drawStretchedAsset(batch, widget.fill, 0, pulseX, fill.y(), fillRight(fill) - pulseX, fill.h(), progressPulseColor());
    }

    private void drawLabel(UiBindingSource binding, SpriteBatch batch, BitmapFont font, UiWidgetDefinition widget, String key, UiProgressBarLayout.Resolved layout) {
        String label = text.textValue(widget, binding);
        if (label.isEmpty()) return;
        UiBox labelBox = layout.label();
        UiRect labelRect = new UiRect(labelBox.x() + 8f, labelBox.y(), Math.max(0f, labelBox.w() - 16f), labelBox.h());
        text.label(binding, batch, font, widget, key, labelRect);
    }

    private float fillRight(UiBox box) {
        return box == null ? 0f : box.x() + Math.max(0f, box.w());
    }

    private Color progressGhostColor() {
        return ghostColor.set(1f, 0.28f, 0.14f, 0.42f);
    }

    private Color progressPulseColor() {
        return pulseColor.set(1f, 1f, 1f, 0.45f);
    }

    private UiInsets insets(float[] values, UiInsets fallback) {
        if (values == null || values.length == 0) return fallback;
        if (values.length == 1) return UiInsets.symmetric(values[0], values[0]);
        if (values.length == 2) return UiInsets.symmetric(values[0], values[1]);
        if (values.length == 4) return UiInsets.of(values[0], values[1], values[2], values[3]);
        return fallback;
    }
}
