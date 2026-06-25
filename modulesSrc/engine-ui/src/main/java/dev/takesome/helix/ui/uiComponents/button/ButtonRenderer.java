package dev.takesome.helix.ui.uiComponents.button;

import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Button drawing isolated from retained input/state mutation. */
final class ButtonRenderer {
    private static final UiColor DEFAULT_HOVER_WASH = new UiColor(1.00f, 0.72f, 0.18f, 0.16f);
    private static final UiColor DEFAULT_HOVER_SHINE = new UiColor(1.00f, 0.94f, 0.54f, 0.34f);

    private final UiButtonContentRenderer content;

    ButtonRenderer(UiButtonContentRenderer content) {
        this.content = content;
    }

    void render(UiRenderContext ctx, UiRect bounds, String label, ButtonState state, ButtonStyle style, boolean enabled) {
        UiElementSkin element = style.backgroundElement(state, enabled);
        boolean drawn = drawTransitionedElement(ctx, bounds, state, style, enabled, element);
        if (!drawn) ctx.fill(bounds, style.backgroundColor(state, enabled));
        renderHoverAffordance(ctx, bounds, state, enabled);
        UiColor border = style.borderColor(state, enabled);
        float strokeWidth = style.effectiveBorderWidth(state);
        if (border != null && strokeWidth > 0f && border.a > 0f) ctx.stroke(bounds, border, strokeWidth);
        content.render(ctx, content.contentBounds(ctx, element, bounds), label, style.icon, style.iconScale,
                style.iconSize, style.iconGap, style.iconInset, style.resolvedTextColor(state, enabled), style.fontScale, style.fontId);
    }

    UiRect contentBounds(UiRenderContext ctx, UiRect bounds, ButtonState state, ButtonStyle style, boolean enabled) {
        return content.contentBounds(ctx, style.backgroundElement(state, enabled), bounds);
    }

    private boolean drawTransitionedElement(UiRenderContext ctx, UiRect bounds, ButtonState state,
                                            ButtonStyle style, boolean enabled, UiElementSkin element) {
        if (!enabled) {
            UiElementSkin disabled = style.disabledElement != null ? style.disabledElement : style.normalElement;
            return disabled != null && ctx.drawElement(disabled, bounds, new UiColor(0.70f, 0.70f, 0.70f, 0.55f));
        }
        UiElementSkin base = style.normalElement != null ? style.normalElement : element;
        if (base == null) return false;
        boolean drawn = ctx.drawElement(base, bounds, style.elementTint(state, true));
        if (drawn) {
            drawn |= drawElementLayer(ctx, bounds, style.hoveredElement, state.hoverTransition() * (1f - state.pressTransition()) * (1f - state.activeTransition()));
            drawn |= drawElementLayer(ctx, bounds, style.activeElement, state.activeTransition() * (1f - state.pressTransition()));
            drawn |= drawElementLayer(ctx, bounds, style.pressedElement, state.pressTransition());
        }
        return drawn;
    }

    private boolean drawElementLayer(UiRenderContext ctx, UiRect bounds, UiElementSkin element, float amount) {
        if (element == null || amount <= 0.001f) return false;
        return ctx.drawElement(element, bounds, new UiColor(1f, 1f, 1f, amount));
    }

    private void renderHoverAffordance(UiRenderContext ctx, UiRect bounds, ButtonState state, boolean enabled) {
        if (!enabled) return;
        float hover = state.hoverTransition() * (1f - state.pressTransition() * 0.45f);
        if (hover <= 0.001f) return;
        UiRect wash = new UiRect(bounds.x + 2f, bounds.y + 2f, Math.max(1f, bounds.w - 4f), Math.max(1f, bounds.h - 4f));
        ctx.fill(wash, DEFAULT_HOVER_WASH.withAlpha(DEFAULT_HOVER_WASH.a * hover));
        UiRect shine = new UiRect(bounds.x + 5f, bounds.y + Math.max(0f, bounds.h - 7f), Math.max(1f, bounds.w - 10f), 3f);
        ctx.fill(shine, DEFAULT_HOVER_SHINE.withAlpha(DEFAULT_HOVER_SHINE.a * hover));
    }
}
