package dev.takesome.helix.ui.uiComponents.checkbox;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Checkbox drawing separated from state and input handling. */
final class CheckboxRenderer {
    private final UiCheckboxFallbackRenderer fallback = new UiCheckboxFallbackRenderer();

    void render(UiRenderContext ctx, UiRect bounds, String label, CheckboxState state, CheckboxStyle style, boolean enabled) {
        float size = Math.min(Math.min(bounds.h, style.boxSize), bounds.w);
        if (size <= 0f) return;
        UiRect box = new UiRect(bounds.x, bounds.y + (bounds.h - size) * 0.5f, size, size);
        UiElementSkin element = style.element(enabled, state);
        boolean drawn = element != null && ctx.drawElement(element, box, style.tint(enabled, state));
        if (!drawn) {
            fallback.render(ctx, box, enabled, state.hovered(), state.checked(), style.styledBoxColor,
                    style.styledInnerColor, style.styledCheckColor, style.styledBorderColor, style.styledBorderWidth);
        }
        if (!label.isBlank()) {
            float x = box.x + box.w + style.labelGap;
            UiRect text = new UiRect(x, bounds.y, Math.max(0f, bounds.right() - x), bounds.h);
            ctx.text(label, text, 1f, style.textColor(), TextAlign.LEFT, style.fontId);
        }
    }
}
