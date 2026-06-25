package dev.takesome.helix.ui.markup.internal.style;

import dev.takesome.helix.data.io.DataFiles;
import dev.takesome.helix.ui.css.UiCssKeyframesRule;
import dev.takesome.helix.ui.dom.UiDomElement;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Internal CSS-like tag/class/id style resolver for HELIX UI Markup. */
public final class UiMarkupCssResolver {
    private final Map<String, Map<String, String>> rules = new LinkedHashMap<>();
    private Map<UiDomElement, Map<String, String>> computedOverrides = new IdentityHashMap<>();
    private Map<UiDomElement, Map<String, Map<String, String>>> computedStateOverrides = new IdentityHashMap<>();
    private Map<UiDomElement, Map<String, Map<String, String>>> computedElementOverrides = new IdentityHashMap<>();
    private Map<String, UiCssKeyframesRule> computedKeyframes = Map.of();

    public UiMarkupCssResolver() {
        defaults();
    }

    public UiMarkupCssResolver load(String path) {
        if (path == null || path.isBlank()) return this;
        parse(DataFiles.readString(path));
        return this;
    }

    public void setComputedOverrides(Map<UiDomElement, Map<String, String>> overrides) {
        computedOverrides = new IdentityHashMap<>();
        if (overrides != null) computedOverrides.putAll(overrides);
    }

    public void clearComputedOverrides() {
        computedOverrides = new IdentityHashMap<>();
        computedStateOverrides = new IdentityHashMap<>();
        computedElementOverrides = new IdentityHashMap<>();
    }

    public void setComputedStateOverrides(Map<UiDomElement, Map<String, Map<String, String>>> overrides) {
        computedStateOverrides = new IdentityHashMap<>();
        if (overrides != null) computedStateOverrides.putAll(overrides);
    }

    public void setComputedElementOverrides(Map<UiDomElement, Map<String, Map<String, String>>> overrides) {
        computedElementOverrides = new IdentityHashMap<>();
        if (overrides != null) computedElementOverrides.putAll(overrides);
    }

    public void setComputedKeyframes(Map<String, UiCssKeyframesRule> keyframes) {
        computedKeyframes = keyframes == null ? Map.of() : Map.copyOf(keyframes);
    }

    public Map<String, UiCssKeyframesRule> keyframes() {
        return computedKeyframes;
    }

    public Map<String, Map<String, String>> stateStyles(UiDomElement node) {
        return computedStateOverrides.getOrDefault(node, Map.of());
    }

    public Map<String, String> elementStyle(UiDomElement node, String elementName) {
        Map<String, Map<String, String>> styles = computedElementOverrides.get(node);
        if (styles == null || elementName == null) return Map.of();
        return styles.getOrDefault(elementName, Map.of());
    }

    public Map<String, String> resolve(UiDomElement node) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (node == null) return out;
        merge(out, rules.get(node.tagName()));
        String classes = node.attribute("class", "");
        if (!classes.isBlank()) {
            for (String cls : classes.split("\\s+")) merge(out, rules.get("." + cls));
        }
        String id = node.id();
        if (!id.isBlank()) merge(out, rules.get("#" + id));
        out.putAll(node.attributes());
        merge(out, computedOverrides.get(node));
        return out;
    }

    public UiMarkupCssResolver parse(String css) {
        if (css == null || css.isBlank()) return this;
        int index = 0;
        while (index < css.length()) {
            int open = css.indexOf('{', index);
            if (open < 0) break;
            int close = css.indexOf('}', open + 1);
            if (close < 0) break;
            String selector = css.substring(index, open).trim();
            String body = css.substring(open + 1, close).trim();
            addRule(selector, body);
            index = close + 1;
        }
        return this;
    }

    private void defaults() {
        rule("popup", "overlay: dim; overlay-color: 0, 0, 0, 0.58; overlay-fade-ms: 180;");
        rule(".popup-panel", "x: center; y: center; w: 540; h: 430; color: transparent; appear-animation: slide; appear-ms: 170; appear-offset-y: -18;");
        rule(".bottom-ribbon", "x: 54; y: 344; w: 432; h: 64;");
        rule(".title", "x: 42; y: 342; w: 456; h: 52; font: title; scale: 1.0; color: #ffd95a; align: center;");
        rule(".hint", "x: 42; y: 302; w: 456; h: 28; font: standart; scale: 1.0; color: #f2dfb7; align: center;");
        rule("button", "w: 380; h: 46; button-style: button.big.square.blue;");
        rule(".danger", "button-style: button.big.square.red;");
    }

    private void rule(String selector, String body) {
        addRule(selector, body);
    }

    private void addRule(String selector, String body) {
        if (selector == null || selector.isBlank() || body == null || body.isBlank()) return;
        for (String rawSelector : selector.split(",")) {
            String key = rawSelector.trim();
            if (key.isBlank()) continue;
            rules.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).putAll(properties(body));
        }
    }

    private Map<String, String> properties(String body) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (String part : body.split(";")) {
            int colon = part.indexOf(':');
            if (colon <= 0) continue;
            String key = part.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = part.substring(colon + 1).trim();
            if (!key.isBlank() && !value.isBlank()) out.put(key, value);
        }
        return out;
    }

    private void merge(Map<String, String> target, Map<String, String> source) {
        if (source != null) target.putAll(source);
    }
}
