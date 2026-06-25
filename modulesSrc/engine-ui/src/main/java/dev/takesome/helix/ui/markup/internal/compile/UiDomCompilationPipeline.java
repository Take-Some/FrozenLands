package dev.takesome.helix.ui.markup.internal.compile;

import dev.takesome.helix.events.bus.EventBus;
import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.ui.markup.UiMarkupDocument;
import dev.takesome.helix.ui.markup.UiMarkupDiagnosticsEvent;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.dom.UiDomTraversal;
import dev.takesome.helix.ui.css.UiIntrinsicTextMeasurer;
import dev.takesome.helix.ui.markup.internal.action.UiMarkupActionBinder;
import dev.takesome.helix.ui.markup.internal.factory.UiDomRetainedNodeFactory;
import dev.takesome.helix.ui.markup.internal.layout.UiDomLayoutResolver;
import dev.takesome.helix.ui.markup.internal.module.UiDomModuleGate;
import dev.takesome.helix.ui.markup.internal.root.UiDomRootDecorator;
import dev.takesome.helix.ui.markup.internal.style.UiDomButtonSkinResolver;
import dev.takesome.helix.ui.markup.internal.style.UiMarkupCssResolver;
import dev.takesome.helix.ui.markup.internal.style.UiDomSkinResolver;
import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.node.ContainerNode;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.skin.UiSkinDescriptorLoader;
import dev.takesome.helix.ui.skin.UiSkinRegistry;
import dev.takesome.helix.ui.runtime.UiRuntimeInspectionSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Owns the complete HELIX UI Markup compilation pipeline.
 */
public final class UiDomCompilationPipeline {
    private final EventBus events;
    private final UiMarkupCssResolver styles = new UiMarkupCssResolver();
    private final UiDomStyleReader styleReader = new UiDomStyleReader();
    private final UiDomLayoutResolver layout = new UiDomLayoutResolver(styleReader);
    private final UiDomModuleGate moduleGate = new UiDomModuleGate();
    private final UiDomRootDecorator rootDecorator = new UiDomRootDecorator(styleReader);
    private final UiDomStyleBridge domStyles;
    private final UiSkinRegistry skinRegistry = new UiSkinRegistry();
    private final UiSkinDescriptorLoader skinLoader = new UiSkinDescriptorLoader();
    private final UiDomSkinResolver skinResolver = new UiDomSkinResolver(styleReader, skinRegistry);
    private final UiDomRetainedNodeFactory nodes;

    public UiDomCompilationPipeline(EventBus events) {
        this(events, null, null);
    }

    public UiDomCompilationPipeline(EventBus events, UiIntrinsicTextMeasurer textMeasurer) {
        this(events, textMeasurer, null);
    }

    public UiDomCompilationPipeline(EventBus events, UiIntrinsicTextMeasurer textMeasurer, EngineI18n i18n) {
        this(events, textMeasurer, i18n, null);
    }

    public UiDomCompilationPipeline(EventBus events, UiIntrinsicTextMeasurer textMeasurer, EngineI18n i18n, UiBindingSource bindingSource) {
        this.events = events;
        this.domStyles = new UiDomStyleBridge(textMeasurer);
        UiMarkupActionBinder actions = new UiMarkupActionBinder(events);
        UiDomButtonSkinResolver buttonSkins = new UiDomButtonSkinResolver(styleReader);
        this.nodes = new UiDomRetainedNodeFactory(styles, actions, styleReader, layout, moduleGate, buttonSkins, skinResolver, i18n, events, bindingSource);
    }

    public static void clearRuntimeCaches() {
        UiDomStyleBridge.clearRuntimeCaches();
        UiSkinDescriptorLoader.clearRuntimeCaches();
    }

    public UiRuntimeInspectionSource lastInspectionSource() {
        return domStyles.lastInspectionSource();
    }

    public Node compile(UiMarkupDocument document, float width, float height) {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        if (document.hasDiagnostics() && events != null) {
            events.emit(new UiMarkupDiagnosticsEvent(document.diagnostics()));
        }
        UiDomElement domRenderRoot = document.dom().renderRoot();
        loadSkinDescriptors(document.dom());

        ContainerNode container = new ContainerNode();
        float safeW = Math.max(1f, width);
        float safeH = Math.max(1f, height);
        UiDomComputedStyles computedStyles = domStyles.compute(document, safeW, safeH);
        styles.setComputedOverrides(computedStyles.base());
        styles.setComputedStateOverrides(computedStyles.states());
        styles.setComputedElementOverrides(computedStyles.elements());
        styles.setComputedKeyframes(computedStyles.keyframes());
        container.setBounds(0f, 0f, safeW, safeH);

        Map<String, String> rootStyle = styles.resolve(domRenderRoot);
        rootDecorator.addRootBackground(container, domRenderRoot, rootStyle, safeW, safeH);
        nodes.compileChildren(container, domRenderRoot, document, safeW, safeH);
        layout.applyState(container, rootStyle);
        return container;
    }

    private void loadSkinDescriptors(UiDomDocument document) {
        skinRegistry.clear();
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        if (document == null || document.rootOptional().isEmpty()) return;
        collectSkinAttributePaths(paths, document.documentElement());
        collectSkinAttributePaths(paths, document.renderRoot());
        paths.addAll(linkedResourcePaths(document, Set.of("ui-skins", "ui-chrome", "chrome", "skins", "skin-set", "skinset")));
        for (String path : paths) {
            if (path == null || path.isBlank()) continue;
            skinLoader.loadInto(skinRegistry, path.trim());
        }
    }

    private void collectSkinAttributePaths(Set<String> target, UiDomElement element) {
        if (target == null || element == null) return;
        String paths = firstAttribute(element, "ui-skins", "ui-chrome", "chrome", "skins", "skin-set", "skinset");
        if (paths.isBlank()) return;
        for (String path : splitPaths(paths)) {
            if (path != null && !path.isBlank()) target.add(path.trim());
        }
    }

    private List<String> linkedResourcePaths(UiDomDocument document, Set<String> relTokens) {
        if (document == null || document.rootOptional().isEmpty() || relTokens == null || relTokens.isEmpty()) return List.of();
        ArrayList<String> out = new ArrayList<>();
        Set<String> normalized = normalizedRelTokens(relTokens);
        for (UiDomElement element : UiDomTraversal.depthFirstElements(document.documentElement())) {
            if ("link".equals(element.tagName()) && linkRelMatches(element, normalized)) {
                String href = element.attribute("href", "").trim();
                if (!href.isBlank()) out.add(href);
            }
        }
        return List.copyOf(out);
    }

    private boolean linkRelMatches(UiDomElement element, Set<String> relTokens) {
        if (element == null || relTokens == null || relTokens.isEmpty()) return false;
        String rel = element.attribute("rel", "").trim();
        if (rel.isBlank()) return false;
        for (String value : rel.toLowerCase(Locale.ROOT).split("\s+")) {
            if (relTokens.contains(value)) return true;
        }
        return false;
    }

    private Set<String> normalizedRelTokens(Set<String> relTokens) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String token : relTokens) {
            if (token != null && !token.isBlank()) out.add(token.trim().toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(out);
    }

    private String firstAttribute(UiDomElement element, String... names) {
        if (element == null || names == null) return "";
        for (String name : names) {
            String value = element.attribute(name, "").trim();
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private List<String> splitPaths(String paths) {
        if (paths == null || paths.isBlank()) return List.of();
        if (paths.contains(",")) return List.of(paths.split("\s*,\s*"));
        return List.of(paths.trim());
    }
}
