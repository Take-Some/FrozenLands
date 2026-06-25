package dev.takesome.helix.ui.markup.internal.factory;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.events.bus.EventBus;
import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.uiComponents.panel.UiPanelNode;
import dev.takesome.helix.ui.uiComponents.text.UiTextNode;
import dev.takesome.helix.ui.css.animation.UiCssAnimationRuntimeController;
import dev.takesome.helix.ui.css.runtime.UiCssNodeStyleApplier;
import dev.takesome.helix.ui.css.runtime.UiCssStyleRuntimeController;
import dev.takesome.helix.ui.css.transition.UiCssTransitionResolver;
import dev.takesome.helix.ui.css.transition.UiCssTransitionTimeline;
import dev.takesome.helix.ui.html.UiHtmlTagRegistry;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.dom.UiDomNode;
import dev.takesome.helix.ui.html.UiHtmlTagSpec;
import dev.takesome.helix.ui.markup.UiMarkupDocument;
import dev.takesome.helix.ui.markup.internal.action.UiMarkupActionBinder;
import dev.takesome.helix.ui.markup.internal.layout.UiDomLayoutResolver;
import dev.takesome.helix.ui.markup.internal.module.UiDomModuleGate;
import dev.takesome.helix.ui.markup.internal.style.UiDomButtonSkinResolver;
import dev.takesome.helix.ui.markup.internal.style.UiMarkupCssResolver;
import dev.takesome.helix.ui.markup.internal.style.UiDomSkinResolver;
import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.node.Node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Dispatches markup nodes through native HTML tag and element-composer registries. */
public final class UiDomRetainedNodeFactory {
    private static final float POPUP_BUTTON_X = 80f;
    private static final float POPUP_BUTTON_START_Y = 234f;
    private static final float POPUP_BUTTON_GAP = 12f;

    private final UiMarkupCssResolver styles;
    private final UiDomStyleReader reader;
    private final UiDomLayoutResolver layout;
    private final UiDomModuleGate moduleGate;
    private final UiDomSkinResolver skinResolver;
    private final UiHtmlTagRegistry htmlTags;
    private final UiBindingSource bindingSource;
    private final UiMarkupElementComposerRegistry composers;
    private final UiMarkupElementComposer buttonComposer;

    public UiDomRetainedNodeFactory(
            UiMarkupCssResolver styles,
            UiMarkupActionBinder actions,
            UiDomStyleReader reader,
            UiDomLayoutResolver layout,
            UiDomModuleGate moduleGate,
            UiDomButtonSkinResolver buttonSkins,
            UiDomSkinResolver skinResolver
    ) {
        this(styles, actions, reader, layout, moduleGate, buttonSkins, skinResolver, null, null, null);
    }

    public UiDomRetainedNodeFactory(
            UiMarkupCssResolver styles,
            UiMarkupActionBinder actions,
            UiDomStyleReader reader,
            UiDomLayoutResolver layout,
            UiDomModuleGate moduleGate,
            UiDomButtonSkinResolver buttonSkins,
            UiDomSkinResolver skinResolver,
            EngineI18n i18n
    ) {
        this(styles, actions, reader, layout, moduleGate, buttonSkins, skinResolver, i18n, null, null);
    }

    public UiDomRetainedNodeFactory(
            UiMarkupCssResolver styles,
            UiMarkupActionBinder actions,
            UiDomStyleReader reader,
            UiDomLayoutResolver layout,
            UiDomModuleGate moduleGate,
            UiDomButtonSkinResolver buttonSkins,
            UiDomSkinResolver skinResolver,
            EngineI18n i18n,
            EventBus events,
            UiBindingSource bindingSource
    ) {
        this.styles = styles;
        this.reader = reader;
        this.layout = layout;
        this.moduleGate = moduleGate;
        this.skinResolver = skinResolver;
        this.bindingSource = bindingSource;
        UiMarkupContainerFactory containers = new UiMarkupContainerFactory(reader, layout, skinResolver);
        UiMarkupTextFactory text = new UiMarkupTextFactory(reader, layout, i18n, bindingSource);
        UiMarkupButtonFactory buttons = new UiMarkupButtonFactory(actions, reader, layout, buttonSkins, i18n);
        UiMarkupImageFactory images = new UiMarkupImageFactory(reader, layout);
        UiMarkupIconFactory icons = new UiMarkupIconFactory(reader, layout);
        UiMarkupInputFactory inputs = new UiMarkupInputFactory(actions, reader, layout, bindingSource);
        this.htmlTags = UiHtmlTagRegistry.loadBuiltins();
        this.buttonComposer = new ButtonMarkupElementComposer(buttons);
        this.composers = new UiMarkupElementComposerRegistry()
                .register(new ContainerMarkupElementComposer(containers))
                .register(new PanelMarkupElementComposer(containers))
                .register(new ImageMarkupElementComposer(images))
                .register(new IconMarkupElementComposer(icons))
                .register(new StyleMarkupElementComposer())
                .register(new TextMarkupElementComposer(text))
                .register(buttonComposer)
                .register(new InputMarkupElementComposer(inputs))
                .register(new SliderMarkupElementComposer(inputs))
                .register(new CheckboxMarkupElementComposer(inputs))
                .register(new ComboBoxMarkupElementComposer(inputs))
                .register(new InputCaptureMarkupElementComposer(inputs));
    }

