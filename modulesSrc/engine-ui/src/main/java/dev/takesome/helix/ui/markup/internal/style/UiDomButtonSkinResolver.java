package dev.takesome.helix.ui.markup.internal.style;

import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.uiComponents.button.UiButtonNode;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.icons.fontawesome.FontAwesomeStyle;

import java.util.Locale;
import java.util.Map;

/** Button-specific skin/icon taxonomy resolution. */
public final class UiDomButtonSkinResolver {
    private final UiDomStyleReader reader;

    public UiDomButtonSkinResolver(UiDomStyleReader reader) {
        this.reader = reader;
    }

    public void applyButtonIcon(UiButtonNode node, Map<String, String> style) {
        UiIcon icon = icon(style);
        if (icon == null) return;
        node.setIcon(
                icon,
                reader.number(style, "icon-scale", reader.number(style, "iconScale", 0.80f)),
                reader.number(style, "icon-size", reader.number(style, "iconSize", 22f)),
                reader.number(style, "icon-gap", reader.number(style, "iconGap", 8f)),
                reader.number(style, "icon-inset", reader.number(style, "iconInset", 16f))
        );
    }

    public void applyButtonSkin(UiButtonNode node, Map<String, String> style) {
        ButtonSkin taxonomySkin = taxonomyButtonSkin(buttonStyle(style));
        if (taxonomySkin != null) {
            node.setElements(
                    UiElementSkin.of("button", taxonomySkin.normal),
                    UiElementSkin.of("button", taxonomySkin.hover),
                    UiElementSkin.of("button", taxonomySkin.pressed),
                    UiElementSkin.of("button", taxonomySkin.disabled)
            );
            return;
        }

        String normal = reader.first(style, "skin", "ui-skin");
        if (normal.isBlank()) return;
        UiElementSkin element = UiElementSkin.of("button", normal);
        node.setElements(element, element, element, element);
    }

    private UiIcon icon(Map<String, String> style) {
        String value = reader.first(style, "data-icon", "icon", "fa", "font-awesome", "fontAwesome");
        if (value.isBlank()) return null;
        return FontAwesomeStyle.SOLID.registry().find(value).orElse(null);
    }

    private String buttonStyle(Map<String, String> style) {
        String explicit = reader.first(style, "button-style", "buttonStyle");
        if (!explicit.isBlank()) return explicit;

        String skin = reader.first(style, "skin", "ui-skin");
        return taxonomyStyleId(skin) ? skin : "";
    }

    private boolean taxonomyStyleId(String value) {
        return value != null && value.trim().toLowerCase(Locale.ROOT).startsWith("button.");
    }

    private ButtonSkin taxonomyButtonSkin(String styleId) {
        if (styleId == null || styleId.isBlank()) return null;
        String[] parts = styleId.trim().toLowerCase(Locale.ROOT).split("\\.");
        if (parts.length != 4 || !"button".equals(parts[0])) return null;
        String size = parts[1];
        String shape = parts[2];
        String color = parts[3];
        if (!safeTaxonomySegment(size) || !safeTaxonomySegment(shape) || !safeTaxonomySegment(color)) return null;

        String base = "ui/buttons/" + size + "/" + shape + "/" + color + "/";
        return new ButtonSkin(
                base + "normal.png",
                base + "hover.png",
                base + "pressed.png",
                base + "disabled.png"
        );
    }

    private boolean safeTaxonomySegment(String value) {
        if (value == null || value.isBlank()) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') continue;
            return false;
        }
        return true;
    }

    private static final class ButtonSkin {
        final String normal;
        final String hover;
        final String pressed;
        final String disabled;

        ButtonSkin(String normal, String hover, String pressed, String disabled) {
            this.normal = normal;
            this.hover = hover;
            this.pressed = pressed;
            this.disabled = disabled;
        }
    }
}
