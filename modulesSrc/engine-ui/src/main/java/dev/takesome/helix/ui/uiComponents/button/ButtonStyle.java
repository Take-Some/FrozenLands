package dev.takesome.helix.ui.uiComponents.button;

import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.icons.UiIcon;

/** Mutable style payload for a retained button node. */
final class ButtonStyle {
    private static final UiColor ELEMENT_NORMAL_TINT = new UiColor(1.00f, 1.00f, 1.00f, 1.00f);
    private static final UiColor ELEMENT_HOVER_TINT = new UiColor(1.00f, 0.98f, 0.82f, 1.00f);
    private static final UiColor ELEMENT_PRESSED_TINT = new UiColor(0.82f, 0.82f, 0.82f, 1.00f);
    private static final UiColor ELEMENT_ACTIVE_TINT = new UiColor(0.72f, 0.88f, 1.00f, 1.00f);
    private static final UiColor ELEMENT_DISABLED_TINT = new UiColor(0.70f, 0.70f, 0.70f, 0.55f);
    private static final UiColor DEFAULT_HOVER_BORDER = new UiColor(1.00f, 0.82f, 0.28f, 0.92f);
    private static final UiColor DEFAULT_PRESSED_BORDER = new UiColor(1.00f, 0.94f, 0.48f, 1.00f);
    static final UiColor DEFAULT_HOVER_TEXT = new UiColor(1.00f, 0.92f, 0.58f, 1.00f);

    private final Runnable dirty;

    UiColor normalColor = new UiColor(0.12f, 0.12f, 0.15f, 0.96f);
    UiColor hoveredColor = new UiColor(0.20f, 0.20f, 0.25f, 0.98f);
    UiColor downColor = new UiColor(0.08f, 0.08f, 0.11f, 1.00f);
    UiColor activeColor = new UiColor(0.10f, 0.22f, 0.34f, 1.00f);
    UiColor disabledColor = new UiColor(0.06f, 0.06f, 0.07f, 0.55f);
    UiColor textColor = UiColor.WHITE;
    UiColor borderColor;
    UiColor hoveredBorderColor;
    UiColor pressedBorderColor;
    UiColor activeBorderColor;
    UiColor disabledBorderColor;
    float borderWidth;
    float fontScale = 1f;
    String fontId;

    UiIcon icon;
    float iconScale = 0.80f;
    float iconSize = 22f;
    float iconGap = 8f;
    float iconInset = 16f;

    UiElementSkin normalElement;
    UiElementSkin hoveredElement;
    UiElementSkin pressedElement;
    UiElementSkin activeElement;
    UiElementSkin disabledElement;

    ButtonStyle(Runnable dirty) {
        this.dirty = dirty == null ? () -> {} : dirty;
    }

    void setColors(UiColor normalColor, UiColor hoveredColor, UiColor downColor, UiColor disabledColor, UiColor textColor) {
        if (normalColor != null) this.normalColor = normalColor;
        if (hoveredColor != null) this.hoveredColor = hoveredColor;
        if (downColor != null) this.downColor = downColor;
        if (disabledColor != null) this.disabledColor = disabledColor;
        if (textColor != null) this.textColor = textColor;
        dirty.run();
    }

    void setActiveColor(UiColor activeColor) {
        if (activeColor != null) this.activeColor = activeColor;
        dirty.run();
    }

    void setBorder(UiColor borderColor, float borderWidth) {
        this.borderColor = borderColor;
        this.borderWidth = Float.isFinite(borderWidth) ? Math.max(0f, borderWidth) : 0f;
        dirty.run();
    }

    void setStateBorders(UiColor hoveredBorderColor, UiColor pressedBorderColor, UiColor activeBorderColor, UiColor disabledBorderColor) {
        if (hoveredBorderColor != null) this.hoveredBorderColor = hoveredBorderColor;
        if (pressedBorderColor != null) this.pressedBorderColor = pressedBorderColor;
        if (activeBorderColor != null) this.activeBorderColor = activeBorderColor;
        if (disabledBorderColor != null) this.disabledBorderColor = disabledBorderColor;
        dirty.run();
    }

    void setElements(UiElementSkin normalElement, UiElementSkin hoveredElement, UiElementSkin pressedElement, UiElementSkin disabledElement) {
        this.normalElement = normalElement;
        this.hoveredElement = hoveredElement;
        this.pressedElement = pressedElement;
        this.disabledElement = disabledElement;
        dirty.run();
    }

