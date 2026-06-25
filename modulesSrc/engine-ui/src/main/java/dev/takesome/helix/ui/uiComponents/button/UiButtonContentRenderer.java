package dev.takesome.helix.ui.uiComponents.button;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Renders button label/icon content independently from button state/input. */
final class UiButtonContentRenderer {
    UiRect contentBounds(UiRenderContext ctx, UiElementSkin element, UiRect bounds) {
        if (ctx == null || element == null) return bounds;
        UiRect content = ctx.elementContentBounds(element, bounds);
        return content == null || content.w <= 0f || content.h <= 0f ? bounds : content;
    }

    void render(
            UiRenderContext ctx,
            UiRect bounds,
            String label,
            UiIcon icon,
            float iconScale,
            float iconSize,
            float iconGap,
            float iconInset,
            UiColor textColor,
            float fontScale,
            String fontId
    ) {
        if (ctx == null || bounds == null) return;

        boolean hasLabel = label != null && !label.isBlank();
        boolean iconDrawn = false;
        if (icon != null && ctx.supportsIcons()) {
            float safeIconSize = Math.min(iconSize, Math.max(0f, bounds.h));
            UiRect iconRect = hasLabel
                    ? new UiRect(bounds.x + iconInset, bounds.y + (bounds.h - safeIconSize) * 0.5f, safeIconSize, safeIconSize)
                    : new UiRect(bounds.x + (bounds.w - safeIconSize) * 0.5f, bounds.y + (bounds.h - safeIconSize) * 0.5f, safeIconSize, safeIconSize);
            iconDrawn = ctx.icon(icon, iconRect, iconScale, textColor, TextAlign.CENTER);
        }

        if (!hasLabel) return;

        UiRect labelBounds = bounds;
        if (iconDrawn) {
            float shift = Math.max(0f, iconInset + iconSize + iconGap * 0.5f);
            labelBounds = new UiRect(bounds.x + shift, bounds.y, Math.max(1f, bounds.w - shift - iconGap), bounds.h);
        }

        ctx.buttonText(label, labelBounds, fontScale, textColor, TextAlign.CENTER, fontId);
    }
}
