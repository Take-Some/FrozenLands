package dev.takesome.helix.ui.uiComponents.input;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Resolves text/caret layout for single-line input controls. */
final class UiInputTextLayout {
    private static final UiColor TEXT = new UiColor(1f, 0.91f, 0.68f, 1f);
    private static final UiColor PLACEHOLDER = new UiColor(1f, 0.91f, 0.68f, 0.52f);
    private static final UiColor CARET = new UiColor(1f, 0.82f, 0.32f, 1f);

    UiRect textBounds(UiRenderContext ctx, UiElementSkin element, UiRect bounds, boolean elementDrawn, float paddingX, float paddingY) {
        UiRect fallback = inset(bounds, paddingX, paddingY);
        if (!elementDrawn || ctx == null || element == null) return fallback;
        UiRect content = ctx.elementContentBounds(element, bounds);
        return content == null || content.w <= 0f || content.h <= 0f ? fallback : content;
    }

    void render(
            UiRenderContext ctx,
            UiRect bounds,
            String value,
            String placeholder,
            float fontScale,
            String fontId,
            boolean focused,
            boolean enabled,
            boolean caretVisible
    ) {
        if (ctx == null || bounds == null) return;
        String safeValue = emptyIfNull(value);
        String shown = safeValue.isEmpty() ? (emptyIfNull(placeholder)) : safeValue;
        UiColor color = safeValue.isEmpty() ? PLACEHOLDER : TEXT;
        if (!shown.isEmpty()) ctx.text(shown, bounds, fontScale, color, TextAlign.LEFT, fontId);

        if (focused && enabled && caretVisible) {
            float caretX = Math.min(bounds.right() - 2f, bounds.x + approximateTextWidth(safeValue, fontScale));
            ctx.fill(new UiRect(caretX, bounds.y + 4f, 2f, Math.max(0f, bounds.h - 8f)), CARET);
        }
    }

    private float approximateTextWidth(String text, float fontScale) {
        if (text == null || text.isEmpty()) return 0f;
        return Math.max(0f, text.length() * 8f * fontScale);
    }

    private UiRect inset(UiRect rect, float x, float y) {
        return new UiRect(rect.x + x, rect.y + y, Math.max(0f, rect.w - x * 2f), Math.max(0f, rect.h - y * 2f));
    }
}