    void setActiveElement(UiElementSkin activeElement) {
        this.activeElement = activeElement;
        dirty.run();
    }

    void setFontScale(float fontScale) {
        if (!Float.isFinite(fontScale) || fontScale <= 0f) return;
        this.fontScale = fontScale;
        dirty.run();
    }

    void setFontId(String fontId) {
        this.fontId = fontId == null || fontId.isBlank() ? null : fontId.trim();
        dirty.run();
    }

    void setIcon(UiIcon icon, float iconScale, float iconSize, float iconGap, float iconInset) {
        this.icon = icon;
        if (Float.isFinite(iconScale) && iconScale > 0f) this.iconScale = iconScale;
        if (Float.isFinite(iconSize) && iconSize > 0f) this.iconSize = iconSize;
        if (Float.isFinite(iconGap) && iconGap >= 0f) this.iconGap = iconGap;
        if (Float.isFinite(iconInset) && iconInset >= 0f) this.iconInset = iconInset;
        dirty.run();
    }

    UiColor elementTint(ButtonState state, boolean enabled) {
        if (!enabled) return ELEMENT_DISABLED_TINT;
        UiColor tint = ELEMENT_NORMAL_TINT;
        tint = mix(tint, ELEMENT_HOVER_TINT, state.hoverTransition());
        tint = mix(tint, ELEMENT_ACTIVE_TINT, state.activeTransition());
        tint = mix(tint, ELEMENT_PRESSED_TINT, state.pressTransition());
        return tint;
    }

    UiColor backgroundColor(ButtonState state, boolean enabled) {
        if (!enabled) return disabledColor;
        UiColor color = normalColor;
        color = mix(color, hoveredColor, state.hoverTransition());
        color = mix(color, activeColor, state.activeTransition());
        color = mix(color, downColor, state.pressTransition());
        return color;
    }

    UiColor borderColor(ButtonState state, boolean enabled) {
        if (!enabled) return fallback(disabledBorderColor);
        UiColor color = borderColor;
        color = mixNullable(color, defaultIfMissing(fallback(hoveredBorderColor), DEFAULT_HOVER_BORDER), state.hoverTransition());
        color = mixNullable(color, fallback(activeBorderColor), state.activeTransition());
        color = mixNullable(color, defaultIfMissing(fallback(pressedBorderColor), DEFAULT_PRESSED_BORDER), state.pressTransition());
        return color;
    }

    float effectiveBorderWidth(ButtonState state) {
        float animated = Math.max(state.hoverTransition() * 1.65f, state.pressTransition() * 2.15f);
        return Math.max(borderWidth, animated);
    }

    UiColor resolvedTextColor(ButtonState state, boolean enabled) {
        if (!enabled) return textColor.withAlpha(textColor.a * 0.62f);
        UiColor color = textColor;
        color = mix(color, DEFAULT_HOVER_TEXT.withAlpha(textColor.a), state.hoverTransition());
        color = mix(color, UiColor.WHITE.withAlpha(textColor.a), state.pressTransition() * 0.55f);
        return color;
    }

    UiElementSkin backgroundElement(ButtonState state, boolean enabled) {
        if (!enabled) return disabledElement != null ? disabledElement : normalElement;
        if (state.pressed()) return pressedElement != null ? pressedElement : normalElement;
        if (state.active()) return activeElement != null ? activeElement : normalElement;
        if (state.hovered()) return hoveredElement != null ? hoveredElement : normalElement;
        return normalElement;
    }

    private UiColor fallback(UiColor color) { return color == null ? borderColor : color; }
    private UiColor defaultIfMissing(UiColor value, UiColor defaultValue) { return value == null ? defaultValue : value; }

    private static UiColor mixNullable(UiColor from, UiColor to, float amount) {
        if (from == null) return to == null ? null : to.withAlpha(to.a * amount);
        if (to == null) return from;
        return mix(from, to, amount);
    }

    private static UiColor mix(UiColor from, UiColor to, float amount) {
        if (from == null) return to;
        if (to == null) return from;
        float t = Math.max(0f, Math.min(1f, amount));
        return new UiColor(
                from.r + (to.r - from.r) * t,
                from.g + (to.g - from.g) * t,
                from.b + (to.b - from.b) * t,
                from.a + (to.a - from.a) * t
        );
    }
}