    public void compileChildren(Node parent, UiMarkupDocument document, float parentW, float parentH) {
        if (document == null) return;
        compileChildren(parent, document.dom().renderRoot(), parentW, parentH);
    }

    public void compileChildren(Node parent, UiDomElement domParent, UiMarkupDocument document, float parentW, float parentH) {
        compileChildren(parent, domParent, parentW, parentH);
    }

    public void compileChildren(Node parent, UiDomElement domParent, float parentW, float parentH) {
        compileChildren(parent, domParent, parentW, parentH, Set.of());
    }

    public void compileChildren(Node parent, UiDomElement domParent, float parentW, float parentH, String... excludedTags) {
        compileChildren(parent, domParent, parentW, parentH, excludedTagSet(excludedTags));
    }

    private void compileChildren(Node parent, UiDomElement domParent, float parentW, float parentH, Set<String> excludedTags) {
        if (parent == null || domParent == null) return;
        int buttonIndex = 0;
        for (UiDomNode childNode : domParent.children()) {
            if (!(childNode instanceof UiDomElement child)) continue;
            if (excludedTags != null && excludedTags.contains(child.tagName())) continue;
            if (moduleGate.closed(child)) continue;
            Map<String, String> style = styles.resolve(child);
            UiHtmlTagSpec tag = resolveTag(child.tagName());
            if (tag == null) continue;
            UiMarkupElementComposer composer = resolveComposer(child, tag);
            if (composer == null) continue;

            if (composer == buttonComposer && !layout.hasPosition(style)) {
                style = new LinkedHashMap<>(style);
                style.put("x", Float.toString(POPUP_BUTTON_X));
                style.put("y", Float.toString(POPUP_BUTTON_START_Y - buttonIndex * (reader.number(style, "h", 46f) + POPUP_BUTTON_GAP)));
                buttonIndex++;
            }

            Node node = composeNode(child, tag, composer, style, parentW, parentH);
            if (node == null) continue;
            attachPseudoElement(node, "before", styles.elementStyle(child, "before"));
            attachPseudoElement(node, "after", styles.elementStyle(child, "after"));
            attachStyleRuntime(node, child, style, styles.stateStyles(child));
            attachBindingRuntime(node, style);
            applyZIndex(node, style);
            parent.add(node);
            parent.sortChildrenByZIndex();
        }
    }

    public Node compileNode(UiDomElement element, UiMarkupDocument document, Map<String, String> style, float parentW, float parentH) {
        return compileNode(element, style, parentW, parentH);
    }

    public Node compileNode(UiDomElement element, Map<String, String> style, float parentW, float parentH) {
        if (element == null) return null;
        UiHtmlTagSpec tag = resolveTag(element.tagName());
        if (tag == null) return null;
        UiMarkupElementComposer composer = resolveComposer(element, tag);
        if (composer == null) return null;
        Node node = composeNode(element, tag, composer, style, parentW, parentH);
        if (node != null) {
            attachPseudoElement(node, "before", styles.elementStyle(element, "before"));
            attachPseudoElement(node, "after", styles.elementStyle(element, "after"));
            attachStyleRuntime(node, element, style, styles.stateStyles(element));
            attachBindingRuntime(node, style);
            applyZIndex(node, style);
        }
        return node;
    }

    private UiHtmlTagSpec resolveTag(String tagName) {
        return htmlTags.resolveOrFallback(tagName);
    }

    private UiMarkupElementComposer resolveComposer(UiDomElement element, UiHtmlTagSpec tag) {
        String componentId = componentComposerId(element);
        if (!componentId.isBlank()) return composers.require(componentId);
        return composers.require(tag.composerId());
    }

    private String componentComposerId(UiDomElement element) {
        String value = firstAttribute(element, "data-component", "data-ui-component", "component");
        if (value.isBlank()) return "";
        return value.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
    }

