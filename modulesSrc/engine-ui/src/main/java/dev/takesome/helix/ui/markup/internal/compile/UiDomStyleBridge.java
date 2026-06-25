package dev.takesome.helix.ui.markup.internal.compile;

import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;

import com.badlogic.gdx.files.FileHandle;
import dev.takesome.helix.data.io.DataFiles;
import dev.takesome.helix.logging.EngineLog;
import dev.takesome.helix.ui.css.UiCssBox;
import dev.takesome.helix.ui.css.UiCssCascade;
import dev.takesome.helix.ui.css.UiCssLayoutEngine;
import dev.takesome.helix.ui.css.UiCssLayoutResult;
import dev.takesome.helix.ui.css.UiCssParser;
import dev.takesome.helix.ui.css.UiCssPropertyRegistry;
import dev.takesome.helix.ui.css.UiCssPropertySpec;
import dev.takesome.helix.ui.css.UiIntrinsicTextMeasurer;
import dev.takesome.helix.ui.css.UiStylesheet;
import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.dom.UiDomTraversal;
import dev.takesome.helix.ui.html.attributes.BindHeightHtmlAttribute;
import dev.takesome.helix.ui.html.attributes.BindWidthHtmlAttribute;
import dev.takesome.helix.ui.html.attributes.BindXHtmlAttribute;
import dev.takesome.helix.ui.html.attributes.BindYHtmlAttribute;
import dev.takesome.helix.ui.markup.UiMarkupDocument;
import dev.takesome.helix.ui.runtime.UiRuntimeInspectionSource;
import org.apache.logging.log4j.Logger;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Bridges canonical DOM into the CSS/DOM computed-style pipeline. */
final class UiDomStyleBridge {
    private static final Logger LOG = EngineLog.logger(UiDomStyleBridge.class);
    private static final String[] STATES = {"hover", "active", "disabled", "focus", "checked", "selected", "open"};
    private static final int STYLESHEET_CACHE_LIMIT = Math.max(0, Integer.getInteger("helix.ui.css.cache.stylesheets", 64));
    private static final int INLINE_STYLESHEET_CACHE_LIMIT = Math.max(0, Integer.getInteger("helix.ui.css.cache.inlineStyles", 64));
    private static final int COMPUTED_STYLE_CACHE_LIMIT = Math.max(0, Integer.getInteger("helix.ui.css.cache.computed", 64));
    private static final Map<String, CachedStylesheet> STYLESHEET_CACHE = lruMap(STYLESHEET_CACHE_LIMIT);
    private static final Map<String, UiStylesheet> INLINE_STYLESHEET_CACHE = lruMap(INLINE_STYLESHEET_CACHE_LIMIT);
    private static final Map<ComputedStyleKey, UiDomComputedStyles> COMPUTED_STYLE_CACHE = lruMap(COMPUTED_STYLE_CACHE_LIMIT);
    private static final Set<String> WARNED_STYLESHEET_FAILURES = ConcurrentHashMap.newKeySet();

    private final UiCssPropertyRegistry cssProperties = UiCssPropertyRegistry.loadBuiltins();
    private final UiCssParser parser = new UiCssParser();
    private final UiCssCascade cascade = new UiCssCascade(cssProperties);
    private final UiCssLayoutEngine layout;
    private final UiIntrinsicTextMeasurer textMeasurer;
    private UiRuntimeInspectionSource lastInspectionSource = UiRuntimeInspectionSource.empty();

    UiDomStyleBridge() {
        this(null);
    }

    UiDomStyleBridge(UiIntrinsicTextMeasurer textMeasurer) {
        this.textMeasurer = textMeasurer;
        this.layout = new UiCssLayoutEngine(cssProperties, textMeasurer);
    }

    UiRuntimeInspectionSource lastInspectionSource() {
        return lastInspectionSource == null ? UiRuntimeInspectionSource.empty() : lastInspectionSource;
    }

    static void clearRuntimeCaches() {
        synchronized (STYLESHEET_CACHE) {
            STYLESHEET_CACHE.clear();
        }
        synchronized (INLINE_STYLESHEET_CACHE) {
            INLINE_STYLESHEET_CACHE.clear();
        }
        synchronized (COMPUTED_STYLE_CACHE) {
            COMPUTED_STYLE_CACHE.clear();
        }
        WARNED_STYLESHEET_FAILURES.clear();
    }

