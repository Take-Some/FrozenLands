package dev.takesome.helix.ui.uiComponents.combo;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.icons.UiIconRegistries;
import dev.takesome.helix.ui.icons.registry.IconRegistry;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Combo-box closed and dropdown overlay renderer. */
final class ComboBoxRenderer {
    private static final UiColor NORMAL = new UiColor(0.08f, 0.12f, 0.15f, 0.96f);
    private static final UiColor HOVER = new UiColor(0.11f, 0.20f, 0.25f, 0.98f);
    private static final UiColor OPEN = new UiColor(0.13f, 0.22f, 0.28f, 0.98f);
    private static final UiColor OPTION = new UiColor(0.06f, 0.10f, 0.12f, 0.98f);
    private static final UiColor OPTION_HOVER = new UiColor(0.18f, 0.24f, 0.27f, 0.98f);
    private static final UiColor DISABLED = new UiColor(0.07f, 0.07f, 0.08f, 0.50f);
    private static final UiColor TEXT = new UiColor(1.00f, 0.93f, 0.75f, 1.00f);
    private static final UiColor ICON = new UiColor(1.00f, 0.78f, 0.36f, 0.96f);
    private static final IconRegistry ICONS = UiIconRegistries.standard();

    void render(UiRenderContext ctx, UiRect b, ComboBoxModel model, ComboBoxInputController state, boolean enabled,
                String closedIconId, String openIconId, UiColor styledBackgroundColor, UiColor styledTextColor,
                UiColor styledIconColor, UiColor styledBorderColor, float styledBorderWidth) {
        ctx.fill(b, backgroundColor(enabled, state.open(), state.hovered(), styledBackgroundColor));
        ctx.stroke(b, borderColor(state.open(), styledBorderColor), borderWidth(styledBorderWidth));
        ctx.text(model.selectedLabel(), new UiRect(b.x + 12f, b.y + 4f, Math.max(1f, b.w - 42f), Math.max(1f, b.h - 8f)), 0.76f, textColor(styledTextColor), TextAlign.LEFT, "standard");
        UiIcon arrow = icon(state.open(), closedIconId, openIconId);
        if (arrow != null && ctx.supportsIcons()) ctx.icon(arrow, new UiRect(b.x + b.w - 31f, b.y + 7f, 20f, 20f), 0.46f, iconColor(styledIconColor), TextAlign.CENTER);
        else ctx.text(state.open() ? "▲" : "▼", new UiRect(b.x + b.w - 32f, b.y + 4f, 26f, Math.max(1f, b.h - 8f)), 0.62f, textColor(styledTextColor), TextAlign.CENTER, "standard");
    }

    void renderOptions(UiRenderContext ctx, UiRect base, ComboBoxModel model, int hoveredOption) {
        for (int i = 0; i < model.size(); i++) {
            UiComboBoxOption option = model.option(i);
            UiRect r = optionRect(base, i);
            ctx.fill(r, i == hoveredOption ? OPTION_HOVER : OPTION);
            UiColor color = option.disabled() ? TEXT.withAlpha(0.42f) : TEXT;
            UiIcon optionIcon = ICONS.find(option.value().equals(model.value()) ? "check" : "circle-info").orElse(null);
            float textX = r.x + 12f;
            if (optionIcon != null && ctx.supportsIcons()) {
                ctx.icon(optionIcon, new UiRect(r.x + 9f, r.y + 7f, 18f, 18f), 0.40f, option.disabled() ? ICON.withAlpha(0.35f) : ICON, TextAlign.CENTER);
                textX = r.x + 34f;
            }
            ctx.text(option.label(), new UiRect(textX, r.y + 4f, Math.max(1f, r.w - (textX - r.x) - 12f), Math.max(1f, r.h - 8f)), 0.72f, color, TextAlign.LEFT, "standard");
            ctx.stroke(r, new UiColor(0.55f, 0.42f, 0.23f, 0.35f), 1f);
        }
    }

    static UiRect optionRect(UiRect base, int index) {
        float h = Math.max(28f, base.h);
        return new UiRect(base.x, base.y - h * (index + 1), base.w, h);
    }

    static int optionIndexAt(float x, float y, UiRect base, ComboBoxModel model, boolean open) {
        if (!open) return -1;
        for (int i = 0; i < model.size(); i++) {
            UiRect r = optionRect(base, i);
            if (x >= r.x && x <= r.x + r.w && y >= r.y && y <= r.y + r.h) return i;
        }
        return -1;
    }

    private UiIcon icon(boolean open, String closedIconId, String openIconId) {
        UiIcon icon = ICONS.find(open ? openIconId : closedIconId).orElse(null);
        if (icon != null) return icon;
        return ICONS.find(open ? "chevron-up" : "chevron-down").orElse(null);
    }

    private UiColor backgroundColor(boolean enabled, boolean open, boolean hovered, UiColor styledBackgroundColor) {
        if (styledBackgroundColor != null) return styledBackgroundColor;
        return !enabled ? DISABLED : open ? OPEN : hovered ? HOVER : NORMAL;
    }

    private UiColor borderColor(boolean open, UiColor styledBorderColor) {
        if (styledBorderColor != null) return styledBorderColor;
        return new UiColor(0.82f, 0.63f, 0.32f, open ? 0.92f : 0.55f);
    }

    private float borderWidth(float styledBorderWidth) { return styledBorderWidth >= 0f ? styledBorderWidth : 1f; }
    private UiColor textColor(UiColor styledTextColor) { return styledTextColor != null ? styledTextColor : TEXT; }
    private UiColor iconColor(UiColor styledIconColor) { return styledIconColor != null ? styledIconColor : ICON; }
}
