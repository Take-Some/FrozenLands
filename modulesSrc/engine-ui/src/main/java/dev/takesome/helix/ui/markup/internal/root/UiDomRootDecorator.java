package dev.takesome.helix.ui.markup.internal.root;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.components.UiAnimatedPanelNode;
import dev.takesome.helix.ui.components.UiAnimatedSpriteNode;
import dev.takesome.helix.ui.components.UiElementNode;
import dev.takesome.helix.ui.uiComponents.panel.UiPanelNode;
import dev.takesome.helix.ui.components.sprite.UiSpriteAnimationSpec;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.node.ContainerNode;

import java.util.Map;

/** Applies document-root visuals: popup dim overlay, background fill and background image. */
public final class UiDomRootDecorator {
    private static final UiColor POPUP_DIM = new UiColor(0f, 0f, 0f, 0.58f);

    private final UiDomStyleReader reader;

    public UiDomRootDecorator(UiDomStyleReader reader) {
        this.reader = reader;
    }

    public void addRootBackground(ContainerNode container, UiDomElement root, Map<String, String> style, float width, float height) {
        if ("dim".equalsIgnoreCase(reader.first(style, "data-overlay", "overlay"))) {
            UiColor overlayColor = reader.color(style, "data-overlay-color", reader.color(style, "overlay-color", POPUP_DIM));
            float fadeMs = reader.number(style, "overlay-fade-ms", reader.number(style, "overlayFadeMs", reader.number(style, "modal-fade-ms", 0f)));
            UiPanelNode overlay = new UiAnimatedPanelNode(overlayColor, fadeMs / 1000f);
            overlay.setBounds(0f, 0f, width, height);
            container.add(overlay);
        }

        UiColor background = reader.color(style, "background-color", null);
        if (background == null) background = reader.color(style, "background", null);
        if (background != null) {
            UiPanelNode fill = new UiPanelNode(background);
            fill.setBounds(0f, 0f, width, height);
            container.add(fill);
        }

        String skin = backgroundImageReference(style);
        if (!skin.isBlank()) {
            if (animatedBackground(style, skin)) {
                UiAnimatedSpriteNode image = new UiAnimatedSpriteNode(backgroundAnimationSpec(style, skin));
                image.setBounds(0f, 0f, width, height);
                container.add(image);
            } else {
                UiElementNode image = new UiElementNode(UiElementSkin.of("image", skin));
                image.setBounds(0f, 0f, width, height);
                container.add(image);
            }
        }
    }

    private UiSpriteAnimationSpec backgroundAnimationSpec(Map<String, String> style, String source) {
        return UiSpriteAnimationSpec.builder(source)
                .grid(
                        reader.integer(style, "background-sprite-columns", 1),
                        reader.integer(style, "background-sprite-rows", 1)
                )
                .frameCount(reader.integer(style, "background-sprite-frames", reader.integer(style, "background-frame-count", 1)))
                .fps(reader.number(style, "background-sprite-fps", reader.number(style, "background-animation-fps", 10f)))
                .startFrame(reader.integer(style, "background-sprite-start-frame", reader.integer(style, "background-start-frame", 0)))
                .mode(reader.first(style, "background-sprite-mode", "background-animation-mode"))
                .loop(reader.bool(style, "background-sprite-loop", reader.bool(style, "background-loop", true)))
                .pingPong(reader.bool(style, "background-sprite-ping-pong", reader.bool(style, "background-ping-pong", false)))
                .autoplay(reader.bool(style, "background-sprite-autoplay", reader.bool(style, "background-autoplay", true)))
                .maxDeltaSeconds(reader.number(style, "background-sprite-max-delta", 0.25f))
                .build();
    }

    private boolean animatedBackground(Map<String, String> style, String source) {
        if (reader.bool(style, "background-animated", false)) return true;
        if (reader.bool(style, "background-animation", false)) return true;
        return source != null && source.trim().toLowerCase(java.util.Locale.ROOT).endsWith(".sprite");
    }

    private String backgroundImageReference(Map<String, String> style) {
        String raw = reader.first(style, "background-skin", "backgroundSkin", "background-image", "backgroundImage");
        return cssUrlReference(raw);
    }

    private static String cssUrlReference(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String value = unquote(raw.trim());
        if (value.isBlank() || "none".equalsIgnoreCase(value)) {
            return "";
        }

        int open = value.indexOf('(');
        int close = value.lastIndexOf(')');
        if (open > 0 && close > open && "url".equalsIgnoreCase(value.substring(0, open).trim())) {
            return unquote(value.substring(open + 1, close).trim());
        }
        return value;
    }

    private static String unquote(String value) {
        if (value == null || value.length() < 2) {
            return trimToEmpty(value);
        }
        String trimmed = value.trim();
        char first = trimmed.charAt(0);
        char last = trimmed.charAt(trimmed.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }
}