    UiDomComputedStyles compute(UiMarkupDocument markupDocument, float width, float height) {
        if (markupDocument == null) return new UiDomComputedStyles(Map.of(), Map.of(), Map.of());
        float safeW = Math.max(1f, width);
        float safeH = Math.max(1f, height);
        DependencyStamp styleStamp = styleDependencyStamp(markupDocument);
        ComputedStyleKey cacheKey = new ComputedStyleKey(
                markupDocument,
                Float.floatToIntBits(safeW),
                Float.floatToIntBits(safeH),
                textMeasurer == null ? 0 : System.identityHashCode(textMeasurer),
                styleStamp.value()
        );
        if (styleStamp.cacheable() && COMPUTED_STYLE_CACHE_LIMIT > 0) {
            synchronized (COMPUTED_STYLE_CACHE) {
                UiDomComputedStyles cached = COMPUTED_STYLE_CACHE.get(cacheKey);
                if (cached != null) return cached;
            }
        }

        UiDomComputedStyles computed = computeUncached(markupDocument, safeW, safeH);
        if (styleStamp.cacheable() && COMPUTED_STYLE_CACHE_LIMIT > 0) {
            synchronized (COMPUTED_STYLE_CACHE) {
                COMPUTED_STYLE_CACHE.put(cacheKey, computed);
            }
        }
        return computed;
    }

    private UiDomComputedStyles computeUncached(UiMarkupDocument markupDocument, float width, float height) {
        UiDomDocument dom = markupDocument.dom();
        List<UiDomElement> elements = UiDomTraversal.depthFirstElements(dom.documentElement());
        dom.drainMutations();
        dom.root().clearDirty();

        UiStylesheet stylesheet = loadStylesheet(markupDocument);
        cascade.apply(dom, stylesheet);
        applyAttributeFallbacks(elements);
        UiCssLayoutResult baseLayout = layout.layout(dom, width, height);
        lastInspectionSource = new UiRuntimeInspectionSource(dom, baseLayout, width, height, System.currentTimeMillis());

        IdentityHashMap<UiDomElement, Map<String, String>> base = new IdentityHashMap<>();
        IdentityHashMap<UiDomElement, Map<String, Map<String, String>>> states = new IdentityHashMap<>();
        IdentityHashMap<UiDomElement, Map<String, Map<String, String>>> pseudoElements = new IdentityHashMap<>();

        for (UiDomElement element : elements) {
            base.put(element, styleMap(element, baseLayout));
            Map<String, Map<String, String>> elementStyles = elementStyles(element);
            if (!elementStyles.isEmpty()) pseudoElements.put(element, elementStyles);
        }

        for (String state : STATES) {
            if (!stylesheetContainsState(stylesheet, state)) continue;
            for (UiDomElement element : elements) element.setPseudoClass(state, true);
            cascade.apply(dom, stylesheet);
            applyAttributeFallbacks(elements);
            UiCssLayoutResult stateLayout = layout.layout(dom, width, height);
            for (UiDomElement element : elements) {
                Map<String, String> value = styleMap(element, stateLayout);
                if (!value.equals(base.get(element))) {
                    states.computeIfAbsent(element, ignored -> new LinkedHashMap<>()).put(state, value);
                }
            }
            for (UiDomElement element : elements) element.setPseudoClass(state, false);
        }

        cascade.apply(dom, stylesheet);
        applyAttributeFallbacks(elements);
        dom.drainMutations();
        dom.root().clearDirty();
        return new UiDomComputedStyles(base, states, pseudoElements, stylesheet.keyframes());
    }

    private UiStylesheet loadStylesheet(UiMarkupDocument document) {
        UiStylesheet out = UiStylesheet.empty();
        for (String path : documentStylesheetPaths(document)) {
            if (path.isBlank()) continue;
            out = out.plus(loadExternalStylesheet(path.trim()));
        }
        for (UiDomElement styleElement : inlineStyleElements(document.dom())) {
            String css = emptyIfNull(styleElement.textContent());
            if (!css.isBlank()) out = out.plus(parseInlineStylesheet(css));
        }
        return out;
    }

    private UiStylesheet loadExternalStylesheet(String path) {
        String cleanPath = DataFiles.normalize(path);
        if (cleanPath.isBlank()) return UiStylesheet.empty();

        FileStamp stamp = fileStamp(cleanPath);
        if (stamp.cacheable() && STYLESHEET_CACHE_LIMIT > 0) {
            synchronized (STYLESHEET_CACHE) {
                CachedStylesheet cached = STYLESHEET_CACHE.get(cleanPath);
                if (cached != null && cached.stamp().equals(stamp)) return cached.stylesheet();
            }
        }

        UiStylesheet stylesheet;
        try {
            stylesheet = parser.parse(DataFiles.readString(cleanPath));
        } catch (RuntimeException ex) {
            warnStylesheetDegraded(cleanPath, ex);
            return UiStylesheet.empty();
        }
        if (stamp.cacheable() && STYLESHEET_CACHE_LIMIT > 0) {
            synchronized (STYLESHEET_CACHE) {
                STYLESHEET_CACHE.put(cleanPath, new CachedStylesheet(stamp, stylesheet));
            }
        }
        return stylesheet;
    }

