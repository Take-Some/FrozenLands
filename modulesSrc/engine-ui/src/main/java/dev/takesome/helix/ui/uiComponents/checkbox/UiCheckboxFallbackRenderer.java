package dev.takesome.helix.ui.uiComponents.checkbox;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.icons.UiIconRegistries;
import dev.takesome.helix.ui.icons.registry.IconRegistry;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Primitive checkbox fallback renderer used when no external skin is available. */
final class UiCheckboxFallbackRenderer {
    private static final UiColor BOX_NORMAL = new UiColor(0.12f, 0.16f, 0.19f, 0.92f);
    private static final UiColor BOX_HOVER = new UiColor(0.18f, 0.31f, 0.35f, 0.96f);
    private static final UiColor BOX_DISABLED = new UiColor(0.09f, 0.09f, 0.10f, 0.52f);
    private static final UiColor CHECK = new UiColor(1.0f, 0.82f, 0.32f, 1f);
    private static final IconRegistry ICONS = UiIconRegistries.standard();

    void render(
            UiRenderContext ctx,
            UiRect box,
            boolean enabled,
            boolean hovered,
            boolean checked,
            UiColor boxColor,
            UiColor innerColor,
            UiColor checkColor,
            UiColor borderColor,
            float borderWidth
    ) {
        if (ctx == null || box == null) return;
        drawBox(ctx, box, enabled, hovered, boxColor, innerColor, borderColor, borderWidth);
        if (checked) drawCheck(ctx, box, checkColor);
    }

    private void drawBox(
            UiRenderContext ctx,
            UiRect box,
            boolean enabled,
            boolean hovered,
            UiColor boxColor,
            UiColor innerColor,
            UiColor borderColor,
            float borderWidth
    ) {
        UiColor color = boxColor != null ? boxColor : !enabled ? BOX_DISABLED : hovered ? BOX_HOVER : BOX_NORMAL;
        ctx.fill(box, color);
        if (borderColor != null && borderWidth > 0f) ctx.stroke(box, borderColor, borderWidth);
        float inset = Math.max(2f, box.w * 0.12f);
        ctx.fill(
                new UiRect(box.x + inset, box.y + inset, Math.max(0f, box.w - inset * 2f), Math.max(0f, box.h - inset * 2f)),
                innerColor != null ? innerColor : new UiColor(0.02f, 0.03f, 0.035f, enabled ? 0.82f : 0.38f)
        );
    }

    private void drawCheck(UiRenderContext ctx, UiRect box, UiColor checkColor) {
        UiColor color = checkColor != null ? checkColor : CHECK;
        UiIcon icon = ICONS.find("check").orElse(null);
        if (icon != null && ctx.supportsIcons() && ctx.icon(icon, inset(box, 5f), 0.50f, color, TextAlign.CENTER)) return;
        float t = Math.max(2f, box.w * 0.13f);
        ctx.fill(new UiRect(box.x + box.w * 0.25f, box.y + box.h * 0.43f, box.w * 0.23f, t), color);
        ctx.fill(new UiRect(box.x + box.w * 0.43f, box.y + box.h * 0.28f, t, box.h * 0.36f), color);
        ctx.fill(new UiRect(box.x + box.w * 0.51f, box.y + box.h * 0.60f, box.w * 0.28f, t), color);
    }

    private UiRect inset(UiRect box, float value) {
        return new UiRect(box.x + value, box.y + value, Math.max(1f, box.w - value * 2f), Math.max(1f, box.h - value * 2f));
    }
}
