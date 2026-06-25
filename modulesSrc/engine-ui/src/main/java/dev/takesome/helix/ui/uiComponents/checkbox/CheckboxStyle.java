package dev.takesome.helix.ui.uiComponents.checkbox;

import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;

/** Mutable visual style for checkbox nodes. */
final class CheckboxStyle {
    static final UiColor NORMAL_TINT = new UiColor(1f, 1f, 1f, 1f);
    static final UiColor HOVER_TINT = new UiColor(1f, 0.96f, 0.76f, 1f);
    static final UiColor DISABLED_TINT = new UiColor(0.74f, 0.74f, 0.74f, 0.55f);
    static final UiColor LABEL = new UiColor(1.0f, 0.90f, 0.68f, 1f);

    private final Runnable dirty;
    float boxSize = 32f;
    float labelGap = 10f;
    String fontId;
    UiColor styledBoxColor;
    UiColor styledInnerColor;
    UiColor styledCheckColor;
    UiColor styledTextColor;
    UiColor styledBorderColor;
    float styledBorderWidth = -1f;
    UiElementSkin checkedElement;
    UiElementSkin uncheckedElement;
    UiElementSkin hoverElement;
    UiElementSkin disabledElement;

    CheckboxStyle(Runnable dirty) { this.dirty = dirty == null ? () -> {} : dirty; }

    void setBoxSize(float boxSize) {
        if (!Float.isFinite(boxSize) || boxSize <= 0f) return;
        this.boxSize = boxSize;
        dirty.run();
    }

    void setLabelGap(float labelGap) {
        if (!Float.isFinite(labelGap) || labelGap < 0f) return;
        this.labelGap = labelGap;
        dirty.run();
    }

    void setFontId(String fontId) {
        this.fontId = fontId == null || fontId.isBlank() ? null : fontId.trim();
        dirty.run();
    }

    void setElements(UiElementSkin checkedElement, UiElementSkin uncheckedElement, UiElementSkin hoverElement, UiElementSkin disabledElement) {
        this.checkedElement = checkedElement;
        this.uncheckedElement = uncheckedElement;
        this.hoverElement = hoverElement;
        this.disabledElement = disabledElement;
        dirty.run();
    }

    void setStyleColors(UiColor box, UiColor inner, UiColor check, UiColor text, UiColor border, float borderWidth) {
        this.styledBoxColor = box;
        this.styledInnerColor = inner;
        this.styledCheckColor = check;
        this.styledTextColor = text;
        this.styledBorderColor = border;
        this.styledBorderWidth = Float.isFinite(borderWidth) ? borderWidth : -1f;
        dirty.run();
    }

    UiElementSkin element(boolean enabled, CheckboxState state) {
        if (!enabled) return disabledElement != null ? disabledElement : uncheckedElement;
        if (state.hovered() && hoverElement != null) return hoverElement;
        return state.checked() ? checkedElement : uncheckedElement;
    }

    UiColor tint(boolean enabled, CheckboxState state) {
        if (!enabled) return DISABLED_TINT;
        if (state.hovered()) return HOVER_TINT;
        return NORMAL_TINT;
    }

    UiColor textColor() { return styledTextColor != null ? styledTextColor : LABEL; }
}