    private UiStylesheet parseInlineStylesheet(String source) {
        String safeSource = emptyIfNull(source);
        if (safeSource.isBlank()) return UiStylesheet.empty();
        if (INLINE_STYLESHEET_CACHE_LIMIT > 0) {
            synchronized (INLINE_STYLESHEET_CACHE) {
                UiStylesheet cached = INLINE_STYLESHEET_CACHE.get(safeSource);
                if (cached != null) return cached;
            }
        }
        UiStylesheet stylesheet;
        try {
            stylesheet = parser.parse(safeSource);
        } catch (RuntimeException ex) {
            warnStylesheetDegraded("<inline-style>", ex);
            return UiStylesheet.empty();
        }
        if (INLINE_STYLESHEET_CACHE_LIMIT > 0) {
            synchronized (INLINE_STYLESHEET_CACHE) {
                INLINE_STYLESHEET_CACHE.put(safeSource, stylesheet);
            }
        }
        return stylesheet;
    }

    private static void warnStylesheetDegraded(String source, RuntimeException ex) {
        String safeSource = emptyIfNull(source).isBlank() ? "<unknown>" : source;
        String reason = ex == null ? "unknown" : ex.getClass().getSimpleName() + ": " + emptyIfNull(ex.getMessage());
        String key = safeSource + "|" + reason;
        if (WARNED_STYLESHEET_FAILURES.add(key)) {
            LOG.warn("UI stylesheet unavailable source='{}'; continuing with empty stylesheet. reason={}", safeSource, reason);
        }
    }

    private DependencyStamp styleDependencyStamp(UiMarkupDocument document) {
        long hash = 0xcbf29ce484222325L;
        boolean cacheable = true;
        hash = mix(hash, document.dom().version());
        for (String path : documentStylesheetPaths(document)) {
            if (path.isBlank()) continue;
            String cleanPath = DataFiles.normalize(path.trim());
            FileStamp stamp = fileStamp(cleanPath);
            hash = mix(hash, cleanPath.hashCode());
            hash = mix(hash, stamp.modified());
            hash = mix(hash, stamp.length());
            cacheable &= stamp.cacheable();
        }
        for (UiDomElement styleElement : inlineStyleElements(document.dom())) {
            String css = emptyIfNull(styleElement.textContent());
            hash = mix(hash, css.length());
            hash = mix(hash, css.hashCode());
        }
        return new DependencyStamp(hash, cacheable);
    }

    private List<String> documentStylesheetPaths(UiMarkupDocument document) {
        if (document == null) return List.of();
        String sourcePath = document.sourcePath();
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        UiDomDocument dom = document.dom();
        collectStylesheetAttributePaths(paths, dom.documentElement(), sourcePath);
        collectStylesheetAttributePaths(paths, dom.renderRoot(), sourcePath);
        for (UiDomElement link : stylesheetLinkElements(dom)) {
            String href = link.attribute("href", "").trim();
            if (!href.isBlank()) paths.add(UiDomResourcePathResolver.resolve(href, sourcePath));
        }
        return List.copyOf(paths);
    }

    private void collectStylesheetAttributePaths(Set<String> target, UiDomElement element, String sourcePath) {
        if (target == null || element == null) return;
        String paths = element.attribute("stylesheet", "").trim();
        if (paths.isBlank()) return;
        for (String path : splitStylesheetPaths(paths)) {
            if (!path.isBlank()) target.add(UiDomResourcePathResolver.resolve(path.trim(), sourcePath));
        }
    }

    private List<UiDomElement> stylesheetLinkElements(UiDomDocument dom) {
        if (dom == null || dom.rootOptional().isEmpty()) return List.of();
        return UiDomTraversal.depthFirstElements(dom.documentElement()).stream()
                .filter(this::isStylesheetLink)
                .toList();
    }

    private boolean isStylesheetLink(UiDomElement element) {
        if (element == null || !"link".equals(element.tagName())) return false;
        if (element.hasAttribute("disabled")) return false;
        if (!relContains(element.attribute("rel", ""), "stylesheet")) return false;
        if (element.attribute("href", "").isBlank()) return false;
        String type = element.attribute("type", "");
        return type.isBlank() || "text/css".equalsIgnoreCase(type.trim());
    }

    private List<UiDomElement> inlineStyleElements(UiDomDocument dom) {
        if (dom == null || dom.rootOptional().isEmpty()) return List.of();
        return UiDomTraversal.depthFirstElements(dom.documentElement()).stream()
                .filter(element -> "style".equals(element.tagName()))
                .toList();
    }

    private boolean relContains(String rel, String token) {
        if (rel == null || rel.isBlank() || token == null || token.isBlank()) return false;
        String expected = token.trim().toLowerCase(Locale.ROOT);
        for (String value : rel.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (expected.equals(value)) return true;
        }
        return false;
    }

