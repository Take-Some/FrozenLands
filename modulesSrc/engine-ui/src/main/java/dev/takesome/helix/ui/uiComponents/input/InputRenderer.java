package dev.takesome.helix.ui.uiComponents.input;

import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.render.UiRenderContext;
import dev.takesome.helix.ui.uiComponents.common.UiInteractiveState;
import dev.takesome.helix.ui.uiComponents.common.UiStatefulSkinResolver;

/** Input field renderer and visual style state. */
final class InputRenderer {
    private static final UiColor NORMAL_TINT = new UiColor(1f, 1f, 1f, 1f);
    private static final UiColor HOVER_TINT = new UiColor(1f, 0.96f, 0.78f, 1f);
    private static final UiColor FOCUSED_TINT = new UiColor(1f, 0.88f, 0.55f, 1f);
    private static final UiColor DISABLED_TINT = new UiColor(0.68f, 0.68f, 0.68f, 0.55f);
    private static final UiColor FALLBACK_NORMAL = new UiColor(0.08f, 0.14f, 0.16f, 0.92f);
    private static final UiColor FALLBACK_HOVER = new UiColor(0.11f, 0.24f, 0.28f, 0.96f);
    private static final UiColor FALLBACK_FOCUSED = new UiColor(0.13f, 0.27f, 0.32f, 0.98f);
    private static final UiColor FALLBACK_DISABLED = new UiColor(0.07f, 0.08f, 0.09f, 0.48f);

    private final Runnable dirty;
    private final UiStatefulSkinResolver skins = new UiStatefulSkinResolver();
    private final UiInputTextLayout textLayout = new UiInputTextLayout();
    private String fontId = AssetProvider.FONT_STANDART;
    private float fontScale = 1f;
    private float paddingX = 18f;
    private float paddingY = 6f;
    private UiElementSkin normalElement;
    private UiElementSkin hoverElement;
    private UiElementSkin focusedElement;
    private UiElementSkin disabledElement;

    InputRenderer(Runnable dirty) { this.dirty = dirty == null ? () -> {} : dirty; }

    void setFontScale(float fontScale) {
        if (!Float.isFinite(fontScale) || fontScale <= 0f) return;
        this.fontScale = fontScale;
        dirty.run();
    }

    void setPadding(float paddingX, float paddingY) {
        if (Float.isFinite(paddingX) && paddingX >= 0f) this.paddingX = paddingX;
        if (Float.isFinite(paddingY) && paddingY >= 0f) this.paddingY = paddingY;
        dirty.run();
    }

    void setFontId(String fontId) {
        this.fontId = fontId == null || fontId.isBlank() ? AssetProvider.FONT_STANDART : fontId.trim();
        dirty.run();
    }

    void setElements(UiElementSkin normalElement, UiElementSkin hoverElement, UiElementSkin focusedElement, UiElementSkin disabledElement) {
        this.normalElement = normalElement;
        this.hoverElement = hoverElement;
        this.focusedElement = focusedElement;
        this.disabledElement = disabledElement;
        dirty.run();
    }

    void render(UiRenderContext ctx, UiRect bounds, InputTextModel model, UiInteractiveState state, boolean focused, boolean enabled, boolean caretVisible) {
        UiElementSkin element = skins.focusElement(enabled, state, normalElement, hoverElement, focusedElement, disabledElement);
        boolean drawn = element != null && ctx.drawElement(element, bounds, tint(enabled, state));
        if (!drawn) ctx.fill(bounds, fallbackColor(enabled, state));
        UiRect textBounds = textLayout.textBounds(ctx, element, bounds, drawn, paddingX, paddingY);
        textLayout.render(ctx, textBounds, model.value(), model.placeholder(), fontScale, fontId, focused, enabled, caretVisible);
    }

    private UiColor tint(boolean enabled, UiInteractiveState state) {
        return skins.focusColor(enabled, state, NORMAL_TINT, HOVER_TINT, FOCUSED_TINT, DISABLED_TINT);
    }

    private UiColor fallbackColor(boolean enabled, UiInteractiveState state) {
        return skins.focusColor(enabled, state, FALLBACK_NORMAL, FALLBACK_HOVER, FALLBACK_FOCUSED, FALLBACK_DISABLED);
    }
}
