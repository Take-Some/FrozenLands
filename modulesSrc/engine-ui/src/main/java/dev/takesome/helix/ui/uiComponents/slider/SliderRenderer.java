package dev.takesome.helix.ui.uiComponents.slider;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.icons.UiIconRegistries;
import dev.takesome.helix.ui.icons.registry.IconRegistry;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Slider drawing separated from numeric model and pointer controller. */
final class SliderRenderer {
    private static final IconRegistry ICONS = UiIconRegistries.standard();
    private static final UiIcon THUMB_ICON = ICONS.find("fontawesome:solid:circle")
            .orElseGet(() -> ICONS.find("circle").orElseGet(() -> ICONS.find("circle-dot").orElse(null)));

    private static final UiColor TRACK_SHADOW = new UiColor(0.00f, 0.00f, 0.00f, 0.42f);
    private static final UiColor TRACK = new UiColor(0.035f, 0.047f, 0.052f, 0.98f);
    private static final UiColor TRACK_BORDER = new UiColor(0.95f, 0.74f, 0.34f, 0.24f);
    private static final UiColor TRACK_INNER = new UiColor(1.00f, 0.88f, 0.58f, 0.10f);
    private static final UiColor TRACK_ACTIVE = new UiColor(0.96f, 0.68f, 0.22f, 0.98f);
    private static final UiColor TRACK_ACTIVE_HOT = new UiColor(1.00f, 0.86f, 0.40f, 0.96f);
    private static final UiColor THUMB = new UiColor(1.00f, 0.76f, 0.24f, 1.00f);
    private static final UiColor THUMB_HOVER = new UiColor(1.00f, 0.91f, 0.52f, 1.00f);
    private static final UiColor THUMB_BORDER = new UiColor(1.00f, 0.94f, 0.68f, 0.96f);
    private static final UiColor THUMB_CORE = new UiColor(0.12f, 0.07f, 0.00f, 0.96f);
    private static final UiColor THUMB_GLOW = new UiColor(1.00f, 0.72f, 0.24f, 0.20f);
    private static final UiColor VALUE_BG = new UiColor(1.00f, 0.75f, 0.24f, 1.00f);
    private static final UiColor VALUE_BORDER = new UiColor(1.00f, 0.92f, 0.62f, 0.94f);
    private static final UiColor VALUE_TEXT = new UiColor(0.13f, 0.08f, 0.00f, 1.00f);
    private static final UiColor VALUE_SHADOW = new UiColor(0.00f, 0.00f, 0.00f, 0.48f);
    private static final UiColor DISABLED = new UiColor(0.20f, 0.20f, 0.22f, 0.48f);
    private static final UiColor TEXT_DISABLED = new UiColor(0.62f, 0.58f, 0.50f, 0.62f);

    void render(UiRenderContext ctx, UiRect b, SliderModel model, UiSliderDragState dragState, boolean enabled) {
        float trackH = Math.max(5f, Math.min(8f, b.h * 0.20f));
        UiRect track = model.trackBounds(b, trackH);
        UiRect valueBox = model.valueBoxBounds(b);
        float ratio = (float) model.ratio();
        float activeW = Math.max(1f, track.w * ratio);
        UiRect active = new UiRect(track.x, track.y, activeW, track.h);
        float thumbSize = Math.max(16f, Math.min(24f, b.h * 0.66f));
        float thumbX = track.x + track.w * ratio;
        UiRect thumb = new UiRect(thumbX - thumbSize * 0.5f, b.y + b.h * 0.5f - thumbSize * 0.5f, thumbSize, thumbSize);
        boolean hot = dragState == UiSliderDragState.DRAGGING || dragState == UiSliderDragState.HOVERED;

        renderTrack(ctx, track, active, enabled);
        renderThumb(ctx, thumb, hot, enabled);
        renderValue(ctx, valueBox, SliderModel.format(model.value()), enabled);
    }