    private List<String> splitStylesheetPaths(String paths) {
        if (paths.contains(",")) return List.of(paths.split("\\s*,\\s*"));
        return List.of(paths.trim());
    }

    private void applyAttributeFallbacks(List<UiDomElement> elements) {
        if (elements == null || elements.isEmpty()) return;
        for (UiDomElement element : elements) {
            for (String attr : cssProperties.attributeFallbackNames()) {
                String value = element.attribute(attr, "");
                if (value.isBlank()) continue;
                putIfMissing(element, cssProperties.require(attr).name(), value);
            }
        }
    }

    private void putIfMissing(UiDomElement element, String property, String value) {
        if (element.style(property).isBlank()) element.setComputedStyle(property, value);
    }

    private Map<String, String> styleMap(UiDomElement element, UiCssLayoutResult result) {
        LinkedHashMap<String, String> style = new LinkedHashMap<>(element.computedStyle());
        result.box(element).ifPresent(box -> applyBox(style, element, box, result));
        copyCssAliases(style);
        return Map.copyOf(style);
    }

    private Map<String, Map<String, String>> elementStyles(UiDomElement element) {
        LinkedHashMap<String, Map<String, String>> out = new LinkedHashMap<>();
        copyElementStyle(out, element, "before");
        copyElementStyle(out, element, "after");
        return out;
    }

    private void copyElementStyle(Map<String, Map<String, String>> target, UiDomElement element, String name) {
        Map<String, String> style = element.pseudoComputedStyle(name);
        if (!style.isEmpty()) target.put(name, Map.copyOf(style));
    }

    private void applyBox(Map<String, String> style, UiDomElement element, UiCssBox box, UiCssLayoutResult result) {
        float localX = box.x();
        float localY = box.y();
        if (element.parent() != null) {
            UiDomElement parent = element.parent();
            UiCssBox parentBox = result.box(parent).orElse(null);
            if (parentBox != null) {
                localX -= parentBox.x();
                localY -= parentBox.y();
            }
        }
        if (!element.hasAttribute(BindXHtmlAttribute.NAME)) style.put("x", number(localX));
        if (!element.hasAttribute(BindYHtmlAttribute.NAME)) style.put("y", number(localY));
        if (!element.hasAttribute(BindWidthHtmlAttribute.NAME)) {
            style.put("w", number(box.width()));
            style.put("width", number(box.width()));
        }
        if (!element.hasAttribute(BindHeightHtmlAttribute.NAME)) {
            style.put("h", number(box.height()));
            style.put("height", number(box.height()));
        }
    }

    private boolean stylesheetContainsState(UiStylesheet stylesheet, String state) {
        if (stylesheet == null || stylesheet.rules().isEmpty()) return false;
        for (var rule : stylesheet.rules()) {
            if (rule.selectorText().toLowerCase(Locale.ROOT).contains(":" + state)) return true;
        }
        return false;
    }

    private void copyCssAliases(Map<String, String> style) {
        for (UiCssPropertySpec property : cssProperties.definitions()) {
            String value = style.getOrDefault(property.name(), "");
            if (value.isBlank()) continue;
            for (String alias : property.aliases()) {
                if (style.getOrDefault(alias, "").isBlank()) style.put(alias, stripQuotes(value));
            }
        }
    }

    private String stripQuotes(String value) {
        String out = trimToEmpty(value);
        if (out.length() >= 2 && ((out.startsWith("\"") && out.endsWith("\"")) || (out.startsWith("'") && out.endsWith("'")))) return out.substring(1, out.length() - 1);
        return out;
    }

    private String number(float value) {
        if (value == Math.rint(value)) return Integer.toString(Math.round(value));
        return String.format(Locale.ROOT, "%s", value);
    }

    private static FileStamp fileStamp(String path) {
        try {
            FileHandle file = DataFiles.file(path);
            if (file != null && file.exists()) {
                return new FileStamp(file.lastModified(), file.length(), true);
            }
        } catch (RuntimeException ignored) {
            // Virtual resources may not expose stable modification metadata.
        }
        return new FileStamp(-1L, -1L, false);
    }

    private static long mix(long seed, long value) {
        return (seed ^ value) * 0x100000001b3L;
    }

    private static <K, V> Map<K, V> lruMap(int limit) {
        return new LinkedHashMap<K, V>(32, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return limit > 0 && size() > limit;
            }
        };
    }

    private record CachedStylesheet(FileStamp stamp, UiStylesheet stylesheet) {
    }

    private record FileStamp(long modified, long length, boolean cacheable) {
    }

    private record DependencyStamp(long value, boolean cacheable) {
    }

    private record ComputedStyleKey(
            UiMarkupDocument document,
            int widthBits,
            int heightBits,
            int textMeasurerIdentity,
            long dependencyStamp
    ) {
    }
}
