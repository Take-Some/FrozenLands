package dev.takesome.helix.ui.markup.internal.factory;


import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.i18n.I18nKey;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.markup.internal.i18n.UiDomI18nText;
import dev.takesome.helix.ui.markup.internal.action.UiMarkupActionBinder;
import dev.takesome.helix.ui.markup.internal.action.UiMarkupComponentEventBridge;
import dev.takesome.helix.ui.markup.internal.layout.UiDomLayoutResolver;
import dev.takesome.helix.ui.markup.internal.style.UiDomButtonSkinResolver;
import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.uiComponents.button.UiButtonNode;
import dev.takesome.helix.ui.uiComponents.button.UiButtonType;
import dev.takesome.helix.ui.node.Node;

import java.util.LinkedHashMap;
import java.util.Map;

/** Creates button markup nodes. */
public final class UiMarkupButtonFactory {
    private final UiMarkupActionBinder actions;
    private final UiDomStyleReader reader;
    private final UiDomLayoutResolver layout;
    private final UiDomButtonSkinResolver buttonSkins;
    private final EngineI18n i18n;

    public UiMarkupButtonFactory(
            UiMarkupActionBinder actions,
            UiDomStyleReader reader,
            UiDomLayoutResolver layout,
            UiDomButtonSkinResolver buttonSkins
    ) {
        this(actions, reader, layout, buttonSkins, null);
    }

    public UiMarkupButtonFactory(
            UiMarkupActionBinder actions,
            UiDomStyleReader reader,
            UiDomLayoutResolver layout,
            UiDomButtonSkinResolver buttonSkins,
            EngineI18n i18n
    ) {
        this.actions = actions;
        this.reader = reader;
        this.layout = layout;
        this.buttonSkins = buttonSkins;
        this.i18n = i18n;
    }

    public Node button(UiDomElement element, Map<String, String> style, float parentW, float parentH, UiDomRetainedNodeFactory nodes) {
        UiButtonNode node = new UiButtonNode(UiDomI18nText.textOrLocalized(element, "text", i18n));
        node.setButtonType(buttonType(style));
        node.setEventSink(new UiMarkupComponentEventBridge(actions, style, localizedEventArguments(style)));
        UiColor text = reader.color(style, "text-color", null);
        if (text == null) text = reader.color(style, "color", null);
        UiColor normal = reader.color(style, "button-color", null);
        if (normal == null) normal = reader.color(style, "background-color", null);
        if (normal == null) normal = reader.color(style, "background", null);
        node.setColors(normal, normal, normal, normal, text);
        UiColor border = reader.color(style, "border-color", reader.color(style, "borderColor", null));
        float borderWidth = reader.number(style, "border-width", reader.number(style, "borderWidth", 0f));
        if (border != null && borderWidth > 0f) node.setBorder(border, borderWidth);
        node.setFontId(reader.font(style));
        if (reader.has(style, "font-size")) node.setFontScale(Math.max(0.01f, reader.number(style, "font-size", 16f) / 16f));
        buttonSkins.applyButtonSkin(node, style);
        buttonSkins.applyButtonIcon(node, style);
        layout.setBounds(node, style, parentW, parentH, 380f, 46f);
        if (nodes != null && element != null && element.childCount() > 0) {
            nodes.compileChildren(node, element, node.bounds().w, node.bounds().h);
        }
        layout.applyState(node, style);
        return node;
    }

    private UiButtonType buttonType(Map<String, String> style) {
        return UiButtonType.fromMarkupType(reader.first(style, "type", "button-type", "buttonType", "data-type"));
    }

    private Map<String, String> localizedEventArguments(Map<String, String> style) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        addLocalizedArgument(out, style, "data-title-key", "data-title");
        addLocalizedArgument(out, style, "data-message-key", "data-message");
        addLocalizedArgument(out, style, "title-key", "title");
        addLocalizedArgument(out, style, "message-key", "message");
        return out;
    }

private void addLocalizedArgument(LinkedHashMap<String, String> out, Map<String, String> style, String keyAttribute, String outputKey) {
        if (style == null || keyAttribute == null || outputKey == null) return;
        if (out.containsKey(outputKey) && !out.getOrDefault(outputKey, "").isBlank()) return;
        String key = style.getOrDefault(keyAttribute, "").trim();
        if (key.isBlank()) return;
        String value = resolveMarkupTextKey(key);
        if (!value.isBlank()) out.put(outputKey, value);
    }

    private String resolveMarkupTextKey(String key) {
        if (key == null || key.isBlank()) return "";
        if (i18n == null) return key.trim();
        String resolved = i18n.resolve(I18nKey.of(key.trim()));
        if (resolved == null || resolved.isBlank()) return key.trim();
        return resolved.trim();
    }

}