    private void renderTrack(UiRenderContext ctx, UiRect track, UiRect active, boolean enabled) {
        fillCapsule(ctx, new UiRect(track.x, track.y + 2f, track.w, track.h + 2f), TRACK_SHADOW);
        fillCapsule(ctx, track, enabled ? TRACK_BORDER : DISABLED);
        fillCapsule(ctx, inset(track, 1f), enabled ? TRACK : DISABLED);

        UiRect trackShine = inset(track, 1.4f);
        fillCapsule(ctx, new UiRect(trackShine.x, trackShine.y, trackShine.w, Math.max(1f, trackShine.h * 0.35f)), enabled ? TRACK_INNER : DISABLED);

        fillCapsule(ctx, active, enabled ? TRACK_ACTIVE : DISABLED);
        fillCapsule(ctx, new UiRect(active.x, active.y, active.w, Math.max(1f, active.h * 0.36f)), enabled ? TRACK_ACTIVE_HOT : DISABLED);
    }

    private void renderThumb(UiRenderContext ctx, UiRect thumb, boolean hot, boolean enabled) {
        UiColor icon = enabled ? (hot ? THUMB_HOVER : THUMB) : DISABLED;
        UiRect shadow = new UiRect(thumb.x + 2f, thumb.y + 2f, thumb.w, thumb.h);
        UiRect iconRect = expand(thumb, hot ? 1.5f : 0.0f);

        if (enabled && hot) fillCapsule(ctx, expand(thumb, 3f), THUMB_GLOW);
        fillCapsule(ctx, shadow, TRACK_SHADOW);
        fillCapsule(ctx, thumb, enabled ? THUMB_BORDER : DISABLED);
        fillCapsule(ctx, inset(thumb, 1.4f), enabled ? icon : DISABLED);

        if (THUMB_ICON != null && ctx.supportsIcons() && ctx.icon(THUMB_ICON, iconRect, hot ? 0.58f : 0.54f, enabled ? icon : TEXT_DISABLED, TextAlign.CENTER)) {
            return;
        }
    }

    private void renderValue(UiRenderContext ctx, UiRect valueBox, String value, boolean enabled) {
        ctx.fill(new UiRect(valueBox.x + 2f, valueBox.y + 2f, valueBox.w, valueBox.h), VALUE_SHADOW);
        ctx.fill(valueBox, enabled ? VALUE_BG : DISABLED);
        ctx.stroke(valueBox, enabled ? VALUE_BORDER : DISABLED, 1f);
        ctx.text(value, inset(valueBox, 3f), 0.70f, enabled ? VALUE_TEXT : TEXT_DISABLED, TextAlign.CENTER, "standard");
    }

    private static void fillCapsule(UiRenderContext ctx, UiRect rect, UiColor color) {
        if (ctx == null || rect == null || rect.w <= 0f || rect.h <= 0f) return;
        float height = Math.max(1f, rect.h);
        int rows = Math.max(1, (int) Math.ceil(height));
        float radius = height * 0.5f;
        float centerY = rect.y + radius;
        for (int i = 0; i < rows; i++) {
            float y = rect.y + i;
            float rowH = Math.min(1f, rect.y + height - y);
            if (rowH <= 0f) continue;
            float sampleY = y + rowH * 0.5f;
            float dy = Math.abs(sampleY - centerY);
            float inside = Math.max(0f, radius * radius - dy * dy);
            float rowInset = radius - (float) Math.sqrt(inside);
            float inset = Math.max(0f, Math.min(radius, rowInset));
            ctx.fill(new UiRect(rect.x + inset, y, Math.max(1f, rect.w - inset * 2f), rowH), color);
        }
    }

    private static UiRect inset(UiRect rect, float amount) {
        float inset = Math.max(0f, amount);
        return new UiRect(
                rect.x + inset,
                rect.y + inset,
                Math.max(1f, rect.w - inset * 2f),
                Math.max(1f, rect.h - inset * 2f)
        );
    }

    private static UiRect expand(UiRect rect, float amount) {
        float spread = Math.max(0f, amount);
        return new UiRect(
                rect.x - spread,
                rect.y - spread,
                rect.w + spread * 2f,
                rect.h + spread * 2f
        );
    }
}