    private String firstAttribute(UiDomElement element, String... names) {
        if (element == null || names == null) return "";
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            String value = element.attribute(name, "");
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private Node composeNode(UiDomElement element, UiHtmlTagSpec tag, UiMarkupElementComposer composer, Map<String, String> style, float parentW, float parentH) {
        return composer.compose(new UiMarkupComposeContext(element, tag, style, parentW, parentH, this));
    }

    private Set<String> excludedTagSet(String... excludedTags) {
        if (excludedTags == null || excludedTags.length == 0) return Set.of();
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        for (String tag : excludedTags) {
            if (tag != null && !tag.isBlank()) out.add(tag.trim().toLowerCase(java.util.Locale.ROOT));
        }
        return Set.copyOf(out);
    }

    private void applyZIndex(Node node, Map<String, String> style) {
        if (node == null || style == null || style.isEmpty()) return;
        node.setZIndex(reader.integer(style, "z-index", reader.integer(style, "zIndex", 0)));
    }

    private void attachBindingRuntime(Node node, Map<String, String> style) {
        if (node == null || bindingSource == null || style == null || style.isEmpty()) return;
        String visibleKey = reader.first(style, "bind-visible");
        if (!visibleKey.isBlank()) {
            node.setVisible(bindingSource.bool(visibleKey));
            node.addRuntimeBinding((boundNode, dt) -> boundNode.setVisible(bindingSource.bool(visibleKey)));
        }
    }

    private void attachStyleRuntime(Node node, UiDomElement element, Map<String, String> style, Map<String, Map<String, String>> states) {
        if (node == null || style == null || style.isEmpty()) return;
        String id = style.getOrDefault("id", "");
        String nodeName = element == null ? "node" : element.tagName();
        Object identity = element == null ? node : element;
        String key = id.isBlank() ? nodeName + "@" + System.identityHashCode(identity) : id;
        UiCssStyleRuntimeController controller = new UiCssStyleRuntimeController(
                key,
                style,
                states,
                new UiCssTransitionTimeline(),
                new UiCssTransitionResolver(),
                new UiCssNodeStyleApplier(skinResolver)
        );
        node.addRuntimeBinding(controller);
        controller.applyInitial(node);
        UiCssAnimationRuntimeController animation = new UiCssAnimationRuntimeController(style, styles.keyframes());
        if (animation.active()) node.addRuntimeBinding(animation);
    }

    private void attachPseudoElement(Node node, String pseudoElement, Map<String, String> style) {
        if (node == null || style == null || style.isEmpty()) return;

        String content = stripQuotes(first(style, "content", ""));
        UiColor background = reader.color(style, "background-color", reader.color(style, "background", null));
        UiColor borderColor = reader.color(style, "border-color", null);
        UiColor textColor = reader.color(style, "color", UiColor.WHITE);
        float width = number(style, "w", number(style, "width", 0f));
        float height = number(style, "h", number(style, "height", 0f));
        boolean hasContent = !content.isBlank();
        boolean hasPaint = background != null || borderColor != null;

        if (hasContent && width <= 0f) width = Math.max(8f, content.length() * 8f);
        if (hasContent && height <= 0f) height = 18f;
        if (!hasContent && !hasPaint && width <= 0f && height <= 0f) return;
        if (width <= 0f || height <= 0f) return;

        Node pseudoNode;
        if (hasPaint) {
            UiPanelNode panel = new UiPanelNode(background == null ? UiColor.TRANSPARENT : background);
            panel.setBounds(0f, 0f, width, height);
            float borderWidth = number(style, "border-width", 0f);
            if (borderColor != null && borderWidth > 0f) panel.setBorder(borderColor, borderWidth);
            if (hasContent) {
                UiTextNode text = new UiTextNode(content, fontScale(style), textColor, TextAlign.LEFT, reader.font(style));
                text.setBounds(0f, 0f, width, height);
                panel.add(text);
            }
            pseudoNode = panel;
        } else {
            UiTextNode text = new UiTextNode(content, fontScale(style), textColor, TextAlign.LEFT, reader.font(style));
            text.setBounds(0f, 0f, width, height);
            pseudoNode = text;
        }

        pseudoNode.setBounds(number(style, "x", number(style, "left", 0f)), number(style, "y", number(style, "top", 0f)), width, height);
        node.add(pseudoNode);
        if ("before".equalsIgnoreCase(pseudoElement)) node.sendChildToBack(pseudoNode);
    }

    private float fontScale(Map<String, String> style) {
        String fontSize = first(style, "font-size", "");
        if (!fontSize.isBlank()) return Math.max(0.01f, reader.number(fontSize, 16f) / 16f);
        return Math.max(0.01f, reader.number(style, "scale", 1f));
    }

    private float number(Map<String, String> style, String key, float fallback) {
        return reader.number(style, key, fallback);
    }

    private String first(Map<String, String> style, String key, String fallback) {
        String value = style.getOrDefault(key, "");
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String stripQuotes(String value) {
        String out = trimToEmpty(value);
        if (out.length() >= 2 && ((out.startsWith("\"") && out.endsWith("\"")) || (out.startsWith("'") && out.endsWith("'")))) {
            return out.substring(1, out.length() - 1);
        }
        return out;
    }
}
